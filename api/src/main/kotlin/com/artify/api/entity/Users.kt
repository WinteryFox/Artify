package com.artify.api.entity

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.*

object Users {
    object Table : UUIDTable("users", "id") {
        val email = text("email").uniqueIndex()
        val handle = text("handle")
        val username = text("username")
        //val discriminator = varchar("discriminator", 5)
        //val displayName = text("display_name")
        val avatar = text("avatar").nullable()
    }

    class Entity(id: EntityID<UUID>) : UUIDEntity(id) {
        companion object : UUIDEntityClass<Entity>(Table)

        var email by Table.email
        var handle by Table.handle
        var username by Table.username
        //var discriminator by Table.discriminator
        //var displayName by Table.displayName
        var avatar by Table.avatar
    }

    @Serializable
    data class Response(
        val id: String,
        val handle: String,
        val username: String,
        //val discriminator: String,
        //val displayName: String,
        val avatar: String?
    ) {
        companion object {
            fun Entity.asResponse() = Response(
                id.value.toString(),
                handle,
                username,
                //discriminator,
                //displayName,
                avatar
            )
        }
    }

    @Serializable
    data class ResponseWithEmail(
        val id: String,
        val email: String,
        val handle: String,
        val username: String,
        //val discriminator: String,
        //val displayName: String,
        val avatar: String?
    ) {
        companion object {
            fun Entity.asResponseWithEmail() = ResponseWithEmail(
                id.value.toString(),
                email,
                handle,
                username,
                //discriminator,
                //displayName,
                avatar
            )
        }
    }
}
