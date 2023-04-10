package com.artify.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.EntityID

object Illustrations {
    object Table : SnowflakeIdTable("media.illustrations", "id") {
        val userId = reference("user_id", Users.Table)
        val title = text("title")
        val body = text("body")
        val hasCommentsEnabled = bool("has_comments_enabled")
        val isPrivate = bool("is_private")
        val isAi = bool("is_ai")
    }

    class Entity(id: EntityID<Snowflake>) : SnowflakeEntity(id) {
        companion object : SnowflakeEntityClass<Entity>(Table)

        var user by Users.Entity referencedOn Table.userId
        var title by Table.title
        var body by Table.body
        var hasCommentsEnabled by Table.hasCommentsEnabled
        var isPrivate by Table.isPrivate
        var isAi by Table.isAi
    }

    @Serializable
    data class Post(
        val title: String,
        val body: String,
        @SerialName("comments_enabled")
        val commentsEnabled: Boolean,
        @SerialName("is_private")
        val isPrivate: Boolean,
        @SerialName("is_ai")
        val isAi: Boolean,
        val illustrations: List<String>
    )
}
