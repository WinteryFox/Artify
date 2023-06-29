package com.artify.api.route

import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider
import com.amazonaws.services.cognitoidp.model.*
import com.artify.api.Code
import com.artify.api.ExceptionWithStatusCode
import com.artify.api.aws.DeviceHelper
import com.artify.api.aws.secretHash
import com.artify.api.entity.Users
import com.auth0.jwt.exceptions.TokenExpiredException
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
import java.text.SimpleDateFormat
import java.util.*

@Serializable
data class Login(
    val email: String,
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

fun Route.authRoute(provider: AWSCognitoIdentityProvider) {
    val logger = KotlinLogging.logger { }
    val cognitoPoolId = application.environment.config.property("aws.cognito.pool").getString()
    val cognitoClientId = application.environment.config.property("aws.cognito.client.id").getString()
    val cognitoClientSecret = application.environment.config.property("aws.cognito.client.secret").getString()

    route("/refresh") {
        post<Refresh> { request ->
            val result = try {
                provider.adminInitiateAuth(
                    AdminInitiateAuthRequest()
                        .withAuthFlow(AuthFlowType.REFRESH_TOKEN_AUTH)
                        .withAuthParameters(
                            mapOf(
                                "REFRESH_TOKEN" to request.refreshToken,
                                "DEVICE_KEY" to request.deviceKey,
                                "SECRET_HASH" to secretHash(cognitoClientId, request.id, cognitoClientSecret)
                            )
                        )
                        .withUserPoolId(cognitoPoolId)
                        .withClientId(cognitoClientId)
                )
            } catch (e: NotAuthorizedException) {
                return@post call.respond(HttpStatusCode.Unauthorized)
            } catch (e: TokenExpiredException) {
                return@post call.respond(HttpStatusCode.Unauthorized)
            } catch (e: UserNotFoundException) {
                return@post call.respond(HttpStatusCode.Unauthorized)
            } catch (e: AWSCognitoIdentityProviderException) {
                logger.catching(e)
                return@post call.respond(HttpStatusCode.InternalServerError)
            }

            if (result.challengeName == null)
                call.respond(
                    HttpStatusCode.OK, Jwt(
                        result.authenticationResult.expiresIn,
                        result.authenticationResult.tokenType,
                        result.authenticationResult.idToken,
                        result.authenticationResult.accessToken,
                        result.authenticationResult.refreshToken,
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
                if (request.device != null) {
                    val helper = DeviceHelper(request.device.key, request.device.groupKey)

                    val auth = provider.adminInitiateAuth(
                        AdminInitiateAuthRequest()
                            .withAuthFlow(AuthFlowType.ADMIN_USER_PASSWORD_AUTH)
                            .withAuthParameters(
                                mapOf(
                                    "USERNAME" to request.email,
                                    "PASSWORD" to request.password,
                                    "DEVICE_KEY" to request.device.key,
                                    "SECRET_HASH" to secretHash(cognitoClientId, request.email, cognitoClientSecret),
                                )
                            )
                            .withUserPoolId(cognitoPoolId)
                            .withClientId(cognitoClientId)
                    )

                    val date = SimpleDateFormat("EEE MMM d HH:mm:ss z yyyy", Locale.US)
                    date.timeZone = SimpleTimeZone(SimpleTimeZone.UTC_TIME, "UTC")
                    val timestamp = date.format(Date())

                    val deviceSrpAuth = provider.adminRespondToAuthChallenge(
                        AdminRespondToAuthChallengeRequest()
                            .withChallengeName("DEVICE_SRP_AUTH")
                            .withChallengeResponses(
                                mapOf(
                                    "USERNAME" to request.email,
                                    "DEVICE_KEY" to request.device.key,
                                    "SRP_A" to helper.srpA(),
                                    "SECRET_HASH" to secretHash(cognitoClientId, request.email, cognitoClientSecret)
                                )
                            )
                            .withSession(auth.session)
                            .withClientId(cognitoClientId)
                            .withUserPoolId(cognitoPoolId)
                    )

                    val devicePasswordVerifier = provider.adminRespondToAuthChallenge(
                        AdminRespondToAuthChallengeRequest()
                            .withChallengeName("DEVICE_PASSWORD_VERIFIER")
                            .withChallengeResponses(
                                mapOf(
                                    "USERNAME" to deviceSrpAuth.challengeParameters["USERNAME"]!!,
                                    "PASSWORD_CLAIM_SECRET_BLOCK" to deviceSrpAuth.challengeParameters["SECRET_BLOCK"]!!,
                                    "TIMESTAMP" to timestamp,
                                    "PASSWORD_CLAIM_SIGNATURE" to helper.passwordClaimSignature(
                                        request.device.password,
                                        deviceSrpAuth.challengeParameters["SRP_B"]!!,
                                        deviceSrpAuth.challengeParameters["SALT"]!!,
                                        timestamp,
                                        deviceSrpAuth.challengeParameters["SECRET_BLOCK"]!!
                                    ),
                                    "DEVICE_KEY" to request.device.key,
                                    "SECRET_HASH" to secretHash(
                                        cognitoClientId,
                                        deviceSrpAuth.challengeParameters["USERNAME"]!!,
                                        cognitoClientSecret
                                    )
                                )
                            )
                            .withSession(deviceSrpAuth.session)
                            .withClientId(cognitoClientId)
                            .withUserPoolId(cognitoPoolId)
                    ).authenticationResult

                    call.respond(
                        HttpStatusCode.OK, Jwt(
                            devicePasswordVerifier.expiresIn,
                            devicePasswordVerifier.tokenType,
                            devicePasswordVerifier.idToken,
                            devicePasswordVerifier.accessToken,
                            devicePasswordVerifier.refreshToken,
                            null
                        )
                    )
                } else {
                    val auth = provider.adminInitiateAuth(
                        AdminInitiateAuthRequest()
                            .withAuthFlow(AuthFlowType.ADMIN_USER_PASSWORD_AUTH)
                            .withAuthParameters(
                                mapOf(
                                    "USERNAME" to request.email,
                                    "PASSWORD" to request.password,
                                    "SECRET_HASH" to secretHash(cognitoClientId, request.email, cognitoClientSecret)
                                )
                            )
                            .withClientId(cognitoClientId)
                            .withUserPoolId(cognitoPoolId)
                    ).authenticationResult

                    val helper = DeviceHelper(auth.newDeviceMetadata.deviceKey, auth.newDeviceMetadata.deviceGroupKey)
                    val config = helper.passwordVerifierConfig()

                    provider.confirmDevice(
                        ConfirmDeviceRequest()
                            .withAccessToken(auth.accessToken)
                            .withDeviceKey(auth.newDeviceMetadata.deviceKey)
                            .withDeviceName(call.request.userAgent())
                            .withDeviceSecretVerifierConfig(
                                DeviceSecretVerifierConfigType()
                                    .withSalt(config.salt)
                                    .withPasswordVerifier(config.passwordVerifier)
                            )
                    )

                    call.respond(
                        HttpStatusCode.OK, Jwt(
                            auth.expiresIn,
                            auth.tokenType,
                            auth.idToken,
                            auth.accessToken,
                            auth.refreshToken,
                            Device(
                                auth.newDeviceMetadata.deviceKey,
                                auth.newDeviceMetadata.deviceGroupKey,
                                config.devicePassword
                            )
                        )
                    )
                }
            } catch (_: InvalidParameterException) {
                throw ExceptionWithStatusCode(HttpStatusCode.BadRequest, Code.BadCredentials)
            } catch (e: NotAuthorizedException) {
                when (e.errorMessage) {
                    "User is disabled." -> throw ExceptionWithStatusCode(HttpStatusCode.BadRequest, Code.AccountDisabled)
                    else -> throw ExceptionWithStatusCode(HttpStatusCode.BadRequest, Code.BadCredentials)
                }
            } catch (_: UserNotConfirmedException) {
                throw ExceptionWithStatusCode(HttpStatusCode.BadRequest, Code.ConfirmEmail)
            } catch (e: ResourceNotFoundException) {
                logger.catching(e)
                return@post call.respond(HttpStatusCode.InternalServerError)
            } catch (e: AWSCognitoIdentityProviderException) {
                logger.catching(e)
                return@post call.respond(HttpStatusCode.InternalServerError)
            }
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
                    provider.confirmSignUp(
                        ConfirmSignUpRequest()
                            .withConfirmationCode(request.code)
                            .withUsername(request.email)
                            .withClientId(cognitoClientId)
                            .withSecretHash(secretHash(cognitoClientId, request.email, cognitoClientSecret))
                    )
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