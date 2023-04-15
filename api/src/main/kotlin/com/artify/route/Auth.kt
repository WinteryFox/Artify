package com.artify.route

import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider
import com.amazonaws.services.cognitoidp.model.*
import com.artify.aws.DeviceSecretVerifier
import com.artify.entity.Users
import com.auth0.jwt.exceptions.TokenExpiredException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Serializable
data class Login(
    val email: String,
    val password: String,
    @SerialName("device_key")
    val deviceKey: String? = null,
    @SerialName("device_group_key")
    val deviceGroupKey: String? = null,
    @SerialName("remember")
    val remember: Boolean? = null
)

@Serializable
data class Refresh(
    val id: String,
    @SerialName("refresh_token")
    val refreshToken: String
)

@Serializable
data class Jwt(
    @SerialName("device_key")
    val deviceKey: String,
    @SerialName("device_group_key")
    val deviceGroupKey: String,
    @SerialName("expires_in")
    val expiresIn: Int,
    val type: String,
    @SerialName("id_token")
    val idToken: String,
    @SerialName("access_token")
    val accessToken: String,
    @SerialName("refresh_token")
    val refreshToken: String? = null,
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

private fun secretHash(cognitoClientId: String, email: String?, secret: String): String {
    val signingKey = SecretKeySpec(secret.toByteArray(), "HmacSHA256")
    val mac = Mac.getInstance("HmacSHA256")
    mac.init(signingKey)
    if (email != null)
        mac.update(email.toByteArray())

    return Base64.getEncoder().encodeToString(mac.doFinal(cognitoClientId.toByteArray()))
}

fun Route.authRoute(provider: AWSCognitoIdentityProvider) {
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
                                "SECRET_HASH" to secretHash(cognitoClientId, request.id, cognitoClientSecret),
                                "DEVICE_KEY" to null // TODO
                            )
                        )
                        .withUserPoolId(cognitoPoolId)
                        .withClientId(cognitoClientId)
                )
            } catch (e: NotAuthorizedException) {
                throw e
                throw BadRequestException("Invalid refresh token")
            } catch (e: TokenExpiredException) {
                throw BadRequestException("Refresh token expired")
            }

            if (result.challengeName == null)
                call.respond(
                    HttpStatusCode.OK, Jwt(
                        result.authenticationResult.newDeviceMetadata.deviceKey,
                        result.authenticationResult.newDeviceMetadata.deviceGroupKey,
                        result.authenticationResult.expiresIn,
                        result.authenticationResult.tokenType,
                        result.authenticationResult.idToken,
                        result.authenticationResult.accessToken,
                        null
                    )
                )
            else
                call.respond(HttpStatusCode.Unauthorized) // TODO: Handle additional challenges
        }
    }

    route("/login") {
        post<Login> { request ->
            val verifier = DeviceSecretVerifier(cognitoPoolId)

            val auth = try {
                if (request.deviceKey != null) {
                    val auth = provider.adminInitiateAuth(
                        AdminInitiateAuthRequest()
                            .withAuthFlow(AuthFlowType.USER_SRP_AUTH)
                            .withAuthParameters(
                                mapOf(
                                    "USERNAME" to request.email,
                                    "SRP_A" to verifier.srpa(),
                                    "SECRET_HASH" to secretHash(cognitoClientId, request.email, cognitoClientSecret)
                                )
                            )
                            .withUserPoolId(cognitoPoolId)
                            .withClientId(cognitoClientId)
                    )

                    /*val date = SimpleDateFormat("EEE MMM d HH:mm:ss z yyyy", Locale.US)
                    date.timeZone = SimpleTimeZone(SimpleTimeZone.UTC_TIME, "UTC")
                    val timestamp = date.format(Date())*/

                    provider.respondToAuthChallenge(
                        verifier.userSrpAuthRequest(
                            auth,
                            cognitoPoolId,
                            cognitoClientId,
                            request.password,
                            secretHash(cognitoClientId, auth.challengeParameters["USERNAME"], cognitoClientSecret)
                        )
                        /*RespondToAuthChallengeRequest()
                            .withChallengeName(auth.challengeName)
                            .withChallengeResponses(
                                mapOf(
                                    "PASSWORD_CLAIM_SIGNATURE" to verifier.passwordClaimSignature(
                                        auth.challengeParameters["USER_ID_FOR_SRP"]!!,
                                        request.password,
                                        auth.challengeParameters["SRP_B"]!!,
                                        auth.challengeParameters["SALT"]!!,
                                        auth.challengeParameters["SECRET_BLOCK"]!!,
                                        timestamp
                                    ),
                                    "PASSWORD_CLAIM_SECRET_BLOCK" to auth.challengeParameters["SECRET_BLOCK"],
                                    "TIMESTAMP" to timestamp,
                                    "USERNAME" to auth.challengeParameters["USERNAME"],
                                    "SECRET_HASH" to secretHash(cognitoClientId, auth.challengeParameters["USERNAME"]!!, cognitoClientSecret)
                                )
                            )
                            .withClientId(cognitoClientId)*/
                    ).authenticationResult
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

                    if (request.remember == true)
                        provider.confirmDevice(
                            ConfirmDeviceRequest()
                                .withAccessToken(auth.accessToken)
                                .withDeviceKey(auth.newDeviceMetadata.deviceKey)
                                .withDeviceName(call.request.userAgent())
                                .withDeviceSecretVerifierConfig(
                                    DeviceSecretVerifierConfigType()
                                        .withSalt(Base64.getEncoder().encodeToString(verifier.salt.toByteArray()))
                                        .withPasswordVerifier(
                                            verifier.verifier(
                                                auth.newDeviceMetadata.deviceKey,
                                                auth.newDeviceMetadata.deviceGroupKey
                                            )
                                        )
                                )
                        )

                    auth
                }
            } catch (e: NotAuthorizedException) {
                throw e
                call.respond(HttpStatusCode.Unauthorized)
                return@post
            }

            call.respond(
                HttpStatusCode.OK, Jwt(
                    request.deviceKey ?: auth.newDeviceMetadata.deviceKey,
                    request.deviceGroupKey ?: auth.newDeviceMetadata.deviceGroupKey,
                    auth.expiresIn,
                    auth.tokenType,
                    auth.idToken,
                    auth.accessToken,
                    auth.refreshToken
                )
            )
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

fun PipelineContext<*, ApplicationCall>.getUser(): Users.Entity? {
    val principal = call.principal<JWTPrincipal>() ?: return null

    return transaction {
        Users.Entity.findById(UUID.fromString(principal.subject))
    }
}
