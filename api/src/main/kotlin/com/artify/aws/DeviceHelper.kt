package com.artify.aws

import org.apache.commons.codec.binary.Hex
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
    private val N: BigInteger = BigInteger(HEX_N, 16)

    private val g: BigInteger = BigInteger(HEX_G, 16)

    private val k: BigInteger

    private val a: BigInteger

    private val A: BigInteger

    init {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(N.toByteArray())

        var tempa: BigInteger
        var tempA: BigInteger

        do {
            tempa = BigInteger(1024, SECURE_RANDOM).mod(N)
            tempA = g.modPow(tempa, N)
        } while (tempA.mod(N) == BigInteger.ZERO)

        a = tempa
        A = tempA

        // k = SHA256_HASH(N + g)
        k = BigInteger(1, hash(N.toByteArray(), g.toByteArray()))
    }

    fun passwordClaimSignature(
        devicePassword: String,
        srpB: String,
        srpSalt: String,
        timestamp: String,
        secretBlock: String
    ): String {
        // FULL_PASSWORD = SHA256_HASH(DeviceGroupKey + DeviceKey + ":" + DevicePassword)
        val fullPassword = hash(deviceGroupKey, deviceKey, ":", devicePassword)

        val B = BigInteger(srpB, 16)
        val salt = BigInteger(srpSalt, 16)

        if (B.mod(N) == BigInteger.ZERO)
            throw RuntimeException("Bad server B")

        // u = SHA256_HASH(SRP_A + SRP_B)
        //val u = BigInteger(1, hash(A.add(B).toByteArray()))
        val u = BigInteger(1, hash(A.toByteArray(), B.toByteArray()))
        if (u.mod(N) == BigInteger.ZERO)
            throw RuntimeException("Hash of A and B cannot be zero")

        // x = SHA256_HASH(salt + FULL_PASSWORD)
        val x = BigInteger(1, hash(salt.toByteArray(), fullPassword))

        // S_USER = [ ( SRP_B - [ k * [ (gx) (mod N) ] ] )(a + ux) ](mod N)
        val S = (B.subtract(k.multiply(g.modPow(x, N))).modPow(a.add(u.multiply(x)), N)).mod(N)
        // K_USER = SHA256_HASH(S_USER)
        val prk = hmac(u.toByteArray(), S.toByteArray())
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

    fun srpA(): String = Hex.encodeHexString(A.toByteArray())

    fun passwordVerifierConfig(): PasswordVerifier {
        // RANDOM_PASSWORD = 40 random bytes, base64-encoded
        var randomPassword = ByteArray(40)
        SECURE_RANDOM.nextBytes(randomPassword)
        randomPassword = Base64.getEncoder().encode(randomPassword)

        // FULL_PASSWORD = SHA256_HASH(DeviceGroupKey + DeviceKey + ":" + RANDOM_PASSWORD)
        val fullPassword =
            hash(deviceGroupKey.toByteArray(), deviceKey.toByteArray(), ":".toByteArray(), randomPassword)

        val salt = ByteArray(16)
        SECURE_RANDOM.nextBytes(salt)

        val x = BigInteger(1, hash(salt, fullPassword))
        val verifier = g.modPow(x, N)

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
