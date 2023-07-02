package com.artify.api.route

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.listObjectsV2
import aws.sdk.kotlin.services.s3.model.GetObjectRequest
import aws.smithy.kotlin.runtime.content.toByteArray
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.assetsRoute(s3client: S3Client) {
    route("/assets") {
        route("/{hash}") {
            get {
                val arg = call.parameters["hash"]!!.split('.')
                val hash = arg[0]

                val contentType = if (arg.size > 1)
                    ContentType.defaultForFileExtension(arg[arg.size - 1])
                else
                    ContentType.Image.PNG

                if (contentType !in setOf(ContentType.Image.PNG, ContentType.Image.JPEG))
                    return@get call.respond(HttpStatusCode.UnsupportedMediaType)

                val size = call.request.queryParameters["size"]

                val key = if (size.isNullOrBlank())
                    hash
                else
                    s3client.listObjectsV2 {
                        bucket = "artify-com"
                        prefix = hash
                    }
                        .contents
                        ?.find { it.key?.endsWith("${size}x${size}") ?: false }
                        ?.key
                        ?: return@get call.respond(HttpStatusCode.NotFound)
                // TODO: There is a bug with listing objects that occasionally occurs where it fails with end of stream... I don't know the cause or fix, requires investigation.

                s3client.getObject(GetObjectRequest {
                    bucket = "artify-com"
                    this.key = key
                }) {
                    call.response.cacheControl(
                        CacheControl.MaxAge(
                            31536000,
                            visibility = CacheControl.Visibility.Public
                        )
                    )

                    if (it.body == null)
                        return@getObject call.respond(HttpStatusCode.BadRequest)

                    call.respondBytes(contentType, HttpStatusCode.OK) {
                        return@respondBytes it.body!!.toByteArray()
                    }
                }
            }
        }
    }
}
