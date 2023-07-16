package com.artify.api.entity

object Likes {
    object Table : org.jetbrains.exposed.sql.Table("interactions.likes") {
        val postId = reference("post_id", Illustrations.Table)
        val userId = reference("user_id", Users.Table)
    }
}
