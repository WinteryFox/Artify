package com.artify.entity

import com.artify.entity.Illustrations.Table.clientDefault
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ColumnType
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.vendors.currentDialect

open class SnowflakeIdTable(name: String = "", columnName: String = "id") : IdTable<Snowflake>(name) {
    final override val id: Column<EntityID<Snowflake>> = snowflake(columnName).autoGenerate().entityId()
    final override val primaryKey = PrimaryKey(id)
}

fun Table.snowflake(name: String): Column<Snowflake> = registerColumn(name, SnowflakeColumnType())

fun Column<Snowflake>.autoGenerate(): Column<Snowflake> = clientDefault { defaultSnowflakeGenerator.nextId() }

class SnowflakeColumnType : ColumnType() {
    override fun sqlType(): String = currentDialect.dataTypeProvider.longType()

    override fun valueFromDB(value: Any): Long = when (value) {
        is Long -> value
        is Number -> value.toLong()
        is String -> value.toLong()
        else -> error("Unexpected value of type Long: $value of ${value::class.qualifiedName}")
    }
}

abstract class SnowflakeEntity(id: EntityID<Snowflake>) : Entity<Snowflake>(id)

abstract class SnowflakeEntityClass<out E : SnowflakeEntity>(
    table: IdTable<Snowflake>,
    entityType: Class<E>? = null,
    entityCtor: ((EntityID<Snowflake>) -> E)? = null
) : EntityClass<Snowflake, E>(table, entityType, entityCtor)
