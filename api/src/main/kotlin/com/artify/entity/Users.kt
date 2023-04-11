package com.artify.entity

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.*

object Users {
    object Table : UUIDTable("users", "id") {
        val username = text("username")
        val avatar = text("avatar").nullable()
    }

    class Entity(id: EntityID<UUID>) : UUIDEntity(id) {
        companion object : UUIDEntityClass<Entity>(Table)

        var username by Table.username
        var avatar by Table.avatar
    }
}
