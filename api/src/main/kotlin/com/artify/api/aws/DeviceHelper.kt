package com.artify.api.aws

import java.lang.RuntimeException
import java.math.BigInteger
import java.nio.charset.Charset
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.*

private const val HEX_N =
    "FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD129024E088A67CC74020BBEA63B139B22514A08798E3404DDEF9519B3CD3A" +
            "431B302B0A6DF25F14374FE1356D6D51C245E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7EDEE386BFB5A899FA5" +
            "AE9F24117C4B1FE649286651ECE45B3DC2007CB8A163BF0598DA48361C55D39A69163FA8FD24CF5F83655D23DCA3AD961C62" +
            "F356208552BB9ED529077096966D670C354E4ABC9804F1746C08CA18217C32905E462E36CE3BE39E772C180E86039B2783A2" +
            "EC07A28FB5C55DF06F4C52C9DE2BCBF6955817183995497CEA956AE515D2261898FA051015728E5A8AAAC42DAD33170D0450" +
            "7A33A85521ABDF1CBA64ECFB850458DBEF0A8AEA71575D060C7DB3970F85A6E1E4C7ABF5AE8CDB0933D71E8C94E04A25619D" +
            "CEE3D2261AD2EE6BF12FFA06D98A0864D87602733EC86A64521F2B18177B200CBBE117577A615D6C770988C0BAD946E208E2" +
            "4FA074E5AB3143DB5BFCE0FD108E4B82D120A93AD2CAFFFFFFFFFFFFFFFF"
private const val HEX_G = "2"
private val SECURE_RANDOM = SecureRandom.getInstance("SHA1PRNG")
private const val DERIVED_KEY_INFO = "Caldera Derived Key"
private const val DERIVED_KEY_SIZE = 16

class DeviceHelper(
    private val deviceKey: String,
    private val deviceGroupKey: String
) {
    private val bigN: BigInteger = BigInteger(HEX_N, 16)

    private val g: BigInteger = BigInteger(HEX_G, 16)

    private val k: BigInteger

    private val a: BigInteger

    private val bigA: BigInteger

    init {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(bigN.toByteArray())

        var tempa: BigInteger
        var tempA: BigInteger

        do {
            tempa = BigInteger(1024, SECURE_RANDOM).mod(bigN)
            tempA = g.modPow(tempa, bigN)
        } while (tempA.mod(bigN) == BigInteger.ZERO)

        a = tempa
        bigA = tempA

        k = BigInteger(1, hash(bigN.toByteArray(), g.toByteArray()))
    }

    fun passwordClaimSignature(
        devicePassword: String,
        srpB: String,
        srpSalt: String,
        timestamp: String,
        secretBlock: String
    ): String {
        val fullPassword = hash(deviceGroupKey, deviceKey, ":", devicePassword)

        val bigB = BigInteger(srpB, 16)
        val salt = BigInteger(srpSalt, 16)

        if (bigB.mod(bigN) == BigInteger.ZERO)
            throw RuntimeException("Bad server B")

        val u = BigInteger(1, hash(bigA.toByteArray(), bigB.toByteArray()))
        if (u.mod(bigN) == BigInteger.ZERO)
            throw RuntimeException("Hash of A and B cannot be zero")

        val x = BigInteger(1, hash(salt.toByteArray(), fullPassword))

        val bigS = (bigB.subtract(k.multiply(g.modPow(x, bigN))).modPow(a.add(u.multiply(x)), bigN)).mod(bigN)
        val prk = hmac(u.toByteArray(), bigS.toByteArray())
        val hkdf = hmac(prk, DERIVED_KEY_INFO, Char(1).toString()).copyOf(DERIVED_KEY_SIZE)

        val signature = hmac(
            hkdf,
            deviceGroupKey.toByteArray(),
            deviceKey.toByteArray(),
            Base64.getDecoder().decode(secretBlock),
            timestamp.toByteArray()
        )

        return Base64.getEncoder().encodeToString(signature)
    }

    fun srpA(): String = HexFormat.of().formatHex(bigA.toByteArray())

    fun passwordVerifierConfig(): PasswordVerifier {
        var randomPassword = ByteArray(40)
        SECURE_RANDOM.nextBytes(randomPassword)
        randomPassword = Base64.getEncoder().encode(randomPassword)

        val fullPassword =
            hash(deviceGroupKey.toByteArray(), deviceKey.toByteArray(), ":".toByteArray(), randomPassword)

        val salt = ByteArray(16)
        SECURE_RANDOM.nextBytes(salt)

        val x = BigInteger(1, hash(salt, fullPassword))
        val verifier = g.modPow(x, bigN)

        return PasswordVerifier(
            randomPassword.toString(Charset.forName("UTF-8")),
            Base64.getEncoder().encodeToString(verifier.toByteArray()),
            Base64.getEncoder().encodeToString(BigInteger(1, salt).toByteArray())
        )
    }
}

data class PasswordVerifier(
    val devicePassword: String,
    val passwordVerifier: String,
    val salt: String
)
