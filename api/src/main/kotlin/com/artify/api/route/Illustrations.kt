package com.artify.api.route

import aws.sdk.kotlin.services.s3.S3Client
import com.artify.api.entity.Illustrations
import com.artify.api.entity.Illustrations.Response.Companion.asResponse
import com.artify.api.route.illustrations.getIllustrations
import com.artify.api.route.illustrations.id.getIllustration
import com.artify.api.route.illustrations.postIllustration
import com.rabbitmq.client.Connection
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

fun Route.illustrationsRoute(
    s3client: S3Client,
    amqpConnection: Connection
) {
    route("/illustrations") {
        authenticate(optional = true) {
            getIllustrations()
        }

        authenticate {
            postIllustration(s3client, amqpConnection)
        }

        route("/{id}") {
            getIllustration()
        }
    }

    route("/users/{id}/illustrations") {
        get {
            val userId = try {
                UUID.fromString(call.parameters["id"]
                    ?.ifBlank { return@get call.respond(HttpStatusCode.NotFound) }
                    ?: return@get call.respond(HttpStatusCode.NotFound))
            } catch (e: IllegalArgumentException) {
                return@get call.respond(HttpStatusCode.NotFound)
            }

            val illustrations = transaction {
                Illustrations.Entity.find {
                    Illustrations.Table.authorId.eq(userId)
                        .and(Illustrations.Table.isPrivate.neq(true))
                }.map { it.asResponse() }
            }

            call.respond(HttpStatusCode.OK, illustrations)
        }
    }
}
