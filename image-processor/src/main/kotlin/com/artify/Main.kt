package com.artify

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.AmazonS3Exception
import com.artify.image.ImageProcessorMessage
import com.rabbitmq.client.CancelCallback
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.DeliverCallback
import io.github.oshai.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import javax.imageio.ImageIO

@OptIn(ExperimentalSerializationApi::class)
suspend fun main() {
    val logger = KotlinLogging.logger {}
    logger.info { "Henlo! Image processor microservice starting!" }
    logger.info { "Connecting to RabbitMQ" }

    val s3client = AmazonS3ClientBuilder
        .standard()
        .withCredentials(
            AWSStaticCredentialsProvider(
                BasicAWSCredentials(
                    System.getenv("AWS_ACCESS_KEY"),
                    System.getenv("AWS_SECRET_KEY")
                )
            )
        )
        .withRegion(Regions.EU_CENTRAL_1)
        .build()

    val factory = ConnectionFactory().apply {
        useSslProtocol()
        host = System.getenv("RABBITMQ_HOST")
        port = System.getenv("RABBITMQ_PORT").toInt()
        username = System.getenv("RABBITMQ_USERNAME")
        password = System.getenv("RABBITMQ_PASSWORD")
    }

    withContext(Dispatchers.Default) {
        launch {
            val connection = factory.newConnection(System.getenv("RABBITMQ_HOST"))
            val channel = connection.createChannel()

            channel.queueDeclare("scaling_queue", true, false, false, null)
            channel.basicConsume(
                "scaling_queue",
                false,
                DeliverCallback { consumerTag, message ->
                    logger.trace { "Received new message for consumer $consumerTag" }

                    val body = Json.decodeFromStream<ImageProcessorMessage>(message.body.inputStream())

                    runBlocking {
                        logger.trace { "Generating scaled thumbnails for hash ${body.hash}" }

                        val blob = try {
                            s3client.getObject("artify-com", "${body.hash}/original")
                        } catch (e: AmazonS3Exception) {
                            logger.error("Failed to fetch object ${body.hash}")
                            return@runBlocking
                        }
                        val image = ImageIO.read(blob.objectContent)

                        val cropped = image.getSubimage(body.position.x, body.position.y, body.size.x, body.size.y)

                        // TODO: Parallelize this?
                        body.scales.map {
                            launch {
                                logger.trace { "Generating scaled down thumbnail at ${it.x}x${it.y}" }

                                s3client.putImage(
                                    "artify-com",
                                    "${body.hash}/${it.x}x${it.y}",
                                    cropped.scale(it.x, it.y),
                                    blob.objectMetadata.contentType
                                )

                                logger.trace { "Finished generating thumbnail at ${it.x}x${it.y}" }
                            }
                        }.joinAll()

                        logger.trace { "Finished generating thumbnails for hash ${body.hash}" }
                    }

                    channel.basicAck(message.envelope.deliveryTag, false)
                    logger.trace { "Acknowledged message with delivery tag ${message.envelope.deliveryTag}" }
                },
                CancelCallback {
                    logger.trace { "Consumer $it was cancelled" }
                }
            )
        }
    }
}
