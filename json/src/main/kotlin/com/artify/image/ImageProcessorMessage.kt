package com.artify.image

import kotlinx.serialization.Serializable

@Serializable
data class ImageProcessorMessage(
    val hash: String,
    val dimensions: Set<Dimension>
) {
    @Serializable
    data class Dimension(
        val x: Int,
        val y: Int,
        val width: Int,
        val height: Int
    )
}
