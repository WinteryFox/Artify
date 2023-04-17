package com.artify.route

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.AmazonS3Exception
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.assetsRoute(s3client: AmazonS3) {
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

                val file = try {
                    if (size.isNullOrBlank())
                        s3client.getObject("artify-com", "$hash/original")
                    else
                        s3client.getObject("artify-com", "$hash/${size}x${size}")
                } catch (e: AmazonS3Exception) {
                    return@get call.respond(HttpStatusCode.NotFound)
                }

                call.respondOutputStream(contentType, HttpStatusCode.OK) {
                    file.objectContent.use {
                        write(it.readAllBytes())
                    }
                }
            }
        }
    }
}
