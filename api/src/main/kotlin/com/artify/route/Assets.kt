package com.artify.route

import com.amazonaws.AmazonServiceException
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.AmazonS3Exception
import io.github.oshai.KotlinLogging
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.assetsRoute(s3client: AmazonS3) {
    val logger = KotlinLogging.logger { }

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
                    if (size.isNullOrBlank()) {
                        s3client.getObject("artify-com", hash)
                    } else {
                        val objects = s3client.listObjects("artify-com", hash)
                        val `object` = objects.objectSummaries.find { it.key.endsWith("${size}x${size}") }
                            ?: return@get call.respond(HttpStatusCode.BadRequest)

                        s3client.getObject(`object`.bucketName, `object`.key)
                    }
                } catch (e: AmazonS3Exception) {
                    return@get call.respond(HttpStatusCode.NotFound)
                } catch (e: AmazonServiceException) {
                    logger.catching(e)
                    return@get call.respond(HttpStatusCode.ServiceUnavailable)
                }

                call.response.header(HttpHeaders.CacheControl, "public,max-age=31536000,immutable")
                call.respondBytes(contentType, HttpStatusCode.OK) {
                    file.objectContent.use {
                        it.readAllBytes()
                    }
                }
            }
        }
    }
}
