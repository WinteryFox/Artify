package com.artify.route

import com.amazonaws.services.s3.model.ObjectMetadata
import com.artify.connection
import com.artify.entity.Illustrations
import com.artify.entity.Illustrations.Response.Companion.asResponse
import com.artify.entity.Users
import com.artify.entity.defaultSnowflakeGenerator
import com.artify.image.ImageProcessorMessage
import com.artify.s3client
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.MessageProperties
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.commons.codec.binary.Hex
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.ByteArrayInputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.*
import javax.imageio.ImageIO

fun Route.illustrationsRoute() {
    route("/illustrations") {
        get {
            // TODO
            val illustrations = transaction {
                Illustrations.Entity
                    .all()
                    .limit(50)
                    .orderBy(Illustrations.Table.id to SortOrder.DESC)
                    .toList()
                    .map { it.asResponse() }
            }

            call.respond(HttpStatusCode.OK, illustrations)
        }

        post<Illustrations.Post> { request ->
            val user = transaction {
                // TODO
                Users.Entity.findById(UUID.fromString("9f96f252-a038-4cbc-967d-73ea0ef90146"))
            } ?: throw BadRequestException("")

            val entity = transaction {
                Illustrations.Entity.new(defaultSnowflakeGenerator.nextId().id) {
                    author = user
                    title = request.title
                    body = request.body
                    commentsEnabled = request.commentsEnabled
                    isPrivate = request.isPrivate
                    isAi = request.isAi
                }.asResponse()
            }

            // Process illustrations, put into bucket and dispatch RabbitMQ message for thumbnails
            request.illustrations.map { illustration ->
                async {
                    val mimeTypeEnd = illustration.indexOf(";base64,")
                    val mimeType = illustration.substring(5, mimeTypeEnd)

                    val data = Base64.getDecoder().decode(illustration.substring(mimeTypeEnd + 8))
                    val image = try {
                        ImageIO.read(data.inputStream())
                    } catch (e: IOException) {
                        throw BadRequestException("Failed to read one or multiple illustrations")
                    }

                    val hash = Hex.encodeHexString(MessageDigest.getInstance("MD5").digest(data))
                    s3client.putObject(
                        "artify-com",
                        "$hash/original",
                        ByteArrayInputStream(data),
                        ObjectMetadata().apply {
                            contentType = mimeType
                            contentLength = data.size.toLong()
                        }
                    )

                    launch {
                        val channel = connection.createChannel()
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
            }.awaitAll()

            call.respond(HttpStatusCode.Created, entity)
        }
    }
}
