package com.artify.api.entity

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.*

object Users {
    object Table : UUIDTable("users", "id") {
        val handle = text("handle").uniqueIndex()
        val username = text("username")
        val avatar = text("avatar").nullable()
    }

    class Entity(id: EntityID<UUID>) : UUIDEntity(id) {
        companion object : UUIDEntityClass<Entity>(Table)

        var handle by Table.handle
        var username by Table.username
        var avatar by Table.avatar
    }

    @Serializable
    data class Response(
        val id: String,
        val handle: String,
        val username: String,
        val avatar: String?
    ) {
        companion object {
            fun Entity.asResponse() = Response(
                id.value.toString(),
                handle,
                username,
                avatar
            )
        }
    }
}
