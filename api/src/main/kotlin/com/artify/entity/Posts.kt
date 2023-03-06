package com.artify.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.EntityID

object Illustrations {
    object Table : SnowflakeIdTable("media.posts", "id") {
        val userId = reference("user_id", Users.Table)
    }

    class Entity(id: EntityID<Snowflake>) : SnowflakeEntity(id) {
        companion object : SnowflakeEntityClass<Entity>(Table)

        var user by Users.Entity referencedOn Table.userId
    }

    @Serializable
    data class Post(
        val id: Long,
        val author: Long,
        val title: String,
        val content: String,
        @SerialName("comments_enabled")
        val commentsEnabled: Boolean,
        @SerialName("is_private")
        val isPrivate: Boolean,
        @SerialName("is_ai")
        val isAi: Boolean,
        val illustrations: List<String>
    )
}
