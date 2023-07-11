package com.artify.api.routes.auth.register

import aws.sdk.kotlin.services.cognitoidentityprovider.CognitoIdentityProviderClient
import aws.sdk.kotlin.services.cognitoidentityprovider.adminConfirmSignUp
import aws.sdk.kotlin.services.cognitoidentityprovider.model.UserNotFoundException
import aws.smithy.kotlin.runtime.time.Instant
import aws.smithy.kotlin.runtime.time.toJvmInstant
import com.artify.api.Code
import com.artify.api.ExceptionWithStatusCode
import com.artify.api.entity.Email
import com.artify.api.routes.auth.Verify
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.transaction

fun Route.verify(
    poolId: String,
    provider: CognitoIdentityProviderClient
) {
    post<Verify> { request ->
        val result = transaction {
            Email.Table.deleteWhere {
                email.eq(request.email)
                    .and(token.eq(request.token))
                    .and(expiry.greater(Instant.now().toJvmInstant()))
            }
        }

        if (result < 1)
            return@post call.respond(HttpStatusCode.BadRequest)

        try {
            provider.adminConfirmSignUp {
                userPoolId = poolId
                username = request.email
            }
        } catch (e: UserNotFoundException) {
            throw ExceptionWithStatusCode(HttpStatusCode.BadRequest, Code.UnknownEmail)
        }

        return@post call.respond(HttpStatusCode.OK)
    }
}
