package com.artify

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.artify.image.ImageProcessorMessage
import com.rabbitmq.client.CancelCallback
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.DeliverCallback
import io.github.oshai.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.awt.Image
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
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

    val factory = ConnectionFactory()

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
                        val blob = s3client.getObject("artify-com", "${body.hash}/original")
                        val image = ImageIO.read(blob.objectContent)

                        body.dimensions.map {
                            async {
                                val scaled = image.getScaledInstance(it.width, it.height, Image.SCALE_SMOOTH)
                                val buffered = BufferedImage(it.width, it.height, image.type)
                                buffered.graphics.drawImage(scaled, -it.x, -it.y, null)

                                val stream = ByteArrayOutputStream()
                                ImageIO.write(
                                    buffered,
                                    blob.objectMetadata.contentType.substringAfter('/'),
                                    stream
                                )

                                val bytes = stream.toByteArray()
                                s3client.putObject(
                                    "artify-com",
                                    "${body.hash}/${it.width}-${it.height}",
                                    ByteArrayInputStream(bytes),
                                    blob.objectMetadata.clone().apply {
                                        contentLength = bytes.size.toLong()
                                    }
                                )
                            }
                        }.awaitAll()
                    }

                    channel.basicAck(message.envelope.deliveryTag, false)
                },
                CancelCallback {
                    logger.trace { "Consumer $it was cancelled" }
                }
            )
        }
    }
}
