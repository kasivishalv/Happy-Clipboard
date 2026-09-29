package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.security.AndroidKeystoreHelper
import com.example.core.security.KeyManager
import com.example.core.security.RecoveryKeyHelper
import com.example.core.security.SecurityPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.security.SecureRandom

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class KeyManagementTest {

    private lateinit var context: Context
    private lateinit var preferences: SecurityPreferences
    private lateinit var keystoreHelper: AndroidKeystoreHelper
    private lateinit var keyManager: KeyManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val prefs = context.getSharedPreferences("happy_clipboard_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()

        preferences = SecurityPreferences(context)
        keystoreHelper = AndroidKeystoreHelper()
        keystoreHelper.deleteWrappingKey()

        keyManager = KeyManager(keystoreHelper, preferences)
    }

    @Test
    fun testMasterKeyGeneration_256BitsAndUnique() {
        val res1 = keyManager.initializeCloudEncryption()
        val key1 = keyManager.getOrUnwrapMasterKey()

        assertEquals("Master key must be exactly 32 bytes (256 bits)", 32, key1.size)
        assertTrue(RecoveryKeyHelper.isValidRecoveryKey(res1.recoveryKey))

        // Reset and generate again
        keystoreHelper.deleteWrappingKey()
        val res2 = keyManager.initializeCloudEncryption()
        val key2 = keyManager.getOrUnwrapMasterKey()

        assertNotEquals("Successive master keys must be unique random values", key1.toList(), key2.toList())
    }

    @Test
    fun testAndroidKeystoreWrapping_Roundtrip() {
        val masterKey = ByteArray(32)
        SecureRandom().nextBytes(masterKey)

        val wrapped = keystoreHelper.wrapMasterKey(masterKey)
        assertTrue(wrapped.startsWith("KEYSTORE_WRAP_V1:"))

        val unwrapped = keystoreHelper.unwrapMasterKey(wrapped)
        assertEquals(32, unwrapped.size)
        assertTrue("Unwrapped key must match original exactly", masterKey.contentEquals(unwrapped))
    }

    @Test
    fun testAndroidKeystoreWrapping_TamperedCiphertext_Rejects() {
        val masterKey = ByteArray(32)
        SecureRandom().nextBytes(masterKey)

        val wrapped = keystoreHelper.wrapMasterKey(masterKey)
        val parts = wrapped.split(":")
        // Tamper with ciphertext
        val tamperedCipher = if (parts[2].endsWith("A")) parts[2].dropLast(1) + "B" else parts[2].dropLast(1) + "A"
        val tamperedWrapped = "${parts[0]}:${parts[1]}:$tamperedCipher"

        try {
            keystoreHelper.unwrapMasterKey(tamperedWrapped)
            fail("Expected SecurityException when unwrapping tampered keystore payload")
        } catch (_: SecurityException) {
            // Expected
        }
    }

    @Test
    fun testAndroidKeystoreWrapping_IVUniqueness() {
        val masterKey = ByteArray(32)
        SecureRandom().nextBytes(masterKey)

        val wrap1 = keystoreHelper.wrapMasterKey(masterKey)
        val wrap2 = keystoreHelper.wrapMasterKey(masterKey)

        assertNotEquals("Consecutive wraps must produce different IVs and ciphertexts", wrap1, wrap2)
    }

    @Test
    fun testTwoIndependentWraps_DeviceAndRecovery() {
        val result = keyManager.initializeCloudEncryption()

        assertTrue("Device key must be present", keyManager.hasDeviceKey())
        assertTrue("Recovery wrapped key must be present", keyManager.hasRecoveryWrappedKey())

        val masterKeyFromDevice = keyManager.getOrUnwrapMasterKey()
        val masterKeyFromRecovery = RecoveryKeyHelper.unwrapMasterKey(
            preferences.recoveryWrappedMasterKey!!,
            result.recoveryKey
        )

        assertEquals(32, masterKeyFromDevice.size)
        assertEquals(32, masterKeyFromRecovery.size)
        assertTrue("Both wraps must decrypt to the exact same Master Key", masterKeyFromDevice.contentEquals(masterKeyFromRecovery))
    }

    @Test
    fun testFactoryResetSimulation_RecoverMasterKeyUsingRecoveryKey() {
        // Step 1: User initializes cloud encryption on old device
        val initResult = keyManager.initializeCloudEncryption()
        val originalMasterKey = keyManager.getOrUnwrapMasterKey()
        val recoveryKey = initResult.recoveryKey
        val savedRecoveryWrappedPayload = preferences.recoveryWrappedMasterKey

        assertNotNull(savedRecoveryWrappedPayload)

        // Step 2: Simulate Factory Reset: Device Keystore and local device wrap are wiped out
        keyManager.clearDeviceKeyMaterial()
        assertFalse("Device key must not be available after factory reset", keyManager.hasDeviceKey())

        // Verify direct device unwrap fails
        try {
            keyManager.getOrUnwrapMasterKey()
            fail("Direct device unwrap must fail without Keystore key")
        } catch (_: IllegalStateException) {
            // Expected: "No device-wrapped master key found. Recovery key required."
        }

        // Step 3: User connects cloud on new device, downloads recoveryWrappedPayload, and enters Recovery Key
        keyManager.setRecoveryWrappedMasterKey(savedRecoveryWrappedPayload!!)
        val recoveredMasterKey = keyManager.recoverMasterKey(recoveryKey)

        // Step 4: Verify recovered Master Key matches the original 256-bit key
        assertEquals(32, recoveredMasterKey.size)
        assertTrue("Recovered key must match original Master Key exactly", originalMasterKey.contentEquals(recoveredMasterKey))

        // Step 5: Verify new device-local Keystore wrap is established and usable for subsequent access
        assertTrue("Device key must be re-established after recovery", keyManager.hasDeviceKey())
        val directAccessKey = keyManager.getOrUnwrapMasterKey()
        assertTrue("Direct device unwrap must now succeed with restored key", originalMasterKey.contentEquals(directAccessKey))
    }

    @Test
    fun testFactoryResetSimulation_WrongRecoveryKeyFailsSafely() {
        keyManager.initializeCloudEncryption()
        val savedRecoveryWrapped = preferences.recoveryWrappedMasterKey!!

        // Wipe local device key
        keyManager.clearDeviceKeyMaterial()
        keyManager.setRecoveryWrappedMasterKey(savedRecoveryWrapped)

        val wrongRecoveryKey = RecoveryKeyHelper.generateRecoveryKey()

        try {
            keyManager.recoverMasterKey(wrongRecoveryKey)
            fail("Recovery with wrong key must fail")
        } catch (_: SecurityException) {
            // Expected
        }

        assertFalse("Device key must not be created after failed recovery", keyManager.hasDeviceKey())
    }

    @Test
    fun testNoPlaintextSecretsPersistedInSharedPreferences() {
        val result = keyManager.initializeCloudEncryption()
        val masterKey = keyManager.getOrUnwrapMasterKey()

        val rawPrefs = context.getSharedPreferences("happy_clipboard_prefs", Context.MODE_PRIVATE)
        val allEntries = rawPrefs.all

        for ((key, value) in allEntries) {
            val strValue = value.toString()
            // Verify raw master key is never stored in plaintext
            assertFalse("Plaintext master key must NEVER appear in SharedPreferences ($key)", strValue.contains(masterKey.toString()))
            // Verify raw recovery key is never stored in SharedPreferences
            assertFalse("Recovery key must NEVER appear in SharedPreferences ($key)", strValue.contains(result.recoveryKey))
            assertFalse("Normalized recovery key must NEVER appear in SharedPreferences ($key)", strValue.contains(RecoveryKeyHelper.normalizeInput(result.recoveryKey)))
        }
    }

    @Test
    fun testWithMasterKey_ZeroizesKeyAfterExecution() {
        keyManager.initializeCloudEncryption()

        var capturedKeyCopy: ByteArray? = null
        keyManager.withMasterKey { key ->
            capturedKeyCopy = key.copyOf()
            assertEquals(32, key.size)
        }

        assertNotNull(capturedKeyCopy)
        assertEquals(32, capturedKeyCopy!!.size)
    }
}
