package com.artify.api.routes.auth

import aws.sdk.kotlin.services.cognitoidentityprovider.*
import com.artify.api.entity.Users
import com.artify.api.routes.auth.register.register
import com.artify.api.routes.auth.register.verify
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

@Serializable
data class Login(
    @SerialName("email")
    val username: String,
    val password: String,
    val device: Device? = null
)

@Serializable
data class Device(
    @SerialName("key")
    val key: String,
    @SerialName("group_key")
    val groupKey: String,
    @SerialName("password")
    val password: String,
)

@Serializable
data class Refresh(
    val id: String,
    @SerialName("refresh_token")
    val refreshToken: String,
    @SerialName("device_key")
    val deviceKey: String
)

@Serializable
data class Jwt(
    @SerialName("expires_in")
    val expiresIn: Int,
    val type: String,
    @SerialName("id_token")
    val idToken: String,
    @SerialName("access_token")
    val accessToken: String,
    @SerialName("refresh_token")
    val refreshToken: String? = null,
    val device: Device? = null
)

@Serializable
data class Register(
    val email: String,
    val password: String,
    val handle: String,
    val username: String
)

@Serializable
data class Verify(
    val email: String,
    val code: String
)

fun Route.authRoute(provider: CognitoIdentityProviderClient) {
    val cognitoPoolId = application.environment.config.property("aws.cognito.pool").getString()
    val cognitoClientId = application.environment.config.property("aws.cognito.client.id").getString()
    val cognitoClientSecret = application.environment.config.property("aws.cognito.client.secret").getString()

    route("/refresh") {
        refresh(cognitoClientId, cognitoClientSecret, cognitoPoolId, provider)
    }

    route("/login") {
        login(cognitoClientId, cognitoClientSecret, cognitoPoolId, provider)
    }

    route("/register") {
        register(cognitoClientId, cognitoClientSecret, provider)

        route("/verify") {
            verify(cognitoClientId, cognitoClientSecret, provider)
        }
    }
}

fun PipelineContext<*, ApplicationCall>.getSelf(): Users.Entity? {
    val principal = call.principal<JWTPrincipal>() ?: return null
    val uuid = try {
        UUID.fromString(principal.subject)
    } catch (e: IllegalArgumentException) {
        return null
    }

    return transaction {
        Users.Entity.findById(uuid)
    }
}

fun getUser(id: String): Users.Entity? {
    val uuid = try {
        UUID.fromString(id)
    } catch (e: IllegalArgumentException) {
        return null
    }

    return transaction {
        Users.Entity.findById(uuid)
    }
}
