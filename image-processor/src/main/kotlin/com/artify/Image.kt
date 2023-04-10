package com.artify

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.PutObjectResult
import java.awt.Image
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
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

fun AmazonS3.putImage(bucket: String, key: String, image: BufferedImage, type: String): PutObjectResult {
    val bytes = image.toByteArray(type)

    return putObject(
        bucket,
        key,
        ByteArrayInputStream(bytes),
        ObjectMetadata().apply {
            contentType = type
            contentLength = bytes.size.toLong()
        }
    )
}
