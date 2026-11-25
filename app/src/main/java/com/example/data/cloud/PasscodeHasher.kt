package com.example.data.cloud

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

data class SaltedPasscode(
    val hashHex: String,
    val saltHex: String,
)

object PasscodeHasher {
    private const val SALT_BYTES = 16
    private const val HASH_BITS = 256
    private const val ITERATIONS = 10_000
    private val random = SecureRandom()

    fun hashNewPasscode(passcode: String): SaltedPasscode {
        val salt = ByteArray(SALT_BYTES)
        random.nextBytes(salt)
        val hash = pbkdf2(passcode, salt)
        return SaltedPasscode(
            hashHex = hash.toHexString(),
            saltHex = salt.toHexString(),
        )
    }

    fun verifyPasscode(passcode: String, saltHex: String, expectedHashHex: String): Boolean {
        if (passcode.isEmpty() || saltHex.isBlank() || expectedHashHex.isBlank()) return false
        val salt = hexToBytes(saltHex) ?: return false
        val expected = hexToBytes(expectedHashHex) ?: return false
        val actual = pbkdf2(passcode, salt)
        return MessageDigest.isEqual(actual, expected)
    }

    private fun pbkdf2(passcode: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(passcode.toCharArray(), salt, ITERATIONS, HASH_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec)
                .encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun ByteArray.toHexString(): String =
        joinToString(separator = "") { b -> "%02x".format(b.toInt() and 0xFF) }

    private fun hexToBytes(hex: String): ByteArray? {
        if (hex.length % 2 != 0) return null
        return runCatching {
            ByteArray(hex.length / 2) { index ->
                val i = index * 2
                hex.substring(i, i + 2).toInt(16).toByte()
            }
        }.getOrNull()
    }
}
