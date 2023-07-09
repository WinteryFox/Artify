package com.artify.api.routes.illustrations.id

import com.artify.api.entity.Illustrations
import com.artify.api.routes.auth.getSelf
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.transactions.transaction

fun Route.patchIllustration() {
    patch<Illustrations.Patch> {
        val illustration = getIllustration() ?: return@patch call.respond(HttpStatusCode.NotFound)
        val user = getSelf() ?: return@patch call.respond(HttpStatusCode.Unauthorized)
        val author = transaction { illustration.author }

        if (author.id.value != user.id.value)
            return@patch call.respond(HttpStatusCode.Forbidden)

        transaction {
            illustration.apply {
                title = it.title
                body = it.body
                commentsEnabled = it.commentsEnabled
                isPrivate = it.isPrivate
                isAi = it.isAi
            }
        }

        call.respond(HttpStatusCode.NoContent)
    }
}
