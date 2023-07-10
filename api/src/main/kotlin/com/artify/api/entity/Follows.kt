package com.artify.api.entity

object Follows {
    object Table : org.jetbrains.exposed.sql.Table("interactions.follows") {
        val targetId = reference("target_id", Users.Table)
        val userId = reference("user_id", Users.Table)
    }
}
