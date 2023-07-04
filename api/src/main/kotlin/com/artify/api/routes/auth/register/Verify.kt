package com.artify.api.routes.auth.register

import aws.sdk.kotlin.services.cognitoidentityprovider.CognitoIdentityProviderClient
import aws.sdk.kotlin.services.cognitoidentityprovider.confirmSignUp
import aws.sdk.kotlin.services.cognitoidentityprovider.model.CodeMismatchException
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ExpiredCodeException
import aws.sdk.kotlin.services.cognitoidentityprovider.model.UserNotFoundException
import com.artify.api.Code
import com.artify.api.ExceptionWithStatusCode
import com.artify.api.aws.secretHash
import com.artify.api.routes.auth.Verify
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.verify(
    cognitoClientId: String,
    cognitoClientSecret: String,
    provider: CognitoIdentityProviderClient
) {
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
