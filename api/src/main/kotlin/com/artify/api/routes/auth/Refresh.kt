package com.artify.api.routes.auth

import aws.sdk.kotlin.services.cognitoidentityprovider.CognitoIdentityProviderClient
import aws.sdk.kotlin.services.cognitoidentityprovider.adminInitiateAuth
import aws.sdk.kotlin.services.cognitoidentityprovider.model.AuthFlowType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.CognitoIdentityProviderException
import aws.sdk.kotlin.services.cognitoidentityprovider.model.NotAuthorizedException
import aws.sdk.kotlin.services.cognitoidentityprovider.model.UserNotFoundException
import com.artify.api.aws.secretHash
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.refresh(
    cognitoClientId: String,
    cognitoClientSecret: String,
    cognitoPoolId: String,
    provider: CognitoIdentityProviderClient
) {
    val logger = KotlinLogging.logger { }

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
        } catch (e: UserNotFoundException) {
            return@post call.respond(HttpStatusCode.Unauthorized)
        } catch (e: CognitoIdentityProviderException) {
            logger.catching(e)
            return@post call.respond(HttpStatusCode.InternalServerError)
        }

        if (result.challengeName != null || result.authenticationResult == null)
            return@post call.respond(HttpStatusCode.Unauthorized)

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
    }
}
