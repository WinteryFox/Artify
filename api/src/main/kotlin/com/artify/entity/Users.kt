package com.artify.entity

import org.jetbrains.exposed.dao.id.EntityID

object Users {
    object Table : SnowflakeIdTable("users", "id")

    class Entity(id: EntityID<Snowflake>) : SnowflakeEntity(id) {
        companion object : SnowflakeEntityClass<Entity>(Table)
    }
}
