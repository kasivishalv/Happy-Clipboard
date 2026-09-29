package com.example.core.security

import android.util.Base64
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Enterprise-grade cryptographic helper for Happy Clipboard.
 *
 * Implements:
 * 1. Versioned HCB container format: HCB_V2
 * 2. OWASP-aligned PBKDF2-HMAC-SHA256 with 600,000 iterations for password-based export
 * 3. Constant-time authenticated AES-256-GCM encryption with randomized 16-byte salt and 12-byte IV
 * 4. Backward-compatible decryption for legacy HCB containers
 * 5. Salted PBKDF2-HMAC-SHA256 PIN hashing with constant-time equality check
 */
object CryptoHelper {
    private const val GCM_TAG_LENGTH = 128
    private const val GCM_IV_LENGTH = 12
    const val DEFAULT_ITERATION_COUNT = 600_000
    private const val PIN_ITERATION_COUNT = 100_000
    private const val LEGACY_ITERATION_COUNT = 10_000
    private const val KEY_LENGTH = 256

    private const val HEADER_MAGIC = "HCB_V2"
    private const val PIN_HEADER_MAGIC = "PIN_V1"

    fun encrypt(plaintext: String, password: String): String {
        val salt = ByteArray(16)
        val iv = ByteArray(GCM_IV_LENGTH)
        SecureRandom().apply {
            nextBytes(salt)
            nextBytes(iv)
        }

        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password.toCharArray(), salt, DEFAULT_ITERATION_COUNT, KEY_LENGTH)
        val secretKey = SecretKeySpec(factory.generateSecret(spec).encoded, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)

        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        val saltB64 = Base64.encodeToString(salt, Base64.NO_WRAP)
        val ivB64 = Base64.encodeToString(iv, Base64.NO_WRAP)
        val cipherB64 = Base64.encodeToString(ciphertext, Base64.NO_WRAP)

        // Versioned container with metadata
        val container = JSONObject().apply {
            put("magic", HEADER_MAGIC)
            put("hcb_version", 2)
            put("app_version", "1.0.0")
            put("kdf", "PBKDF2WithHmacSHA256")
            put("iterations", DEFAULT_ITERATION_COUNT)
            put("key_len", KEY_LENGTH)
            put("salt", saltB64)
            put("iv", ivB64)
            put("ciphertext", cipherB64)
            put("created_at", System.currentTimeMillis())
        }

        return "$HEADER_MAGIC:${Base64.encodeToString(container.toString().toByteArray(Charsets.UTF_8), Base64.NO_WRAP)}"
    }

    fun decrypt(encryptedPayload: String, password: String): String {
        val trimmed = encryptedPayload.trim()

        // Check if versioned HCB container
        if (trimmed.startsWith("$HEADER_MAGIC:")) {
            val base64Container = trimmed.substringAfter("$HEADER_MAGIC:")
            val jsonString = String(Base64.decode(base64Container, Base64.NO_WRAP), Charsets.UTF_8)
            val container = JSONObject(jsonString)

            val salt = Base64.decode(container.getString("salt"), Base64.NO_WRAP)
            val iv = Base64.decode(container.getString("iv"), Base64.NO_WRAP)
            val ciphertext = Base64.decode(container.getString("ciphertext"), Base64.NO_WRAP)
            val iterations = container.optInt("iterations", DEFAULT_ITERATION_COUNT)
            val keyLen = container.optInt("key_len", KEY_LENGTH)

            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val spec = PBEKeySpec(password.toCharArray(), salt, iterations, keyLen)
            val secretKey = SecretKeySpec(factory.generateSecret(spec).encoded, "AES")

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

            val plaintextBytes = cipher.doFinal(ciphertext)
            return String(plaintextBytes, Charsets.UTF_8)
        }

        // Backward compatibility: Legacy "salt:iv:ciphertext" format
        val parts = trimmed.split(":")
        if (parts.size == 3) {
            val salt = Base64.decode(parts[0], Base64.NO_WRAP)
            val iv = Base64.decode(parts[1], Base64.NO_WRAP)
            val ciphertext = Base64.decode(parts[2], Base64.NO_WRAP)

            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val spec = PBEKeySpec(password.toCharArray(), salt, LEGACY_ITERATION_COUNT, KEY_LENGTH)
            val secretKey = SecretKeySpec(factory.generateSecret(spec).encoded, "AES")

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

            val plaintextBytes = cipher.doFinal(ciphertext)
            return String(plaintextBytes, Charsets.UTF_8)
        }

        throw IllegalArgumentException("Unsupported or corrupt HCB backup container format")
    }

    /**
     * Hashes a 4-digit PIN using PBKDF2-HMAC-SHA256 with a cryptographically secure random 16-byte salt.
     * Returns a serialized representation: "PIN_V1:<salt_b64>:<hash_b64>"
     */
    fun hashPin(pin: String): String {
        require(pin.length == 4 && pin.all { it.isDigit() }) { "PIN must be exactly 4 digits" }
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)

        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(pin.toCharArray(), salt, PIN_ITERATION_COUNT, KEY_LENGTH)
        val hash = factory.generateSecret(spec).encoded

        val saltB64 = Base64.encodeToString(salt, Base64.NO_WRAP)
        val hashB64 = Base64.encodeToString(hash, Base64.NO_WRAP)
        return "$PIN_HEADER_MAGIC:$saltB64:$hashB64"
    }

    /**
     * Verifies a candidate PIN against a stored credential.
     * Supports:
     * 1. Versioned salted PBKDF2 hash (PIN_V1) using constant-time comparison.
     * 2. Seamless legacy migration for user-configured plaintext PINs (strictly excluding any universal/default PIN).
     */
    fun verifyPin(candidatePin: String, stored: String): Boolean {
        if (candidatePin.length != 4 || !candidatePin.all { it.isDigit() } || stored.isBlank()) {
            return false
        }

        if (stored.startsWith("$PIN_HEADER_MAGIC:")) {
            val parts = stored.split(":")
            if (parts.size == 3) {
                return try {
                    val salt = Base64.decode(parts[1], Base64.NO_WRAP)
                    val expectedHash = Base64.decode(parts[2], Base64.NO_WRAP)

                    val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    val spec = PBEKeySpec(candidatePin.toCharArray(), salt, PIN_ITERATION_COUNT, KEY_LENGTH)
                    val actualHash = factory.generateSecret(spec).encoded

                    MessageDigest.isEqual(expectedHash, actualHash)
                } catch (_: Exception) {
                    false
                }
            }
        }

        // Backward compatibility: If an existing user had a legacy plaintext PIN stored,
        // strictly ensure it is not empty, matches candidate, and is not a universal fallback.
        if (stored.length == 4 && stored.all { it.isDigit() } && stored != "1234") {
            return stored == candidatePin
        }

        return false
    }
}
