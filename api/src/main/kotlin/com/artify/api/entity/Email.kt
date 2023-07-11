package com.artify.api.entity

import org.jetbrains.exposed.sql.javatime.timestamp

object Email {
    object Table : org.jetbrains.exposed.sql.Table("tokens.email") {
        val token = text("token")
        val email = reference("email", Users.Table.email)
        val expiry = timestamp("expiry")
    }
}
