package com.artify.api.routes.users.id.follow

import com.artify.api.entity.Follows
import com.artify.api.routes.auth.getSelf
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

fun Route.deleteFollow() {
    delete {
        val self = getSelf()!!
        val id = UUID.fromString(call.parameters["id"]!!)

        if (self.id.value == id)
            return@delete call.respond(HttpStatusCode.BadRequest)

        transaction {
            Follows.Table.deleteWhere {
                userId.eq(self.id).and(targetId.eq(id))
            }
        }

        return@delete call.respond(HttpStatusCode.NoContent)
    }
}
