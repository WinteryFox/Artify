package com.artify.api.route.illustrations

import com.artify.api.entity.Illustrations
import com.artify.api.entity.Illustrations.ResponseWithAuthor.Companion.asResponseWithAuthor
import com.artify.api.route.getSelf
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.transactions.transaction

fun Route.getIllustrations() {
    get {
        val user = getSelf()

        val illustrations = if (user == null) {
            transaction {
                Illustrations.Entity
                    .all()
                    .limit(50)
                    .orderBy(Illustrations.Table.id to SortOrder.DESC)
                    .map { it.asResponseWithAuthor() }
            }
        } else {
            // TODO: Apply some kind of recommendation algorithm
            transaction {
                Illustrations.Entity
                    .all()
                    .limit(50)
                    .orderBy(Illustrations.Table.id to SortOrder.DESC)
                    .map { it.asResponseWithAuthor() }
            }
        }

        call.respond(HttpStatusCode.OK, illustrations)
    }
}
