package com.artify.api.routes.users

import com.artify.api.entity.Users.Response.Companion.asResponse
import com.artify.api.routes.auth.getSelf
import com.artify.api.routes.auth.getUser
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.transactions.transaction

fun Route.usersRoute() {
    route("/users") {
        authenticate {
            route("/@me") {
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

        route("/{id}") {
            get {
                getUser(call.parameters["id"]!!)?.asResponse()?.let {
                    call.respond(HttpStatusCode.OK, it)
                } ?: call.respond(HttpStatusCode.NotFound)
            }
        }
    }
}