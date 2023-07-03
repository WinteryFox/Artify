package com.artify

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.PutObjectResponse
import aws.sdk.kotlin.services.s3.putObject
import aws.smithy.kotlin.runtime.content.ByteStream
import java.awt.Image
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

fun BufferedImage.scale(width: Int, height: Int): BufferedImage {
    val scaled = getScaledInstance(width, height, Image.SCALE_SMOOTH)
    val buffered = BufferedImage(width, height, type)
    buffered.graphics.drawImage(scaled, 0, 0, null)

    return buffered
}

fun BufferedImage.toByteArray(type: String): ByteArray {
    val stream = ByteArrayOutputStream()
    ImageIO.write(
        this,
        type,
        stream
    )

    return stream.toByteArray()
}

suspend fun S3Client.putImage(bucket: String, key: String, image: BufferedImage, type: String): PutObjectResponse {
    val bytes = image.toByteArray(type.substringAfter('/'))

    return putObject {
        this.bucket = bucket
        this.key = key
        body = ByteStream.fromBytes(image.toByteArray(type))
        contentType = type
        contentLength = bytes.size.toLong()
    }
}
