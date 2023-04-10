package com.artify.route.illustration

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.getIllustrations() {
    get {
        call.respond("Henlo, world!")
    }
}
