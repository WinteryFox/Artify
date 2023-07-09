package com.artify.api.routes.illustrations.id

import com.artify.api.routes.auth.getSelf
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.transactions.transaction

fun Route.deleteIllustration() {
    delete {
        val illustration = getIllustration() ?: return@delete call.respond(HttpStatusCode.NotFound)
        val user = getSelf() ?: return@delete call.respond(HttpStatusCode.Unauthorized)
        val author = transaction { illustration.author }

        if (author.id.value != user.id.value)
            return@delete call.respond(HttpStatusCode.Forbidden)

        transaction {
            illustration.delete()
        }

        call.respond(HttpStatusCode.NoContent)
    }
}
