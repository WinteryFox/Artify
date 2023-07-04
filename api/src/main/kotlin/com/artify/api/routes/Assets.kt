package com.artify.api.routes

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
        route(Regex("^(?<hash>[a-f0-9]{32})((?=\\.(?<extension>[a-z]+)$)|$)")) {
            get {
                val hash = call.parameters["hash"]!!
                val extension = call.parameters["extension"]
                val contentType = parseContentType(extension)

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

                s3client.getObject(GetObjectRequest {
                    bucket = "artify-com"
                    this.key = key
                }) {
                    if (it.body == null)
                        return@getObject call.respond(HttpStatusCode.BadRequest)

                    call.response.cacheControl(
                        CacheControl.MaxAge(
                            31536000,
                            visibility = CacheControl.Visibility.Public
                        )
                    )

                    call.respondBytes(contentType, HttpStatusCode.OK) {
                        return@respondBytes it.body!!.toByteArray()
                    }
                }
            }
        }
    }
}

fun parseContentType(extension: String?): ContentType =
    if (extension.isNullOrBlank())
        ContentType.Image.PNG
    else
        ContentType.defaultForFileExtension(extension)
