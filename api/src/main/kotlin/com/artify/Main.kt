package com.artify

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.autohead.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json

fun main(args: Array<String>): Unit = EngineMain.main(args)

@Suppress("unused")
fun Application.application() {
    install(AutoHeadResponse)
    install(ContentNegotiation) {
        json(Json {
            encodeDefaults = false
        })
    }

    routing {
        trace {
            application.log.trace(it.buildText())
        }

        route("/api") {
            route("/posts") {
                get {
                    // TODO: Fetch posts
                }

                post<String> {

                }
            }
        }
    }
}
