package com.artify.api.aws

import java.security.MessageDigest
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

private const val hashingAlgorithm: String = "SHA-256"
private const val hmacAlgorithm: String = "HmacSHA256"

fun hash(vararg input: String, algorithm: String = hashingAlgorithm): ByteArray =
    hash(input.map { it.toByteArray() }, algorithm)

fun hash(vararg input: ByteArray, algorithm: String = hashingAlgorithm): ByteArray =
    hash(input.toList(), algorithm)

fun hash(input: List<ByteArray>, algorithm: String = hashingAlgorithm): ByteArray {
    val digest = MessageDigest.getInstance(algorithm)
    for (i in input)
        digest.update(i)

    return digest.digest()
}

fun hmac(key: ByteArray, vararg input: String, algorithm: String = hmacAlgorithm): ByteArray =
    hmac(key, input.map { it.toByteArray() }, algorithm)

fun hmac(key: ByteArray, vararg input: ByteArray, algorithm: String = hmacAlgorithm): ByteArray =
    hmac(key, input.toList(), algorithm)

fun hmac(key: ByteArray, input: List<ByteArray>, algorithm: String = hmacAlgorithm): ByteArray {
    val keySpec = SecretKeySpec(key, algorithm)
    val mac = Mac.getInstance(algorithm)
    mac.init(keySpec)

    for (i in input)
        mac.update(i)

    return mac.doFinal()
}

fun secretHash(cognitoClientId: String, email: String?, secret: String): String {
    val signingKey = SecretKeySpec(secret.toByteArray(), "HmacSHA256")
    val mac = Mac.getInstance("HmacSHA256")
    mac.init(signingKey)
    if (email != null)
        mac.update(email.toByteArray())

    return Base64.getEncoder().encodeToString(mac.doFinal(cognitoClientId.toByteArray()))
}
