package com.artify

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.GetObjectRequest
import aws.sdk.kotlin.services.s3.model.S3Exception
import aws.smithy.kotlin.runtime.content.toByteArray
import com.artify.json.message.ImageProcessorMessage
import com.rabbitmq.client.CancelCallback
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.DeliverCallback
import io.github.oshai.kotlinlogging.KotlinLogging
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

    val s3client = S3Client.fromEnvironment {
        region = System.getenv("AWS_S3_REGION")
    }

    val factory = ConnectionFactory().apply {
        if (System.getenv("RABBITMQ_SSL").toBoolean())
            useSslProtocol()
        host = System.getenv("RABBITMQ_HOST")
        virtualHost = System.getenv("RABBITMQ_VHOST")
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

                        try {
                            s3client.getObject(GetObjectRequest {
                                bucket = "artify-com"
                                key = body.hash
                            }) { `object` ->
                                val image = ImageIO.read(`object`.body!!.toByteArray().inputStream())

                                val cropped = image.getSubimage(body.position.x, body.position.y, body.size.x, body.size.y)

                                body.scales.map {
                                    launch {
                                        logger.trace { "Generating scaled down thumbnail at ${it.x}x${it.y}" }

                                        s3client.putImage(
                                            "artify-com",
                                            "${body.hash}/${it.x}x${it.y}",
                                            cropped.scale(it.x, it.y),
                                            `object`.contentType!!
                                        )

                                        logger.trace { "Finished generating thumbnail at ${it.x}x${it.y}" }
                                    }
                                }.joinAll()

                                logger.trace { "Finished generating thumbnails for hash ${body.hash}" }
                            }
                        } catch (e: S3Exception) {
                            logger.error { "Failed to fetch object ${body.hash}" }
                            return@runBlocking
                        }
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
