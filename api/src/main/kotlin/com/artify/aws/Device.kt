package com.artify.aws

import org.apache.commons.codec.binary.Hex
import java.math.BigInteger
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random.Default.nextBytes


private const val HEX_N = ("FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD1"
        + "29024E088A67CC74020BBEA63B139B22514A08798E3404DD"
        + "EF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245"
        + "E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7ED"
        + "EE386BFB5A899FA5AE9F24117C4B1FE649286651ECE45B3D"
        + "C2007CB8A163BF0598DA48361C55D39A69163FA8FD24CF5F"
        + "83655D23DCA3AD961C62F356208552BB9ED529077096966D"
        + "670C354E4ABC9804F1746C08CA18217C32905E462E36CE3B"
        + "E39E772C180E86039B2783A2EC07A28FB5C55DF06F4C52C9"
        + "DE2BCBF6955817183995497CEA956AE515D2261898FA0510"
        + "15728E5A8AAAC42DAD33170D04507A33A85521ABDF1CBA64"
        + "ECFB850458DBEF0A8AEA71575D060C7DB3970F85A6E1E4C7"
        + "ABF5AE8CDB0933D71E8C94E04A25619DCEE3D2261AD2EE6B"
        + "F12FFA06D98A0864D87602733EC86A64521F2B18177B200C"
        + "BBE117577A615D6C770988C0BAD946E208E24FA074E5AB31"
        + "43DB5BFCE0FD108E4B82D120A93AD2CAFFFFFFFFFFFFFFFF")
private val SECURE_RANDOM = SecureRandom.getInstance("SHA1PRNG")
private val N = BigInteger(HEX_N, 16)
private val G = BigInteger.valueOf(2)

class DeviceSecretVerifier(
    private val poolId: String
) {
    private val k: BigInteger

    private val a: BigInteger

    private val A: BigInteger

    val salt = BigInteger(128, SECURE_RANDOM)

    private val secret = nextBytes(ByteArray(128)).toString()

    init {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(N.toByteArray())
        k = BigInteger(1, digest.digest(G.toByteArray()))

        var tempa: BigInteger
        var tempA: BigInteger

        do {
            tempa = BigInteger(1024, SECURE_RANDOM).mod(N)
            tempA = G.modPow(tempa, N)
        } while (tempA.mod(N).equals(BigInteger.ZERO))

        a = tempa
        A = tempA
    }

    fun passwordClaimSignature(
        userIdForSrp: String,
        password: String,
        B: String,
        salt: String,
        secretBlock: String,
        timestamp: String
    ): String {
        val keySpec = SecretKeySpec(
            passwordAuthenticationKey(userIdForSrp, password, BigInteger(B, 16), BigInteger(salt, 16)),
            "HmacSHA256"
        )

        val pool = if (poolId.contains('_'))
            poolId.split('_')[1]
        else
            poolId

        val mac = Mac.getInstance("HmacSHA256")
        mac.init(keySpec)
        mac.update(pool.toByteArray())
        mac.update(userIdForSrp.toByteArray())
        mac.update(Base64.getDecoder().decode(secretBlock))
        mac.update(timestamp.toByteArray())

        return Base64.getEncoder().encodeToString(mac.doFinal())
    }

    private fun passwordAuthenticationKey(
        userId: String,
        password: String,
        B: BigInteger,
        salt: BigInteger
    ): ByteArray {
        // u = H(A, B)
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(A.toByteArray())
        digest.update(B.toByteArray())

        val u = BigInteger(1, digest.digest())
        if (u == BigInteger.ZERO)
            throw SecurityException("Hash of A and B cannot be zero")

        // x = H(salt | H(poolName | userId | ":" | password))
        digest.reset()
        digest.update(poolId.split("_", limit = 2)[1].toByteArray())
        digest.update(userId.toByteArray())
        digest.update(":".toByteArray())
        digest.update(password.toByteArray())
        val userIdHash = digest.digest()

        digest.reset()
        digest.update(salt.toByteArray())

        val x = BigInteger(1, digest.digest(userIdHash))

        // S = (B - kg^x)^(a + ux)
        val S = (B.subtract(k.multiply(G.modPow(x, N))).modPow(a.add(u.multiply(x)), N)).mod(N)

        // K = H(S)
        digest.reset()
        digest.update(S.toByteArray())
        val K = digest.digest()

        val hkdf = Hkdf.getInstance("HmacSHA256")
        hkdf.init(S.toByteArray(), u.toByteArray())
        return hkdf.deriveKey("Caldera Derived Key".toByteArray(), 16)
        //return HKDF.fromHmacSha256()
        //    .extractAndExpand(S.toByteArray(), u.toByteArray(), "Caldera Derived Key".toByteArray(), 16)
    }

    fun verifier(deviceKey: String, deviceGroupKey: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt.toByteArray())
        digest.update(getUserIdHash(deviceGroupKey, deviceKey, secret))

        val x = BigInteger(1, digest.digest())
        return Base64.getEncoder().encodeToString(G.modPow(x, N).toByteArray())
    }

    fun srpa(): String = Hex.encodeHexString(A.toByteArray())

    private fun getUserIdHash(deviceGroupKey: String, deviceKey: String, secret: String): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(deviceGroupKey.toByteArray())
        digest.update(deviceKey.toByteArray())
        digest.update(":".toByteArray())
        digest.update(secret.toByteArray())

        return digest.digest()
    }
}
