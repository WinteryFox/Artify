package com.artify.api.routes.illustrations.id

import com.artify.api.entity.Illustrations
import com.artify.api.entity.Illustrations.ResponseWithAuthor.Companion.asResponseWithAuthor
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.transactions.transaction

fun Route.getIllustration() {
    get {
        val id = try {
            call.parameters["id"]?.toLong()
                ?: return@get call.respond(HttpStatusCode.NotFound)
        } catch (e: NumberFormatException) {
            return@get call.respond(HttpStatusCode.NotFound)
        }

        val result = transaction {
            Illustrations.Entity
                .find { Illustrations.Table.id.eq(id) }
                .singleOrNull()
                ?.asResponseWithAuthor()
        }

        if (result != null)
            call.respond(HttpStatusCode.OK, result)
        else
            call.respond(HttpStatusCode.NotFound)
    }
}
