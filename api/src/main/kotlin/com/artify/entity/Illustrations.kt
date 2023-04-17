package com.artify.entity

import com.artify.entity.Users.Response.Companion.asResponse
import com.artify.textArray
import io.ktor.server.plugins.requestvalidation.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable

object Illustrations {
    object Table : LongIdTable("media.illustrations", "id") {
        val userId = reference("user_id", Users.Table)
        val title = text("title")
        val body = text("body")
        val commentsEnabled = bool("comments_enabled")
        val isPrivate = bool("is_private")
        val isAi = bool("is_ai")
        val hashes = textArray("hashes")
    }

    class Entity(id: EntityID<Long>) : LongEntity(id) {
        companion object : LongEntityClass<Entity>(Table)

        var author by Users.Entity referencedOn Table.userId
        var title by Table.title
        var body by Table.body
        var commentsEnabled by Table.commentsEnabled
        var isPrivate by Table.isPrivate
        var isAi by Table.isAi
        var hashes by Table.hashes
    }

    @Serializable
    data class Response(
        val id: Long,
        val author: Users.Response,
        val title: String,
        val body: String,
        @SerialName("comments_enabled")
        val commentsEnabled: Boolean,
        @SerialName("is_private")
        val isPrivate: Boolean,
        @SerialName("is_ai")
        val isAi: Boolean,
        val hashes: List<String>
    ) {
        companion object {
            fun Entity.asResponse() = Response(
                id.value,
                author.asResponse(),
                title,
                body,
                commentsEnabled,
                isPrivate,
                isAi,
                hashes.toList()
            )
        }
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
    ) {
        fun validate(): ValidationResult {
            if (title.length > 100)
                return ValidationResult.Invalid("Title is >100 characters")

            if (body.length > 5000)
                return ValidationResult.Invalid("Body is >5000 characters")

            if (illustrations.isEmpty())
                return ValidationResult.Invalid("Must at least upload one illustration")

            if (illustrations.size > 10)
                return ValidationResult.Invalid("Cannot upload more than 10 illustrations")

            for (illustration in illustrations) {
                if (!illustration.startsWith("data:"))
                    return ValidationResult.Invalid("One or multiple illustrations does not start with \"data:\"")

                val mimeTypeEnd = illustration.indexOf(";base64,")
                if (mimeTypeEnd == -1)
                    return ValidationResult.Invalid("One or multiple illustrations does not have a base64 extension")

                val mimeType = illustration.substring(5, mimeTypeEnd)
                if (mimeType !in setOf("image/png", "image/jpeg", "image/webp"))
                    return ValidationResult.Invalid("One or multiple illustrations has an unsupported MIME type")
            }

            return ValidationResult.Valid
        }
    }
}
