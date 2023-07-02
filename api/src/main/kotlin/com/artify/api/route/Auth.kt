package com.artify.api.route

import aws.sdk.kotlin.services.cognitoidentityprovider.*
import aws.sdk.kotlin.services.cognitoidentityprovider.model.*
import com.artify.api.Code
import com.artify.api.ExceptionWithStatusCode
import com.artify.api.aws.Auth
import com.artify.api.aws.DeviceHelper
import com.artify.api.aws.secretHash
import com.artify.api.entity.Users
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
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
    val logger = KotlinLogging.logger { }
    val cognitoPoolId = application.environment.config.property("aws.cognito.pool").getString()
    val cognitoClientId = application.environment.config.property("aws.cognito.client.id").getString()
    val cognitoClientSecret = application.environment.config.property("aws.cognito.client.secret").getString()

    route("/refresh") {
        post<Refresh> { request ->
            val result = try {
                provider.adminInitiateAuth {
                    authFlow = AuthFlowType.RefreshTokenAuth
                    authParameters = mapOf(
                        "REFRESH_TOKEN" to request.refreshToken,
                        "DEVICE_KEY" to request.deviceKey,
                        "SECRET_HASH" to secretHash(cognitoClientId, request.id, cognitoClientSecret)
                    )
                    userPoolId = cognitoPoolId
                    clientId = cognitoClientId
                }
            } catch (e: NotAuthorizedException) {
                return@post call.respond(HttpStatusCode.Unauthorized)
            /*} catch (e: TokenExpiredException) { TODO
                return@post call.respond(HttpStatusCode.Unauthorized)*/
            } catch (e: UserNotFoundException) {
                return@post call.respond(HttpStatusCode.Unauthorized)
            } catch (e: CognitoIdentityProviderException) {
                logger.catching(e)
                return@post call.respond(HttpStatusCode.InternalServerError)
            }

            if (result.challengeName == null)
                call.respond(
                    HttpStatusCode.OK, Jwt(
                        result.authenticationResult!!.expiresIn,
                        result.authenticationResult!!.tokenType!!,
                        result.authenticationResult!!.idToken!!,
                        result.authenticationResult!!.accessToken!!,
                        result.authenticationResult!!.refreshToken,
                        null
                    )
                )
            else
                TODO("Handle additional challenges")
        }
    }

    route("/login") {
        post<Login> { request ->
            try {
                val authHelper = Auth(
                    cognitoPoolId,
                    cognitoClientId,
                    cognitoClientSecret,
                    if (request.device != null)
                        DeviceHelper(request.device.key, request.device.groupKey)
                    else
                        null
                )

                val authenticationResult = authHelper.adminInitiateAuth(
                    provider,
                    request.username,
                    request.password,
                    request.device
                )

                val deviceConfig = if (request.device == null) {
                    val deviceHelper = DeviceHelper(
                        authenticationResult.newDeviceMetadata!!.deviceKey!!,
                        authenticationResult.newDeviceMetadata!!.deviceGroupKey!!
                    )
                    val config = deviceHelper.passwordVerifierConfig()

                    provider.confirmDevice {
                        deviceKey = authenticationResult.newDeviceMetadata!!.deviceKey
                        deviceName = call.request.userAgent()
                        accessToken = authenticationResult.accessToken
                        deviceSecretVerifierConfig {
                            salt = config.salt
                            passwordVerifier = config.passwordVerifier
                        }
                    }

                    config
                } else null

                call.respond(
                    HttpStatusCode.OK, Jwt(
                        authenticationResult.expiresIn,
                        authenticationResult.tokenType!!,
                        authenticationResult.idToken!!,
                        authenticationResult.accessToken!!,
                        authenticationResult.refreshToken,
                        if (deviceConfig == null)
                            null
                        else
                            Device(
                                authenticationResult.newDeviceMetadata!!.deviceKey!!,
                                authenticationResult.newDeviceMetadata!!.deviceGroupKey!!,
                                deviceConfig.devicePassword
                            )
                    )
                )
            } catch (_: InvalidParameterException) {
                throw ExceptionWithStatusCode(HttpStatusCode.BadRequest, Code.BadCredentials)
            } catch (e: NotAuthorizedException) {
                when (e.message) {
                    "User is disabled." -> throw ExceptionWithStatusCode(
                        HttpStatusCode.BadRequest,
                        Code.AccountDisabled
                    )

                    else -> throw ExceptionWithStatusCode(HttpStatusCode.BadRequest, Code.BadCredentials)
                }
            } catch (_: UserNotConfirmedException) {
                throw ExceptionWithStatusCode(HttpStatusCode.BadRequest, Code.ConfirmEmail)
            } catch (e: ResourceNotFoundException) {
                logger.catching(e)
                return@post call.respond(HttpStatusCode.InternalServerError)
            } catch (e: CognitoIdentityProviderException) {
                logger.catching(e)
                return@post call.respond(HttpStatusCode.InternalServerError)
            }
        }
    }

    route("/register") {
        post<Register> { request ->
            val result = try {
                provider.signUp {
                    username = request.email
                    password = request.password
                    clientId = cognitoClientId
                    secretHash = secretHash(cognitoClientId, request.email, cognitoClientSecret)
                }
            } catch (e: InvalidPasswordException) {
                throw ExceptionWithStatusCode(HttpStatusCode.BadRequest, Code.InvalidPassword)
            } catch (e: UsernameExistsException) {
                throw ExceptionWithStatusCode(HttpStatusCode.BadRequest, Code.EmailTaken)
            } catch (e: InvalidParameterException) {
                throw ExceptionWithStatusCode(HttpStatusCode.BadRequest, Code.InvalidPassword)
            }

            transaction {
                Users.Entity.new(UUID.fromString(result.userSub)) {
                    handle = request.handle
                    username = request.username
                }
            }

            call.respond(HttpStatusCode.Created)
        }

        route("/verify") {
            post<Verify> { request ->
                try {
                    provider.confirmSignUp {
                        confirmationCode = request.code
                        username = request.email
                        clientId = cognitoClientId
                        secretHash = secretHash(cognitoClientId, request.email, cognitoClientSecret)
                    }
                } catch (e: UserNotFoundException) {
                    throw ExceptionWithStatusCode(HttpStatusCode.BadRequest, Code.UnknownEmail)
                } catch (e: CodeMismatchException) {
                    throw ExceptionWithStatusCode(HttpStatusCode.BadRequest, Code.InvalidCode)
                } catch (e: ExpiredCodeException) {
                    throw ExceptionWithStatusCode(HttpStatusCode.BadRequest, Code.ExpiredCode)
                }

                call.respond(HttpStatusCode.OK)
            }
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
