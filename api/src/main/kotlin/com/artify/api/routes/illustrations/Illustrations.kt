package com.artify.api.routes.illustrations

import aws.sdk.kotlin.services.s3.S3Client
import com.artify.api.routes.illustrations.id.deleteIllustration
import com.artify.api.routes.illustrations.id.getIllustration
import com.artify.api.routes.illustrations.id.patchIllustration
import com.rabbitmq.client.Connection
import io.ktor.server.auth.*
import io.ktor.server.routing.*

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
            authenticate(optional = true) {
                getIllustration()
            }

            authenticate {
                patchIllustration()

                deleteIllustration()
            }
        }
    }
}
