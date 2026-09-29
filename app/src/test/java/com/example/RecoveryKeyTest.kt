package com.example

import com.example.core.security.RecoveryKeyHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.security.SecureRandom

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RecoveryKeyTest {

    @Test
    fun testGenerateRecoveryKey_FormatAndLength() {
        val key = RecoveryKeyHelper.generateRecoveryKey()

        // 14 groups of 4 separated by hyphens -> 14 * 4 + 13 = 69 characters
        assertEquals(69, key.length)

        val groups = key.split("-")
        assertEquals(14, groups.size)
        for (group in groups) {
            assertEquals(4, group.length)
        }

        assertTrue("Key must be valid", RecoveryKeyHelper.isValidRecoveryKey(key))
    }

    @Test
    fun testEntropyLength_DecodesToExact256Bits() {
        val key = RecoveryKeyHelper.generateRecoveryKey()
        val decodedEntropy = RecoveryKeyHelper.decodeAndValidateRecoveryKey(key)

        assertEquals("Entropy must be exactly 32 bytes (256 bits)", 32, decodedEntropy.size)
    }

    @Test
    fun testChecksum_ValidChecksumPasses_CorruptedChecksumFails() {
        val key = RecoveryKeyHelper.generateRecoveryKey()
        assertTrue(RecoveryKeyHelper.isValidRecoveryKey(key))

        // Corrupt the last character (which is part of the checksum)
        val lastChar = key.last()
        val replacement = if (lastChar == '0') '1' else '0'
        val corruptedKey = key.dropLast(1) + replacement

        assertFalse("Corrupted checksum must not be valid", RecoveryKeyHelper.isValidRecoveryKey(corruptedKey))

        try {
            RecoveryKeyHelper.decodeAndValidateRecoveryKey(corruptedKey)
            fail("Expected IllegalArgumentException for invalid checksum")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("checksum mismatch", ignoreCase = true) == true)
        }
    }

    @Test
    fun testTolerantDecoding_CaseHyphenSpaceSubstitutions() {
        val originalKey = RecoveryKeyHelper.generateRecoveryKey()
        val expectedEntropy = RecoveryKeyHelper.decodeAndValidateRecoveryKey(originalKey)

        // Lowercase with spaces instead of hyphens
        val lowerWithSpaces = originalKey.lowercase().replace("-", " ")
        val entropy1 = RecoveryKeyHelper.decodeAndValidateRecoveryKey(lowerWithSpaces)
        assertTrue(expectedEntropy.contentEquals(entropy1))

        // No hyphens or spaces at all
        val noSeparators = originalKey.replace("-", "")
        val entropy2 = RecoveryKeyHelper.decodeAndValidateRecoveryKey(noSeparators)
        assertTrue(expectedEntropy.contentEquals(entropy2))
    }

    @Test
    fun testInvalidCharacters_Rejected() {
        val key = RecoveryKeyHelper.generateRecoveryKey()
        // Replace first character with invalid letter 'U' (excluded from Crockford Base32)
        val invalidKey = "U" + key.substring(1)
        assertFalse(RecoveryKeyHelper.isValidRecoveryKey(invalidKey))
    }

    @Test
    fun testInvalidLength_Rejected() {
        assertFalse(RecoveryKeyHelper.isValidRecoveryKey("1234-5678"))
        assertFalse(RecoveryKeyHelper.isValidRecoveryKey(""))
    }

    @Test
    fun testRecoveryKeyWrapping_Roundtrip_Succeeds() {
        val masterKey = ByteArray(32)
        SecureRandom().nextBytes(masterKey)
        val recoveryKey = RecoveryKeyHelper.generateRecoveryKey()

        val wrappedPayload = RecoveryKeyHelper.wrapMasterKey(masterKey, recoveryKey)
        assertTrue(wrappedPayload.startsWith("RECOVERY_WRAP_V1:"))

        val unwrappedKey = RecoveryKeyHelper.unwrapMasterKey(wrappedPayload, recoveryKey)
        assertEquals(32, unwrappedKey.size)
        assertTrue("Unwrapped master key must match original exactly", masterKey.contentEquals(unwrappedKey))
    }

    @Test
    fun testRecoveryKeyWrapping_WrongKeyRejects() {
        val masterKey = ByteArray(32)
        SecureRandom().nextBytes(masterKey)
        val recoveryKey1 = RecoveryKeyHelper.generateRecoveryKey()
        val recoveryKey2 = RecoveryKeyHelper.generateRecoveryKey()

        val wrappedPayload = RecoveryKeyHelper.wrapMasterKey(masterKey, recoveryKey1)

        try {
            RecoveryKeyHelper.unwrapMasterKey(wrappedPayload, recoveryKey2)
            fail("Expected SecurityException when unwrapping with wrong recovery key")
        } catch (_: SecurityException) {
            // Expected
        }
    }

    @Test
    fun testRecoveryKeyWrapping_TamperedCiphertextRejects() {
        val masterKey = ByteArray(32)
        SecureRandom().nextBytes(masterKey)
        val recoveryKey = RecoveryKeyHelper.generateRecoveryKey()

        val wrappedPayload = RecoveryKeyHelper.wrapMasterKey(masterKey, recoveryKey)
        val prefix = "RECOVERY_WRAP_V1:"
        val jsonBytes = android.util.Base64.decode(wrappedPayload.removePrefix(prefix), android.util.Base64.NO_WRAP)
        val container = org.json.JSONObject(String(jsonBytes, Charsets.UTF_8))

        // Extract and tamper with decoded ciphertext bytes directly
        val cipherBytes = android.util.Base64.decode(container.getString("ciphertext"), android.util.Base64.NO_WRAP)
        assertTrue("Ciphertext byte array must be non-empty", cipherBytes.isNotEmpty())
        cipherBytes[0] = (cipherBytes[0].toInt() xor 0xFF).toByte()
        container.put("ciphertext", android.util.Base64.encodeToString(cipherBytes, android.util.Base64.NO_WRAP))

        val tamperedPayload = "$prefix${android.util.Base64.encodeToString(container.toString().toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)}"

        try {
            RecoveryKeyHelper.unwrapMasterKey(tamperedPayload, recoveryKey)
            fail("Expected SecurityException when unwrapping tampered ciphertext")
        } catch (_: SecurityException) {
            // Expected: AES-GCM AEADBadTagException wrapped in SecurityException
        }
    }

    @Test
    fun testRecoveryKeyWrapping_TamperedSaltRejects() {
        val masterKey = ByteArray(32)
        SecureRandom().nextBytes(masterKey)
        val recoveryKey = RecoveryKeyHelper.generateRecoveryKey()

        val wrappedPayload = RecoveryKeyHelper.wrapMasterKey(masterKey, recoveryKey)
        val prefix = "RECOVERY_WRAP_V1:"
        val jsonBytes = android.util.Base64.decode(wrappedPayload.removePrefix(prefix), android.util.Base64.NO_WRAP)
        val container = org.json.JSONObject(String(jsonBytes, Charsets.UTF_8))

        // Tamper with salt
        val saltBytes = android.util.Base64.decode(container.getString("salt"), android.util.Base64.NO_WRAP)
        saltBytes[0] = (saltBytes[0].toInt() xor 0xFF).toByte()
        container.put("salt", android.util.Base64.encodeToString(saltBytes, android.util.Base64.NO_WRAP))

        val tamperedPayload = "$prefix${android.util.Base64.encodeToString(container.toString().toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)}"

        try {
            RecoveryKeyHelper.unwrapMasterKey(tamperedPayload, recoveryKey)
            fail("Expected SecurityException when unwrapping with tampered salt")
        } catch (_: SecurityException) {
            // Expected
        }
    }

    @Test
    fun testRecoveryKeyWrapping_TamperedNonceRejects() {
        val masterKey = ByteArray(32)
        SecureRandom().nextBytes(masterKey)
        val recoveryKey = RecoveryKeyHelper.generateRecoveryKey()

        val wrappedPayload = RecoveryKeyHelper.wrapMasterKey(masterKey, recoveryKey)
        val prefix = "RECOVERY_WRAP_V1:"
        val jsonBytes = android.util.Base64.decode(wrappedPayload.removePrefix(prefix), android.util.Base64.NO_WRAP)
        val container = org.json.JSONObject(String(jsonBytes, Charsets.UTF_8))

        // Tamper with nonce
        val nonceBytes = android.util.Base64.decode(container.getString("nonce"), android.util.Base64.NO_WRAP)
        nonceBytes[0] = (nonceBytes[0].toInt() xor 0xFF).toByte()
        container.put("nonce", android.util.Base64.encodeToString(nonceBytes, android.util.Base64.NO_WRAP))

        val tamperedPayload = "$prefix${android.util.Base64.encodeToString(container.toString().toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)}"

        try {
            RecoveryKeyHelper.unwrapMasterKey(tamperedPayload, recoveryKey)
            fail("Expected SecurityException when unwrapping with tampered nonce")
        } catch (_: SecurityException) {
            // Expected
        }
    }

    @Test
    fun testEmptyRecoveryKeyRejection() {
        assertFalse(RecoveryKeyHelper.isValidRecoveryKey(""))
        assertFalse(RecoveryKeyHelper.isValidRecoveryKey("   "))
        try {
            RecoveryKeyHelper.decodeAndValidateRecoveryKey("")
            fail("Expected IllegalArgumentException for empty recovery key")
        } catch (_: IllegalArgumentException) {
            // Expected
        }
    }

    @Test
    fun testNonceAndSaltUniqueness() {
        val masterKey = ByteArray(32)
        SecureRandom().nextBytes(masterKey)
        val recoveryKey = RecoveryKeyHelper.generateRecoveryKey()

        val wrap1 = RecoveryKeyHelper.wrapMasterKey(masterKey, recoveryKey)
        val wrap2 = RecoveryKeyHelper.wrapMasterKey(masterKey, recoveryKey)

        // Even with the same master key and recovery key, two wraps MUST be completely different (fresh salt + nonce)
        assertNotEquals("Consecutive wraps must produce different outputs (unique nonce/salt)", wrap1, wrap2)
    }
}
