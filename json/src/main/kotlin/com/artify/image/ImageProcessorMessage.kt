package com.artify.image

import kotlinx.serialization.Serializable

@Serializable
data class ImageProcessorMessage(
    val hash: String,
    val position: Dimension,
    val size: Dimension,
    val scales: Set<Dimension>
) {
    @Serializable
    data class Dimension(
        val x: Int,
        val y: Int
    )
}
