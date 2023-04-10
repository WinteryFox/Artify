package com.artify

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.ObjectMetadata
import com.artify.entity.Illustrations
import com.artify.image.ImageProcessorMessage
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.MessageProperties
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.autohead.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.requestvalidation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.commons.codec.binary.Hex
import org.jetbrains.exposed.sql.Database
import java.io.ByteArrayInputStream
import java.security.MessageDigest
import java.util.*

fun main(args: Array<String>): Unit = EngineMain.main(args)

@Suppress("unused")
fun Application.application() {
    Database.connect(HikariDataSource {
        driverClassName = "org.postgresql.Driver"
        jdbcUrl = System.getenv("POSTGRES_HOST")
        username = System.getenv("POSTGRES_USER")
        password = System.getenv("POSTGRES_PASSWORD")
        isAutoCommit = false
        transactionIsolation = "TRANSACTION_REPEATABLE_READ"
    })

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
    val connection = factory.newConnection(System.getenv("RABBITMQ_HOST"))

    install(AutoHeadResponse)
    install(ContentNegotiation) {
        json(Json {
            encodeDefaults = false
        })
    }
    install(RequestValidation) {
        validate<Illustrations.Post> {
            // TODO
            ValidationResult.Valid
        }
    }
    install(StatusPages) {
        exception<RequestValidationException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, cause.reasons.joinToString())
        }
    }

    routing {
        trace {
            application.log.trace(it.buildText())
        }

        route("/api") {
            route("/illustrations") {
                get {
                    // TODO: Fetch posts
                }

                post<Illustrations.Post> { body ->
                    for (illustration in body.illustrations) {
                        if (!illustration.startsWith("data:"))
                            throw BadRequestException("One or multiple illustrations does not start with \"data:\"")

                        val mimeTypeEnd = illustration.indexOf(";base64,")
                        if (mimeTypeEnd == -1)
                            throw BadRequestException("One or multiple illustrations does not have a base64 extension")

                        val mimeType = illustration.substring(5, mimeTypeEnd)
                        if (mimeType !in setOf("image/png", "image/jpeg", "image/gif", "image/webp"))
                            throw BadRequestException("One or multiple illustrations has an unsupported MIME type")

                        val data = Base64.getDecoder().decode(illustration.substring(mimeTypeEnd + 8))
                        val hash = Hex.encodeHexString(MessageDigest.getInstance("MD5").digest(data))
                        s3client.putObject("artify-com", "$hash/original", ByteArrayInputStream(data), ObjectMetadata().apply {
                            contentType = mimeType
                            contentLength = data.size.toLong()
                        })

                        launch {
                            val channel = connection.createChannel()
                            channel.queueDeclare("scaling_queue", true, false, false, null)
                            channel.basicPublish(
                                "",
                                "scaling_queue",
                                AMQP.BasicProperties.Builder()
                                    .deliveryMode(MessageProperties.PERSISTENT_BASIC.deliveryMode).build(),
                                Json.encodeToString(
                                    ImageProcessorMessage(
                                        hash,
                                        setOf(
                                            ImageProcessorMessage.Dimension(0, 0, 480, 480)
                                        )
                                    )
                                ).toByteArray()
                            )
                        }
                    }

                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }
}

fun HikariDataSource(builder: HikariConfig.() -> Unit): HikariDataSource {
    val config = HikariConfig()
    config.apply(builder)
    config.validate()
    return HikariDataSource(config)
}
