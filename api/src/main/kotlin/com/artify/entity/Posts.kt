package com.artify.entity

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable

object Posts {
    object Table : SnowflakeIdTable("media.posts", "id") {
        val userId = reference("user_id", Users.Table)
    }

    class Entity(id: EntityID<Snowflake>) : SnowflakeEntity(id) {
        companion object : SnowflakeEntityClass<Entity>(Table)

        var user by Users.Entity referencedOn Table.userId
    }
}
