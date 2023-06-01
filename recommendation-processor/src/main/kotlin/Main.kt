package com.artify

import com.artify.json.message.LikeMessage
import com.rabbitmq.client.CancelCallback
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.DeliverCallback
import io.github.oshai.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream

@OptIn(ExperimentalSerializationApi::class)
suspend fun main() {
    val logger = KotlinLogging.logger {}
    logger.info { "Henlo! Recommendation processor microservice starting!" }
    logger.info { "Connecting to RabbitMQ" }

    val factory = ConnectionFactory().apply {
        if (System.getenv("RABBITMQ_SSL").toBoolean())
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

            channel.queueDeclare("like_queue", true, false, false, null)
            channel.basicConsume(
                "like_queue",
                false,
                DeliverCallback { consumerTag, message ->
                    logger.trace { "Received new message for consumer $consumerTag" }

                    val body = Json.decodeFromStream<LikeMessage>(message.body.inputStream())

                    // TODO: Run algorithm and stuff
                    logger.info { "Like is from user ${body.userId} and illustration is ${body.illustrationId}" }

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
