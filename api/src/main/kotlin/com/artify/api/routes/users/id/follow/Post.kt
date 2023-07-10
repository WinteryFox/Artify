package com.artify.api.routes.users.id.follow

import com.artify.api.entity.Follows
import com.artify.api.routes.auth.getSelf
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

fun Route.postFollow() {
    post {
        val self = getSelf()!!
        val id = UUID.fromString(call.parameters["id"]!!)

        if (self.id.value == id)
            return@post call.respond(HttpStatusCode.BadRequest)

        transaction {
            Follows.Table.insertIgnore {
                it[targetId] = id
                it[userId] = self.id
            }
        }

        return@post call.respond(HttpStatusCode.NoContent)
    }
}
