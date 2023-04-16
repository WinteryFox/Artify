package com.artify.aws

import com.amazonaws.services.cognitoidp.model.AdminRespondToAuthChallengeRequest

fun AdminRespondToAuthChallengeRequest.respondToChallenge(name: String, parameters: Map<String, String>): AdminRespondToAuthChallengeRequest {
    when (name) {
        "DEVICE_SRP_AUTH" -> solveDeviceSrp()
        "DEVICE_PASSWORD_VERIFIER" -> solveDevicePasswordVerifier()
        else -> throw NotImplementedError("Challenge $name is not implemented")
    }

    return this
}

fun AdminRespondToAuthChallengeRequest.solveDeviceSrp() {
    withChallengeName("DEVICE_SRP_AUTH")

}

fun AdminRespondToAuthChallengeRequest.solveDevicePasswordVerifier() {
    withChallengeName("DEVICE_PASSWORD_VERIFIER")
}
