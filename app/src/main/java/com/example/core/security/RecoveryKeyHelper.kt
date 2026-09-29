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
 * Enterprise-grade Recovery Key management and key-wrapping helper for Happy Clipboard.
 *
 * Implements:
 * 1. 256-bit cryptographically secure random entropy generation.
 * 2. Human-readable, error-resistant Crockford Base32 encoding with 20-bit SHA-256 checksum.
 * 3. Normalization and tolerant decoding (case-insensitive, ignores hyphens/spaces, maps O->0, I/L->1).
 * 4. OWASP-aligned PBKDF2-HMAC-SHA256 key derivation with 600,000 iterations and 16-byte random salt.
 * 5. Authenticated AES-256-GCM master key wrapping and unwrapping with fresh 12-byte random nonces.
 */
object RecoveryKeyHelper {

    const val ENTROPY_BYTES = 32 // 256 bits of true randomness
    const val SALT_BYTES = 16
    const val NONCE_BYTES = 12
    const val GCM_TAG_LENGTH = 128
    const val KEY_LENGTH_BITS = 256
    const val KDF_ITERATIONS = 600_000
    const val KDF_ALGORITHM = "PBKDF2WithHmacSHA256"
    const val CIPHER_ALGORITHM = "AES/GCM/NoPadding"
    const val WRAP_HEADER = "RECOVERY_WRAP_V1"

    // Crockford Base32 alphabet: 32 alphanumeric symbols excluding ambiguous letters (I, L, O, U)
    private const val ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"
    private const val ENCODED_DATA_CHARS = 52 // 256 bits / 5 bits per char = 51.2 -> 52 chars (260 bits, 4 bits pad)
    private const val CHECKSUM_CHARS = 4      // 20 bits of SHA-256 checksum = 4 chars
    const val TOTAL_KEY_CHARS = ENCODED_DATA_CHARS + CHECKSUM_CHARS // 56 characters (14 groups of 4)

    /**
     * Generates a 256-bit high-entropy Recovery Key formatted as 14 hyphen-separated 4-character blocks:
     * e.g., "XXXX-XXXX-XXXX-XXXX-XXXX-XXXX-XXXX-XXXX-XXXX-XXXX-XXXX-XXXX-XXXX-XXXX"
     */
    fun generateRecoveryKey(): String {
        val entropy = ByteArray(ENTROPY_BYTES)
        SecureRandom().nextBytes(entropy)
        return encodeRecoveryKey(entropy)
    }

    /**
     * Encodes raw 32-byte (256-bit) entropy into a Crockford Base32 formatted recovery string with checksum.
     */
    fun encodeRecoveryKey(entropy: ByteArray): String {
        require(entropy.size == ENTROPY_BYTES) { "Entropy must be exactly $ENTROPY_BYTES bytes (256 bits)" }

        // Encode 256 bits (32 bytes) into 52 Crockford characters
        val dataChars = encodeBase32(entropy, ENCODED_DATA_CHARS)

        // Compute 20-bit checksum from SHA-256 hash of raw entropy
        val checksumChars = computeChecksumChars(entropy)

        val fullRaw = dataChars + checksumChars
        check(fullRaw.length == TOTAL_KEY_CHARS) { "Invalid encoded length: ${fullRaw.length}" }

        // Format as 14 groups of 4 separated by hyphens
        return fullRaw.chunked(4).joinToString("-")
    }

    /**
     * Validates and decodes a recovery key string into raw 32-byte entropy.
     * Tolerates lowercase, spaces, hyphens, and standard Crockford substitutions (O->0, I/L->1).
     * Throws IllegalArgumentException on invalid length, invalid characters, or checksum mismatch.
     */
    fun decodeAndValidateRecoveryKey(inputKey: String): ByteArray {
        val normalized = normalizeInput(inputKey)

        if (normalized.length != TOTAL_KEY_CHARS) {
            throw IllegalArgumentException(
                "Invalid recovery key length: expected $TOTAL_KEY_CHARS characters, got ${normalized.length}"
            )
        }

        for (ch in normalized) {
            if (ALPHABET.indexOf(ch) < 0) {
                throw IllegalArgumentException("Invalid character in recovery key: '$ch'")
            }
        }

        val dataPart = normalized.substring(0, ENCODED_DATA_CHARS)
        val checksumPart = normalized.substring(ENCODED_DATA_CHARS)

        val entropy = decodeBase32(dataPart, ENTROPY_BYTES)

        // Verify checksum
        val expectedChecksum = computeChecksumChars(entropy)
        if (expectedChecksum != checksumPart) {
            throw IllegalArgumentException("Recovery key checksum mismatch. Please re-check the entered key.")
        }

        return entropy
    }

