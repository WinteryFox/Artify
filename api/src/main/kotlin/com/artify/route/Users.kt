package com.artify.route

import com.artify.entity.Users.Response.Companion.asResponse
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
                    val user = getUser()
                    if (user != null)
                        call.respond(HttpStatusCode.OK, user.asResponse())
                    else
                        call.respond(HttpStatusCode.Unauthorized) // TODO: Extra insert since the token *is* valid?
                }
            }
        }

        route("/{id}") {
            get {

            }
        }
    }
}
