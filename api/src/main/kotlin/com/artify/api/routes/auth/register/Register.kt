package com.artify.api.routes.auth.register

import aws.sdk.kotlin.services.cognitoidentityprovider.CognitoIdentityProviderClient
import aws.sdk.kotlin.services.cognitoidentityprovider.model.InvalidParameterException
import aws.sdk.kotlin.services.cognitoidentityprovider.model.InvalidPasswordException
import aws.sdk.kotlin.services.cognitoidentityprovider.model.UsernameExistsException
import aws.sdk.kotlin.services.cognitoidentityprovider.signUp
import aws.smithy.kotlin.runtime.time.Instant
import aws.smithy.kotlin.runtime.time.toJvmInstant
import com.artify.api.Code
import com.artify.api.ExceptionWithStatusCode
import com.artify.api.aws.secretHash
import com.artify.api.entity.Email
import com.artify.api.entity.Users
import com.artify.api.routes.auth.Register
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.security.SecureRandom
import java.util.*
import kotlin.time.Duration.Companion.days

fun Route.register(
    cognitoClientId: String,
    cognitoClientSecret: String,
    provider: CognitoIdentityProviderClient
) {
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
                email = request.email
                handle = request.handle
                username = request.username
                //discriminator = request.discriminator
                //displayName = request.username
            }
        }

        call.respond(HttpStatusCode.Created)

        val bytes = ByteArray(128)
        SecureRandom.getInstanceStrong().nextBytes(bytes)
        transaction {
            Email.Table.insert {
                it[email] = request.email
                it[token] = HexFormat.of().formatHex(bytes)
                it[expiry] = Instant.now().plus(7.days).toJvmInstant()
            }
        }
    }
}
