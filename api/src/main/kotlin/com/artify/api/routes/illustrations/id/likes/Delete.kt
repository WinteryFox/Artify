package com.artify.api.routes.illustrations.id.likes

import com.artify.api.entity.Likes
import com.artify.api.routes.auth.getSelfId
import com.artify.api.routes.illustrations.id.parseId
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.transaction

fun Route.deleteLike() {
    delete {
        val postId = parseId() ?: return@delete call.respond(HttpStatusCode.NotFound)
        val selfId = getSelfId() ?: return@delete call.respond(HttpStatusCode.Unauthorized)

        transaction { Likes.Table.deleteWhere { this.postId eq postId and (userId eq selfId) } }

        return@delete call.respond(HttpStatusCode.NoContent)
    }
}
