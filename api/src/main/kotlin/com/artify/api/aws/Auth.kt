package com.artify.api.aws

import aws.sdk.kotlin.services.cognitoidentityprovider.CognitoIdentityProviderClient
import aws.sdk.kotlin.services.cognitoidentityprovider.adminInitiateAuth
import aws.sdk.kotlin.services.cognitoidentityprovider.adminRespondToAuthChallenge
import aws.sdk.kotlin.services.cognitoidentityprovider.model.*
import com.artify.api.routes.auth.Device
import io.ktor.server.plugins.*
import java.text.SimpleDateFormat
import java.util.*

class Auth(
    private val poolId: String,
    private val clientId: String,
    private val clientSecret: String,
    private val deviceHelper: DeviceHelper? = null
) {
    suspend fun adminInitiateAuth(
        provider: CognitoIdentityProviderClient,
        username: String,
        password: String,
        device: Device?
    ): AuthenticationResultType {
        val parameters = mutableMapOf(
            "USERNAME" to username,
            "PASSWORD" to password,
            "SECRET_HASH" to secretHash(clientId, username, clientSecret),
        )
        if (device != null)
            parameters["DEVICE_KEY"] = device.key

        val response = provider.adminInitiateAuth {
            authFlow = AuthFlowType.AdminUserPasswordAuth
            authParameters = parameters
            userPoolId = poolId
            clientId = this@Auth.clientId
        }
        var challengeName: ChallengeNameType? = response.challengeName
        var challengeResponse: AdminRespondToAuthChallengeResponse? = null

        while (challengeName != null) {
            challengeResponse =
                provider.solveChallenge(challengeName, username, device, challengeResponse)
            challengeName = challengeResponse.challengeName
        }

        return if (challengeResponse != null)
            challengeResponse.authenticationResult!!
        else
            response.authenticationResult!!
    }

    private suspend fun CognitoIdentityProviderClient.solveChallenge(
        name: ChallengeNameType,
        username: String,
        device: Device?,
        response: AdminRespondToAuthChallengeResponse?
    ): AdminRespondToAuthChallengeResponse =
        when (name) {
            ChallengeNameType.DeviceSrpAuth -> {
                if (device == null)
                    throw BadRequestException("Device may not be null")

                solveSrpAuth(username, device, response?.session)
            }

            ChallengeNameType.DevicePasswordVerifier -> {
                if (device == null)
                    throw BadRequestException("Device may not be null")

                solveDevicePasswordVerifier(
                    username,
                    device,
                    response!!.challengeParameters?.get("SECRET_BLOCK")!!,
                    response.challengeParameters?.get("SRP_B")!!,
                    response.challengeParameters?.get("SALT")!!
                )
            }

            else -> TODO()
        }

    private suspend fun CognitoIdentityProviderClient.adminSolve(
        challenge: ChallengeNameType,
        responses: Map<String, String>,
        session: String? = null
    ): AdminRespondToAuthChallengeResponse {
        return adminRespondToAuthChallenge {
            challengeName = challenge
            challengeResponses = responses
            this.session = session
            userPoolId = poolId
            clientId = this@Auth.clientId
        }
    }

    private suspend fun CognitoIdentityProviderClient.solveSrpAuth(
        username: String,
        device: Device,
        session: String? = null
    ): AdminRespondToAuthChallengeResponse =
        adminSolve(
            ChallengeNameType.DeviceSrpAuth,
            mapOf(
                "USERNAME" to username,
                "DEVICE_KEY" to device.key,
                "SRP_A" to deviceHelper!!.srpA(),
                "SECRET_HASH" to secretHash(clientId, username, clientSecret)
            ),
            session
        )

    private suspend fun CognitoIdentityProviderClient.solveDevicePasswordVerifier(
        username: String,
        device: Device,
        secretBlock: String,
        srpB: String,
        salt: String
    ): AdminRespondToAuthChallengeResponse {
        val date = SimpleDateFormat("EEE MMM d HH:mm:ss z yyyy", Locale.US)
        date.timeZone = SimpleTimeZone(SimpleTimeZone.UTC_TIME, "UTC")
        val timestamp = date.format(Date())

        return adminSolve(
            ChallengeNameType.DevicePasswordVerifier,
            mapOf(
                "USERNAME" to username,
                "PASSWORD_CLAIM_SECRET_BLOCK" to secretBlock,
                "TIMESTAMP" to timestamp,
                "PASSWORD_CLAIM_SIGNATURE" to deviceHelper!!.passwordClaimSignature(
                    device.password,
                    srpB,
                    salt,
                    timestamp,
                    secretBlock
                ),
                "DEVICE_KEY" to device.key,
                "SECRET_HASH" to secretHash(
                    clientId,
                    username,
                    clientSecret
                )
            )
        )
    }
}
