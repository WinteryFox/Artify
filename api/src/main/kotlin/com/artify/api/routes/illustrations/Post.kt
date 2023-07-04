package com.artify.api.routes.illustrations

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.putObject
import aws.smithy.kotlin.runtime.content.ByteStream
import com.artify.api.entity.Illustrations
import com.artify.api.entity.Illustrations.Response.Companion.asResponse
import com.artify.api.entity.defaultSnowflakeGenerator
import com.artify.api.routes.auth.getSelf
import com.artify.json.message.ImageProcessorMessage
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Connection
import com.rabbitmq.client.MessageProperties
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.transactions.transaction
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.*
import javax.imageio.ImageIO

fun Route.postIllustration(
    s3client: S3Client,
    amqpConnection: Connection
) {
    post<Illustrations.Post> { request ->
        val user = getSelf() ?: throw BadRequestException("Unknown user")

        // Process illustrations, put into bucket and dispatch RabbitMQ message for thumbnails
        val images = request.illustrations.map { illustration ->
            async {
                val mimeTypeEnd = illustration.indexOf(";base64,")

                val data = Base64.getDecoder().decode(illustration.substring(mimeTypeEnd + 8))
                val image = try {
                    ImageIO.read(data.inputStream())
                } catch (e: IOException) {
                    throw BadRequestException("Failed to read one or multiple illustrations")
                }

                val stream = ByteArrayOutputStream()
                ImageIO.write(
                    image,
                    "png",
                    stream
                )
                val bytes = stream.toByteArray()

                val hash = HexFormat.of().formatHex(MessageDigest.getInstance("MD5").digest(data))
                s3client.putObject {
                    bucket = "artify-com"
                    key = hash
                    body = ByteStream.fromBytes(bytes)
                    metadata = mapOf(
                        "Content-Type" to "image/png",
                        "Content-Length" to bytes.size.toString()
                    )
                }

                return@async hash to image
            }
        }.awaitAll().toMap()

        val entity = transaction {
            Illustrations.Entity.new(defaultSnowflakeGenerator.nextId().id) {
                author = user
                title = request.title
                body = request.body
                commentsEnabled = request.commentsEnabled
                isPrivate = request.isPrivate
                isAi = request.isAi
                hashes = images.keys.toTypedArray()
            }.asResponse()
        }

        call.respond(HttpStatusCode.Created, entity)

        for (image in images)
            dispatchImageProcessingMessage(amqpConnection, image.key, image.value)
    }
}

private suspend fun dispatchImageProcessingMessage(
    amqpConnection: Connection,
    hash: String,
    image: BufferedImage,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    withContext(dispatcher) {
        val channel = amqpConnection.createChannel()
        channel.queueDeclare("scaling_queue", true, false, false, null)

        val cropWidth = if (image.width < image.height) image.width else image.height
        val cropHeight = if (image.height < image.width) image.height else image.width
        val cropPosition = ImageProcessorMessage.Dimension(
            (image.width / 2) - (cropWidth / 2),
            (image.height / 2) - (cropHeight / 2)
        )
        val cropSize = ImageProcessorMessage.Dimension(cropWidth, cropHeight)
        val scales = listOf(
            ImageProcessorMessage.Dimension(512, 512),
            ImageProcessorMessage.Dimension(256, 256),
            ImageProcessorMessage.Dimension(128, 128)
        ).filter { it.x < cropWidth && it.y < cropHeight }.toSet()

        channel.basicPublish(
            "",
            "scaling_queue",
            AMQP.BasicProperties.Builder()
                .deliveryMode(MessageProperties.PERSISTENT_BASIC.deliveryMode).build(),
            Json.encodeToString(
                ImageProcessorMessage(
                    hash,
                    cropPosition,
                    cropSize,
                    scales
                )
            ).toByteArray()
        )
    }
}
