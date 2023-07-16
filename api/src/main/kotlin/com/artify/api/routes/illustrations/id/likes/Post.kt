package com.artify.api.routes.illustrations.id.likes

import com.artify.api.entity.Likes
import com.artify.api.routes.auth.getSelf
import com.artify.api.routes.illustrations.id.getIllustration
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.transactions.transaction

fun Route.postLike() {
    post {
        val self = getSelf() ?: return@post call.respond(HttpStatusCode.Unauthorized)
        val illustration = getIllustration() ?: return@post call.respond(HttpStatusCode.NotFound)

        transaction {
            Likes.Table.insertIgnore {
                it[postId] = illustration.id
                it[userId] = self.id
            }
        }

        return@post call.respond(HttpStatusCode.Created)
    }
}
