package com.artify.entity

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable

object Users {
    object Table : SnowflakeIdTable("users", "id")

    class Entity(id: EntityID<Snowflake>) : SnowflakeEntity(id) {
        companion object : SnowflakeEntityClass<Entity>(Table)
    }
}