    /**
     * Checks whether an input recovery key string has valid syntax and a matching checksum without throwing.
     */
    fun isValidRecoveryKey(inputKey: String): Boolean {
        return try {
            decodeAndValidateRecoveryKey(inputKey)
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Wraps a 256-bit Cloud Master Key using a derived key from the Recovery Key via PBKDF2 (600,000 iterations).
     * Output is an authenticated versioned container string: "RECOVERY_WRAP_V1:<base64(container_json)>".
     */
    fun wrapMasterKey(masterKeyBytes: ByteArray, recoveryKey: String): String {
        require(masterKeyBytes.size == ENTROPY_BYTES) { "Master key must be exactly $ENTROPY_BYTES bytes (256 bits)" }
        // Validate recovery key syntax & checksum
        val rawEntropy = decodeAndValidateRecoveryKey(recoveryKey)
        val normalizedKey = normalizeInput(recoveryKey)

        val salt = ByteArray(SALT_BYTES)
        val nonce = ByteArray(NONCE_BYTES)
        SecureRandom().apply {
            nextBytes(salt)
            nextBytes(nonce)
        }

        val derivedKey = deriveKey(normalizedKey.toCharArray(), salt, KDF_ITERATIONS)

        val cipher = Cipher.getInstance(CIPHER_ALGORITHM)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, nonce)
        cipher.init(Cipher.ENCRYPT_MODE, derivedKey, gcmSpec)
        val ciphertext = cipher.doFinal(masterKeyBytes)

        val container = JSONObject().apply {
            put("version", 1)
            put("kdf", KDF_ALGORITHM)
            put("iterations", KDF_ITERATIONS)
            put("key_len", KEY_LENGTH_BITS)
            put("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            put("cipher", "AES-256-GCM")
            put("nonce", Base64.encodeToString(nonce, Base64.NO_WRAP))
            put("ciphertext", Base64.encodeToString(ciphertext, Base64.NO_WRAP))
            put("created_at", System.currentTimeMillis())
        }

        // Clean raw entropy from memory
        rawEntropy.fill(0)

        val containerJson = container.toString()
        val encodedPayload = Base64.encodeToString(containerJson.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        return "$WRAP_HEADER:$encodedPayload"
    }

    /**
     * Unwraps a 256-bit Cloud Master Key from a versioned container using the Recovery Key.
     * Throws SecurityException if the key is incorrect or ciphertext has been tampered with.
     */
    fun unwrapMasterKey(wrappedPayload: String, recoveryKey: String): ByteArray {
        val trimmed = wrappedPayload.trim()
        if (!trimmed.startsWith("$WRAP_HEADER:")) {
            throw IllegalArgumentException("Unsupported wrapped payload format. Expected header $WRAP_HEADER")
        }

        val normalizedKey = normalizeInput(recoveryKey)
        // Validate checksum
        decodeAndValidateRecoveryKey(normalizedKey)

        val base64Json = trimmed.substringAfter("$WRAP_HEADER:")
        val jsonBytes = try {
            Base64.decode(base64Json, Base64.NO_WRAP)
        } catch (e: Exception) {
            throw IllegalArgumentException("Corrupt base64 in wrapped payload", e)
        }

        val container = JSONObject(String(jsonBytes, Charsets.UTF_8))
        val version = container.optInt("version", 0)
        if (version != 1) {
            throw IllegalArgumentException("Unsupported wrapped payload version: $version")
        }

        val iterations = container.getInt("iterations")
        val salt = Base64.decode(container.getString("salt"), Base64.NO_WRAP)
        val nonce = Base64.decode(container.getString("nonce"), Base64.NO_WRAP)
        val ciphertext = Base64.decode(container.getString("ciphertext"), Base64.NO_WRAP)

        val derivedKey = deriveKey(normalizedKey.toCharArray(), salt, iterations)

        return try {
            val cipher = Cipher.getInstance(CIPHER_ALGORITHM)
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, nonce)
            cipher.init(Cipher.DECRYPT_MODE, derivedKey, gcmSpec)
            val masterKey = cipher.doFinal(ciphertext)
            if (masterKey.size != ENTROPY_BYTES) {
                throw SecurityException("Invalid unwrapped key length: ${masterKey.size}")
            }
            masterKey
        } catch (e: Exception) {
            throw SecurityException("Recovery key unwrapping failed: incorrect key or tampered payload", e)
        }
    }

    /**
     * Derives a 256-bit AES key from the normalized recovery key characters and salt via PBKDF2WithHmacSHA256.
     */
    private fun deriveKey(keyChars: CharArray, salt: ByteArray, iterations: Int): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance(KDF_ALGORITHM)
        val spec = PBEKeySpec(keyChars, salt, iterations, KEY_LENGTH_BITS)
        val secret = factory.generateSecret(spec).encoded
        return SecretKeySpec(secret, "AES")
    }

    /**
     * Normalizes user input: converts to uppercase, removes spaces/hyphens, maps O->0, I/L->1.
     */
    fun normalizeInput(input: String): String {
        val sb = StringBuilder()
        for (ch in input.uppercase().trim()) {
            when (ch) {
                '-', ' ', '\t', '\n', '\r' -> continue
                'O' -> sb.append('0')
                'I', 'L' -> sb.append('1')
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    private fun computeChecksumChars(entropy: ByteArray): String {
        val sha256 = MessageDigest.getInstance("SHA-256").digest(entropy)
        // Extract first 20 bits (2.5 bytes = 4 Crockford chars of 5 bits each)
        val b0 = sha256[0].toInt() and 0xFF
        val b1 = sha256[1].toInt() and 0xFF
        val b2 = sha256[2].toInt() and 0xFF
        val bits20 = (b0 shl 16) or (b1 shl 8) or b2 // top 24 bits

        val c1 = ALPHABET[(bits20 ushr 19) and 0x1F]
        val c2 = ALPHABET[(bits20 ushr 14) and 0x1F]
        val c3 = ALPHABET[(bits20 ushr 9) and 0x1F]
        val c4 = ALPHABET[(bits20 ushr 4) and 0x1F]
        return "$c1$c2$c3$c4"
    }

    private fun encodeBase32(data: ByteArray, targetCharCount: Int): String {
        val sb = StringBuilder()
        var buffer = 0
        var bitsLeft = 0

        for (b in data) {
            buffer = (buffer shl 8) or (b.toInt() and 0xFF)
            bitsLeft += 8
            while (bitsLeft >= 5) {
                val index = (buffer ushr (bitsLeft - 5)) and 0x1F
                sb.append(ALPHABET[index])
                bitsLeft -= 5
            }
        }
        if (bitsLeft > 0) {
            val index = (buffer shl (5 - bitsLeft)) and 0x1F
            sb.append(ALPHABET[index])
        }
        return sb.toString().padEnd(targetCharCount, '0').substring(0, targetCharCount)
    }

    private fun decodeBase32(encoded: String, expectedByteCount: Int): ByteArray {
        val result = ByteArray(expectedByteCount)
        var buffer = 0
        var bitsLeft = 0
        var byteIndex = 0

        for (ch in encoded) {
            val charVal = ALPHABET.indexOf(ch)
            if (charVal < 0) {
                throw IllegalArgumentException("Illegal character '$ch' in Base32 stream")
            }
            buffer = (buffer shl 5) or charVal
            bitsLeft += 5
            if (bitsLeft >= 8) {
                if (byteIndex < expectedByteCount) {
                    result[byteIndex++] = ((buffer ushr (bitsLeft - 8)) and 0xFF).toByte()
                }
                bitsLeft -= 8
            }
        }
        return result
    }
}
