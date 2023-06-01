package com.artify.json.message

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LikeMessage(
    @SerialName("user_id")
    val userId: String,
    @SerialName("illustration_id")
    val illustrationId: Long
)
