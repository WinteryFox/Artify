package com.artify

import io.ktor.http.*
import kotlinx.serialization.Serializable

@Serializable
open class Code(
    @Suppress("unused")
    val code: Int,
    val message: String
) {
    object ConfirmEmail : Code(100, "Please confirm your email first")

    object BadCredentials : Code(101, "Email or password is incorrect")

    object InvalidPassword : Code(102, "Invalid password")

    object EmailTaken : Code(103, "Email is taken")

    object UnknownEmail : Code(104, "Email is not registered")

    object InvalidCode : Code(105, "Code is invalid")

    object ExpiredCode : Code(106, "Code is expired")

    object AccountDisabled : Code(107, "Account is disabled")
}

class ExceptionWithStatusCode(
    val status: HttpStatusCode,
    val code: Code,
    cause: Throwable? = null
) : RuntimeException(code.message, cause)
