package com.artify.aws

import java.security.MessageDigest
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

fun hash(vararg input: String, algorithm: String = "SHA-256"): ByteArray =
    hash(input.map { it.toByteArray() }, algorithm)

fun hash(vararg input: ByteArray, algorithm: String = "SHA-256"): ByteArray =
    hash(input.toList(), algorithm)

fun hash(input: List<ByteArray>, algorithm: String = "SHA-256"): ByteArray {
    val digest = MessageDigest.getInstance(algorithm)
    for (i in input)
        digest.update(i)

    return digest.digest()
}

fun hmac(key: ByteArray, vararg input: String, algorithm: String = "HmacSHA256"): ByteArray =
    hmac(key, input.map { it.toByteArray() }, algorithm)

fun hmac(key: ByteArray, vararg input: ByteArray, algorithm: String = "HmacSHA256"): ByteArray =
    hmac(key, input.toList(), algorithm)

fun hmac(key: ByteArray, input: List<ByteArray>, algorithm: String = "HmacSHA256"): ByteArray {
    val keySpec = SecretKeySpec(key, algorithm)
    val mac = Mac.getInstance(algorithm)
    mac.init(keySpec)

    for (i in input)
        mac.update(i)

    return mac.doFinal()
}

fun secretHash(cognitoClientId: String, email: String, secret: String): String =
    Base64.getEncoder().encodeToString(hmac(secret.toByteArray(), email, cognitoClientId))
