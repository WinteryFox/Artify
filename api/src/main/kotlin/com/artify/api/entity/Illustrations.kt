package com.artify.api.entity

import com.artify.api.entity.Users.Response.Companion.asResponse
import com.artify.api.textArray
import io.ktor.server.plugins.requestvalidation.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.transactions.transaction

object Illustrations {
    const val MAX_TITLE_LENGTH = 100
    const val MAX_BODY_LENGTH = 5000
    const val MAX_ILLUSTRATIONS = 10

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
        val id: String,
        @SerialName("author")
        val author: String,
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
                id.value.toString(),
                author.id.value.toString(),
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
    data class ResponseWithAuthor(
        val id: String,
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
            fun Entity.asResponseWithAuthor(): ResponseWithAuthor {
                val author = transaction { author.asResponse() }

                return ResponseWithAuthor(
                    id.value.toString(),
                    author,
                    title,
                    body,
                    commentsEnabled,
                    isPrivate,
                    isAi,
                    hashes.toList()
                )
            }
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
            val reasons = mutableListOf<String>()

            if (title.isBlank() || title.length > MAX_TITLE_LENGTH)
                reasons.add("Title may not be blank or >100 characters")

            if (body.isBlank() || body.length > MAX_BODY_LENGTH)
                reasons.add("Body may not be blank or >5000 characters")

            if (illustrations.isEmpty())
                reasons.add("Must at least upload one illustration")

            if (illustrations.size > MAX_ILLUSTRATIONS)
                reasons.add("May not upload more than 10 illustrations")

            for (illustration in illustrations) {
                if (!illustration.startsWith("data:"))
                    reasons.add("One or multiple illustrations does not start with \"data:\"")

                val mimeTypeEnd = illustration.indexOf(";base64,")
                if (mimeTypeEnd == -1)
                    reasons.add("One or multiple illustrations does not have a base64 extension")

                val mimeType = illustration.substring(5, mimeTypeEnd)
                if (mimeType !in setOf("image/png", "image/jpeg", "image/webp"))
                    reasons.add("One or multiple illustrations has an unsupported MIME type")
            }

            return if (reasons.isEmpty())
                ValidationResult.Valid
            else
                ValidationResult.Invalid(reasons)
        }
    }

    @Serializable
    data class Patch(
        val title: String? = null,
        val body: String? = null,
        @SerialName("comments_enabled")
        val commentsEnabled: Boolean? = null,
        @SerialName("is_private")
        val isPrivate: Boolean? = null,
        @SerialName("is_ai")
        val isAi: Boolean? = null
    ) {
        fun validate(): ValidationResult {
            val reasons = mutableListOf<String>()

            if (title != null && (title.isBlank() || title.length > MAX_TITLE_LENGTH))
                reasons.add("Title may not be blank or >100 characters")

            if (body != null && (body.isBlank() || body.length > MAX_BODY_LENGTH))
                reasons.add("Body may not be blank or >5000 characters")

            return if (reasons.isEmpty())
                ValidationResult.Valid
            else
                ValidationResult.Invalid(reasons)
        }
    }
}
