package com.artify.api.route.auth

import aws.sdk.kotlin.services.cognitoidentityprovider.CognitoIdentityProviderClient
import aws.sdk.kotlin.services.cognitoidentityprovider.confirmDevice
import aws.sdk.kotlin.services.cognitoidentityprovider.model.*
import com.artify.api.Code
import com.artify.api.ExceptionWithStatusCode
import com.artify.api.aws.Auth
import com.artify.api.aws.DeviceHelper
import com.artify.api.route.Device
import com.artify.api.route.Jwt
import com.artify.api.route.Login
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.login(
    cognitoClientId: String,
    cognitoClientSecret: String,
    cognitoPoolId: String,
    provider: CognitoIdentityProviderClient
) {
    val logger = KotlinLogging.logger { }

    post<Login> { request ->
        try {
            val authHelper = Auth(
                cognitoPoolId,
                cognitoClientId,
                cognitoClientSecret,
                request.device?.let {
                    DeviceHelper(request.device.key, request.device.groupKey)
                }
            )

            val authenticationResult = authHelper.adminInitiateAuth(
                provider,
                request.username,
                request.password,
                request.device
            )

            val deviceConfig = request.device?.let {
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
            }

            call.respond(
                HttpStatusCode.OK, Jwt(
                    authenticationResult.expiresIn,
                    authenticationResult.tokenType!!,
                    authenticationResult.idToken!!,
                    authenticationResult.accessToken!!,
                    authenticationResult.refreshToken,
                    deviceConfig?.let {
                        Device(
                            authenticationResult.newDeviceMetadata!!.deviceKey!!,
                            authenticationResult.newDeviceMetadata!!.deviceGroupKey!!,
                            deviceConfig.devicePassword
                        )
                    }
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
