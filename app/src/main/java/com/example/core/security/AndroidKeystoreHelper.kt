package com.example.core.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Android Keystore helper for Happy Clipboard.
 *
 * Generates and stores a device-local hardware-backed AES-256 key in AndroidKeyStore.
 * Wraps the Cloud Master Key using AES/GCM/NoPadding with a fresh random IV for each wrap.
 * The master key never leaves the device in plaintext.
 */
class AndroidKeystoreHelper(
    private val keyStoreProvider: String = ANDROID_KEYSTORE
) {

    companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val MASTER_KEY_WRAPPER_ALIAS = "happy_clipboard_master_key_device_wrap"
        const val WRAP_HEADER = "KEYSTORE_WRAP_V1"
        const val GCM_TAG_LENGTH = 128
        const val MASTER_KEY_LENGTH = 32 // 256 bits
    }

    private val isKeystoreProviderAvailable: Boolean =
        java.security.Security.getProvider(keyStoreProvider) != null

    private val testPasswordProtection = KeyStore.PasswordProtection(charArrayOf())

    private val keyStore: KeyStore by lazy {
        if (isKeystoreProviderAvailable) {
            KeyStore.getInstance(keyStoreProvider).apply {
                load(null)
            }
        } else {
            // Software keystore for headless JVM / test execution
            KeyStore.getInstance(KeyStore.getDefaultType()).apply {
                load(null, null)
            }
        }
    }

    /**
     * Checks if the device wrapping key exists in KeyStore.
     */
    fun hasWrappingKey(): Boolean {
        return keyStore.containsAlias(MASTER_KEY_WRAPPER_ALIAS)
    }

    /**
     * Retrieves or generates a 256-bit AES key inside AndroidKeyStore.
     */
    fun getOrCreateWrappingKey(): SecretKey {
        if (keyStore.containsAlias(MASTER_KEY_WRAPPER_ALIAS)) {
            val entry = if (isKeystoreProviderAvailable) {
                keyStore.getEntry(MASTER_KEY_WRAPPER_ALIAS, null) as? KeyStore.SecretKeyEntry
            } else {
                keyStore.getEntry(MASTER_KEY_WRAPPER_ALIAS, testPasswordProtection) as? KeyStore.SecretKeyEntry
            }
            if (entry != null) {
                return entry.secretKey
            }
        }

        return if (isKeystoreProviderAvailable) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, keyStoreProvider)
            val spec = KeyGenParameterSpec.Builder(
                MASTER_KEY_WRAPPER_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build()

            keyGenerator.init(spec)
            keyGenerator.generateKey()
        } else {
            val keyGenerator = KeyGenerator.getInstance("AES")
            keyGenerator.init(256)
            val secretKey = keyGenerator.generateKey()
            keyStore.setEntry(
                MASTER_KEY_WRAPPER_ALIAS,
                KeyStore.SecretKeyEntry(secretKey),
                testPasswordProtection
            )
            secretKey
        }
    }

    /**
     * Wraps the 256-bit Cloud Master Key using the Keystore AES key with AES/GCM/NoPadding.
     * Generates a fresh random IV for every call.
     * Output format: "KEYSTORE_WRAP_V1:<base64_iv>:<base64_ciphertext>"
     */
    fun wrapMasterKey(masterKeyBytes: ByteArray): String {
        require(masterKeyBytes.size == MASTER_KEY_LENGTH) {
            "Master key must be exactly $MASTER_KEY_LENGTH bytes (256 bits)"
        }

        val secretKey = getOrCreateWrappingKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv ?: throw IllegalStateException("Cipher failed to generate IV")
        val ciphertext = cipher.doFinal(masterKeyBytes)

        val ivB64 = Base64.encodeToString(iv, Base64.NO_WRAP)
        val cipherB64 = Base64.encodeToString(ciphertext, Base64.NO_WRAP)
        return "$WRAP_HEADER:$ivB64:$cipherB64"
    }

    /**
     * Unwraps the Cloud Master Key from a Keystore-wrapped payload.
     * Throws SecurityException if decryption or authentication tag check fails.
     */
    fun unwrapMasterKey(wrappedPayload: String): ByteArray {
        val trimmed = wrappedPayload.trim()
        if (!trimmed.startsWith("$WRAP_HEADER:")) {
            throw IllegalArgumentException("Unsupported keystore wrap format: expected header $WRAP_HEADER")
        }

        val parts = trimmed.split(":")
        if (parts.size != 3) {
            throw IllegalArgumentException("Invalid keystore wrap payload components")
        }

        val iv = Base64.decode(parts[1], Base64.NO_WRAP)
        val ciphertext = Base64.decode(parts[2], Base64.NO_WRAP)

        val secretKey = if (keyStore.containsAlias(MASTER_KEY_WRAPPER_ALIAS)) {
            val entry = if (isKeystoreProviderAvailable) {
                keyStore.getEntry(MASTER_KEY_WRAPPER_ALIAS, null) as? KeyStore.SecretKeyEntry
            } else {
                keyStore.getEntry(MASTER_KEY_WRAPPER_ALIAS, testPasswordProtection) as? KeyStore.SecretKeyEntry
            }
            entry?.secretKey ?: throw IllegalStateException("Keystore wrapping key entry invalid")
        } else {
            throw IllegalStateException("Keystore wrapping key not found on this device")
        }

        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            val masterKey = cipher.doFinal(ciphertext)
            if (masterKey.size != MASTER_KEY_LENGTH) {
                throw SecurityException("Invalid unwrapped master key length: ${masterKey.size}")
            }
            masterKey
        } catch (e: Exception) {
            throw SecurityException("Failed to unwrap master key from Android Keystore", e)
        }
    }

    /**
     * Deletes the wrapping key from AndroidKeyStore (e.g., on device deregistration or factory reset).
     */
    fun deleteWrappingKey() {
        if (keyStore.containsAlias(MASTER_KEY_WRAPPER_ALIAS)) {
            keyStore.deleteEntry(MASTER_KEY_WRAPPER_ALIAS)
        }
    }
}
