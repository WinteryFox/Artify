package com.artify.api

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.regions.Regions
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProviderClientBuilder
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.artify.api.entity.Illustrations
import com.artify.api.route.assetsRoute
import com.artify.api.route.authRoute
import com.artify.api.route.illustrationsRoute
import com.artify.api.route.usersRoute
import com.auth0.jwk.JwkProviderBuilder
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.autohead.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.plugins.requestvalidation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Database
import java.util.concurrent.TimeUnit

fun main(args: Array<String>): Unit = EngineMain.main(args)

@Suppress("unused")
fun Application.api() {
    val logger = KotlinLogging.logger { }

    val s3client: AmazonS3 = AmazonS3ClientBuilder
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

    val cognitoProvider: AWSCognitoIdentityProvider = AWSCognitoIdentityProviderClientBuilder
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
        if (environment.config.property("rabbitmq.ssl").getString().toBoolean())
            useSslProtocol()
        host = environment.config.property("rabbitmq.host").getString()
        virtualHost = environment.config.property("rabbitmq.vhost").getString()
        port = environment.config.property("rabbitmq.port").getString().toInt()
        username = environment.config.property("rabbitmq.username").getString()
        password = environment.config.property("rabbitmq.password").getString()
    }

    val connection: Connection = factory.newConnection()

    Database.connect(HikariDataSource {
        driverClassName = "org.postgresql.Driver"
        jdbcUrl = environment.config.property("postgres.host").getString()
        username = environment.config.property("postgres.username").getString()
        password = environment.config.property("postgres.password").getString()
        isAutoCommit = false
        transactionIsolation = "TRANSACTION_REPEATABLE_READ"
    })

    install(DefaultHeaders)
    install(CORS) {
        allowCredentials = true
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Head)
        allowHeader(HttpHeaders.AccessControlAllowHeaders)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.AccessControlAllowOrigin)
        allowHeader(HttpHeaders.Authorization)
        anyHost()
    }
    install(AutoHeadResponse)
    install(ContentNegotiation) {
        json(Json {
            encodeDefaults = false
        })
    }
    install(RequestValidation) {
        validate<Illustrations.Post> {
            it.validate()
        }
    }
    install(StatusPages) {
        exception<RequestValidationException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, cause.reasons.joinToString())
        }

        exception<ExceptionWithStatusCode> { call, cause ->
            if (cause.cause != null)
                logger.catching(cause.cause!!)

            call.respond(cause.status, cause.code)
        }
    }
    install(Authentication) {
        jwt {
            realm = this@api.environment.config.property("aws.cognito.client.id").getString()
            val issuer = this@api.environment.config.property("aws.cognito.issuer").getString()
            val provider = JwkProviderBuilder(issuer)
                .cached(10, 24, TimeUnit.HOURS)
                .rateLimited(10, 1, TimeUnit.MINUTES)
                .build()

            verifier(
                provider,
                issuer
            )
            validate { credentials ->
                if (credentials.issuer == issuer)
                    JWTPrincipal(credentials.payload)
                else
                    null
            }
        }
    }

    routing {
        trace {
            application.log.trace(it.buildText())
        }

        route("/api") {
            authRoute(cognitoProvider)
            usersRoute()
            illustrationsRoute(s3client, connection)
            assetsRoute(s3client)
        }
    }
}

fun HikariDataSource(builder: HikariConfig.() -> Unit): HikariDataSource {
    val config = HikariConfig()
    config.apply(builder)
    config.validate()
    return HikariDataSource(config)
}