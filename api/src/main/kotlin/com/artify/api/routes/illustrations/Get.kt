package com.artify.api.routes.illustrations

import com.artify.api.entity.Follows
import com.artify.api.entity.Illustrations
import com.artify.api.entity.Illustrations.ResponseWithAuthor.Companion.asResponseWithAuthor
import com.artify.api.entity.Illustrations.Table.userId
import com.artify.api.routes.auth.getSelfId
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

const val limit = 15

private enum class Mode(
    val value: Short
) {
    TRENDING(0),
    RECENT(1),
    FOLLOWING(2);

    companion object {
        fun fromInt(value: Short) = entries.firstOrNull { it.value == value }
    }
}

fun Route.getIllustrations() {
    get {
        val selfId = getSelfId()
        val mode =
            call.request.queryParameters["mode"]?.toShortOrNull()?.let { Mode.fromInt(it) }
                ?: return@get call.respond(HttpStatusCode.BadRequest)

        val illustrations = transaction {
            when (mode) {
                Mode.TRENDING -> TODO()

                Mode.RECENT -> Illustrations.Entity.all()
                    .limit(limit)

                Mode.FOLLOWING -> Illustrations.Entity.find {
                    userId inSubQuery
                            Follows.Table.select { (Follows.Table.userId eq selfId) }
                                .adjustSlice { slice(Follows.Table.targetId) }
                }.limit(limit)
            }
                .orderBy(Illustrations.Table.id to SortOrder.DESC)
                .map { it.asResponseWithAuthor() }
        }

        return@get call.respond(HttpStatusCode.OK, illustrations)
    }
}
