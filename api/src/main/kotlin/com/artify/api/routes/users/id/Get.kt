package com.artify.api.routes.users.id

import com.artify.api.entity.Users.Response.Companion.asResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.getUser() {
    get {
        com.artify.api.routes.auth.getUser(call.parameters["id"]!!)?.asResponse()?.let {
            call.respond(HttpStatusCode.OK, it)
        } ?: call.respond(HttpStatusCode.NotFound)
    }
}
