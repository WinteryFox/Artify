package com.artify.route

import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider
import com.amazonaws.services.cognitoidp.model.*
import com.artify.entity.Users
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Serializable
data class Login(
    val email: String,
    val password: String
)

@Serializable
data class Register(
    val email: String,
    val password: String,
    val username: String
)

@Serializable
data class Verify(
    val email: String,
    val code: String
)

private fun secretHash(id: String, email: String, secret: String): String {
    val signingKey = SecretKeySpec(secret.toByteArray(), "HmacSHA256")
    val mac = Mac.getInstance("HmacSHA256")
    mac.init(signingKey)
    mac.update(email.toByteArray())

    return Base64.getEncoder().encodeToString(mac.doFinal(id.toByteArray()))
}

fun Route.authRoute(provider: AWSCognitoIdentityProvider) {
    val cognitoPoolId = application.environment.config.property("cognito.pool").getString()
    val cognitoClientId = application.environment.config.property("cognito.client.id").getString()
    val cognitoClientSecret = application.environment.config.property("cognito.client.secret").getString()

    route("/login") {
        post<Login> { request ->
            // TODO
            val result = try {
                provider.adminInitiateAuth(
                    AdminInitiateAuthRequest()
                        .withAuthFlow(AuthFlowType.ADMIN_USER_PASSWORD_AUTH)
                        .withAuthParameters(
                            mapOf(
                                "USERNAME" to request.email,
                                "PASSWORD" to request.password,
                                "SECRET_HASH" to secretHash(cognitoClientId, request.email, cognitoClientSecret)
                            )
                        )
                        .withUserPoolId(cognitoPoolId)
                        .withClientId(cognitoClientId)
                )
            } catch (e: NotAuthorizedException) {
                throw BadRequestException("Email or password is incorrect")
            }

            call.respond(HttpStatusCode.OK)
        }
    }

    route("/register") {
        post<Register> { request ->
            val result = try {
                provider.signUp(
                    SignUpRequest()
                        .withUsername(request.email)
                        .withPassword(request.password)
                        .withClientId(cognitoClientId)
                        .withSecretHash(secretHash(cognitoClientId, request.email, cognitoClientSecret))
                )
            } catch (e: InvalidPasswordException) {
                throw BadRequestException("Invalid password")
            } catch (e: UsernameExistsException) {
                throw BadRequestException("Email is taken")
            }

            transaction {
                Users.Entity.new(UUID.fromString(result.userSub)) {
                    username = request.username
                }
            }

            call.respond(HttpStatusCode.Created)
        }

        route("/verify") {
            post<Verify> { request ->
                try {
                    provider.confirmSignUp(
                        ConfirmSignUpRequest()
                            .withConfirmationCode(request.code)
                            .withUsername(request.email)
                            .withClientId(cognitoClientId)
                            .withSecretHash(secretHash(cognitoClientId, request.email, cognitoClientSecret))
                    )
                } catch (e: UserNotFoundException) {
                    throw BadRequestException("Email is not registered")
                } catch (e: CodeMismatchException) {
                    throw BadRequestException("Code is invalid")
                } catch (e: ExpiredCodeException) {
                    throw BadRequestException("Code is expired")
                }

                call.respond(HttpStatusCode.OK)
            }
        }
    }
}
