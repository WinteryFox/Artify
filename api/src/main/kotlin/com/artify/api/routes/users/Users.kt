package com.artify.api.routes.users

import com.artify.api.entity.Illustrations
import com.artify.api.entity.Illustrations.Response.Companion.asResponse
import com.artify.api.entity.Users.Response.Companion.asResponse
import com.artify.api.routes.auth.getSelf
import com.artify.api.routes.users.id.follow.deleteFollow
import com.artify.api.routes.users.id.follow.postFollow
import com.artify.api.routes.users.id.getUser
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

fun Route.usersRoute() {
    route("/users") {
        route("/@me") {
            authenticate {
                get {
                    call.respond(HttpStatusCode.OK, getSelf()!!.asResponse())
                }

                patch {
                    TODO()
                }

                delete {
                    transaction {
                        getSelf()!!.delete()
                    }

                    call.respond(HttpStatusCode.OK)
                }
            }
        }

        route(Regex("(?<id>[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})")) {
            getUser()

            get {
                val userId = UUID.fromString(call.parameters["id"])!!

                val illustrations = transaction {
                    Illustrations.Entity.find {
                        Illustrations.Table.userId.eq(userId)
                    }.map { it.asResponse() }
                }

                call.respond(HttpStatusCode.OK, illustrations)
            }

            route("/follow") {
                authenticate {
                    postFollow()

                    deleteFollow()
                }
            }
        }
    }
}
