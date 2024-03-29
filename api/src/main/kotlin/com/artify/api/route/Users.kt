package com.artify.api.route

import com.artify.api.entity.Users.Response.Companion.asResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.usersRoute() {
    route("/users") {
        authenticate {
            route("/@me") {
                get {
                    call.respond(HttpStatusCode.OK, getSelf()!!.asResponse())
                }
            }

            patch {
                TODO()
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
