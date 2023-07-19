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

const val default = 10
const val illustrations = 25

private enum class Mode(
    val value: Short
) {
    SIMILAR(0), // Any posts similar to the user's previous interests (liked work)
    NOVEL(1), // Similar but with 50% new content further away from interests
    TRENDING(2), // Anything with an increased amount of interactions the last week
    RECENT(3), // Sorted by most recent post date
    FOLLOWING(4); // Posts from whoever the user is following sorted by recency

    companion object {
        fun fromInt(value: Short) = entries.firstOrNull { it.value == value }
    }
}

fun Route.getIllustrations() {
    get {
        val selfId = getSelfId()
        val mode = call.request.queryParameters["mode"]?.toShortOrNull()?.let { Mode.fromInt(it) } ?: Mode.TRENDING
        val limit =
            call.request.queryParameters["limit"]?.toShortOrNull()?.coerceAtMost(illustrations.toShort()) ?: default

        val illustrations = transaction {
            when (mode) {
                Mode.SIMILAR -> TODO()

                Mode.NOVEL -> TODO()

                Mode.TRENDING -> TODO()

                Mode.RECENT -> Illustrations.Entity.all()
                    .limit(limit.toInt())
                    .orderBy(Illustrations.Table.id to SortOrder.DESC)

                Mode.FOLLOWING -> Illustrations.Entity.find {
                    userId inSubQuery
                            Follows.Table.select { (Follows.Table.userId eq selfId) }
                                .adjustSlice { slice(Follows.Table.targetId) }
                }.limit(limit.toInt())
            }
                .orderBy(Illustrations.Table.id to SortOrder.DESC)
                .map { it.asResponseWithAuthor() }
        }

        return@get call.respond(HttpStatusCode.OK, illustrations)
    }
}
