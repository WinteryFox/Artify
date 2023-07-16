package com.artify.api.routes.illustrations.id

import com.artify.api.entity.Illustrations
import com.artify.api.entity.Illustrations.ResponseWithAuthor.Companion.asResponseWithAuthor
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction

fun PipelineContext<*, ApplicationCall>.parseId(): Long? =
    try {
        call.parameters["id"]?.toLong()
    } catch (e: NumberFormatException) {
        null
    }

fun PipelineContext<*, ApplicationCall>.getIllustration(): Illustrations.Entity? =
    parseId()?.let { getIllustration(it) }

fun getIllustration(id: Long): Illustrations.Entity? =
    transaction {
        Illustrations.Entity
            .find { Illustrations.Table.id eq id and (Illustrations.Table.isPrivate neq true) }
            .singleOrNull()
    }

fun Route.getIllustration() {
    get {
        val illustration = getIllustration()?.asResponseWithAuthor()

        if (illustration != null)
            call.respond(HttpStatusCode.OK, illustration)
        else
            call.respond(HttpStatusCode.NotFound)
    }
}
