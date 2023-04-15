package com.artify.aws

import io.ktor.utils.io.core.*
import java.security.GeneralSecurityException
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.util.*
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.ShortBufferException
import javax.crypto.spec.SecretKeySpec
import kotlin.text.toByteArray

class Hkdf(algorithm: String) {
    private val EMPTY_ARRAY = ByteArray(0)
    private var algorithm: String? = null
    private var prk: SecretKey? = null

    /**
     * @param algorithm REQUIRED: The type of HMAC algorithm to be used.
     */
    init {
        if (!algorithm.startsWith("Hmac")) {
            throw IllegalArgumentException(
                "Invalid algorithm " + algorithm
                        + ". Hkdf may only be used with Hmac algorithms."
            )
        } else {
            this.algorithm = algorithm
        }
    }

    /**
     * @param ikm REQUIRED: The input key material.
     */
    fun init(ikm: ByteArray) {
        this.init(ikm, null as ByteArray?)
    }

    /**
     * @param ikm  REQUIRED: The input key material.
     * @param salt REQUIRED: Random bytes for salt.
     */
    fun init(ikm: ByteArray, salt: ByteArray?) {
        var realSalt = if (salt == null) EMPTY_ARRAY else salt.clone()
        var rawKeyMaterial: ByteArray = EMPTY_ARRAY
        try {
            val e = Mac.getInstance(algorithm)
            if (realSalt.size == 0) {
                realSalt = ByteArray(e.macLength)
                Arrays.fill(realSalt, 0.toByte())
            }
            e.init(SecretKeySpec(realSalt, algorithm))
            rawKeyMaterial = e.doFinal(ikm)
            val key = SecretKeySpec(rawKeyMaterial, algorithm)
            Arrays.fill(rawKeyMaterial, 0.toByte())
            unsafeInitWithoutKeyExtraction(key)
        } catch (var10: GeneralSecurityException) {
            throw RuntimeException("Unexpected exception", var10)
        } finally {
            Arrays.fill(rawKeyMaterial, 0.toByte())
        }
    }

    /**
     * @param rawKey REQUIRED: Current secret key.
     * @throws InvalidKeyException
     */
    @Throws(InvalidKeyException::class)
    private fun unsafeInitWithoutKeyExtraction(rawKey: SecretKey) {
        if (rawKey.algorithm != algorithm) {
            throw InvalidKeyException(
                ("Algorithm for the provided key must match the algorithm for this Hkdf. Expected "
                        + algorithm + " but found " + rawKey.algorithm)
            )
        } else {
            prk = rawKey
        }
    }

    /**
     * @param info   REQUIRED
     * @param length REQUIRED
     * @return converted bytes.
     */
    private fun deriveKey(info: String, length: Int): ByteArray {
        return this.deriveKey(info.toByteArray(), length)
    }

    /**
     * @param info   REQUIRED
     * @param length REQUIRED
     * @return converted bytes.
     */
    fun deriveKey(info: ByteArray, length: Int): ByteArray {
        val result = ByteArray(length)
        try {
            this.deriveKey(info, length, result, 0)
            return result
        } catch (var5: ShortBufferException) {
            throw RuntimeException(var5)
        }
    }

    /**
     * @param info   REQUIRED
     * @param length REQUIRED
     * @param output REQUIRED
     * @param offset REQUIRED
     * @throws ShortBufferException
     */
    @Throws(ShortBufferException::class)
    private fun deriveKey(info: ByteArray, length: Int, output: ByteArray, offset: Int) {
        assertInitialized()
        if (length < 0) {
            throw IllegalArgumentException("Length must be a non-negative value.")
        } else if (output.size < offset + length) {
            throw ShortBufferException()
        } else {
            val mac = createMac()
            if (length > MAX_KEY_SIZE * mac.macLength) {
                throw IllegalArgumentException(
                    "Requested keys may not be longer than 255 times the underlying HMAC length."
                )
            } else {
                var t = EMPTY_ARRAY
                try {
                    var loc = 0
                    var i: Byte = 1
                    while (loc < length) {
                        mac.update(t)
                        mac.update(info)
                        mac.update(i)
                        t = mac.doFinal()
                        var x = 0
                        while (x < t.size && loc < length) {
                            output[loc] = t[x]
                            ++x
                            ++loc
                        }
                        ++i
                    }
                } finally {
                    Arrays.fill(t, 0.toByte())
                }
            }
        }
    }

    /**
     * @return the generates message authentication code.
     */
    private fun createMac(): Mac {
        try {
            val ex = Mac.getInstance(algorithm)
            ex.init(prk)
            return ex
        } catch (var2: NoSuchAlgorithmException) {
            throw RuntimeException(var2)
        } catch (var3: InvalidKeyException) {
            throw RuntimeException(var3)
        }
    }

    /**
     * Checks for a valid pseudo-random key.
     */
    private fun assertInitialized() {
        if (prk == null) {
            throw IllegalStateException("Hkdf has not been initialized")
        }
    }

    companion object {
        private val MAX_KEY_SIZE = 255
        @Throws(NoSuchAlgorithmException::class)
        fun getInstance(algorithm: String): Hkdf {
            return Hkdf(algorithm)
        }
    }
}