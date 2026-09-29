package com.example.core.security

import java.security.SecureRandom

/**
 * Result returned upon initializing cloud encryption.
 */
data class KeyInitializationResult(
    val recoveryKey: String,
    val keyVersion: Int = 1,
    val isNewKey: Boolean = true
)

/**
 * Orchestrator for Happy Clipboard Cloud Master Key lifecycle.
 *
 * Manages:
 * 1. 256-bit Cloud Master Key generation via SecureRandom.
 * 2. Device-local hardware-backed protection via AndroidKeystoreHelper.
 * 3. High-entropy Recovery Key derivation and wrapping via RecoveryKeyHelper.
 * 4. Factory-reset / new-device recovery flow without the old device's Keystore.
 * 5. Secure memory management (zeroizing keys after operations).
 */
class KeyManager(
    private val keystoreHelper: AndroidKeystoreHelper = AndroidKeystoreHelper(),
    private val preferences: SecurityPreferences
) {

    companion object {
        const val MASTER_KEY_BYTES = 32 // 256 bits
    }

    /**
     * Initializes cloud encryption:
     * - Generates a fresh 256-bit Cloud Master Key.
     * - Generates (or uses provided) 256-bit Recovery Key.
     * - Wraps master key for device storage using Android Keystore.
     * - Wraps master key for disaster recovery using Recovery Key.
     * - Zeroizes temporary raw key material.
     */
    fun initializeCloudEncryption(customRecoveryKey: String? = null): KeyInitializationResult {
        val masterKey = ByteArray(MASTER_KEY_BYTES)
        SecureRandom().nextBytes(masterKey)

        try {
            val recoveryKey = customRecoveryKey?.let {
                RecoveryKeyHelper.decodeAndValidateRecoveryKey(it)
                RecoveryKeyHelper.normalizeInput(it).chunked(4).joinToString("-")
            } ?: RecoveryKeyHelper.generateRecoveryKey()

            // 1. Device wrap via Android Keystore
            val deviceWrapped = keystoreHelper.wrapMasterKey(masterKey)
            preferences.deviceWrappedMasterKey = deviceWrapped

            // 2. Recovery wrap via Recovery Key + PBKDF2 (600,000 iterations)
            val recoveryWrapped = RecoveryKeyHelper.wrapMasterKey(masterKey, recoveryKey)
            preferences.recoveryWrappedMasterKey = recoveryWrapped

            preferences.cloudMasterKeyVersion = 1
            preferences.cloudSyncEnabled = true

            return KeyInitializationResult(
                recoveryKey = recoveryKey,
                keyVersion = 1,
                isNewKey = true
            )
        } finally {
            masterKey.fill(0)
        }
    }

    /**
     * Checks if cloud encryption has been initialized on this or an associated device.
     */
    fun hasCloudEncryptionKey(): Boolean {
        return hasDeviceKey() || hasRecoveryWrappedKey()
    }

    /**
     * Checks if this device currently has hardware Keystore key material to unwrap the master key locally.
     */
    fun hasDeviceKey(): Boolean {
        val wrapped = preferences.deviceWrappedMasterKey
        return !wrapped.isNullOrBlank() && keystoreHelper.hasWrappingKey()
    }

    /**
     * Checks if a recovery-wrapped master key exists in preferences.
     */
    fun hasRecoveryWrappedKey(): Boolean {
        return !preferences.recoveryWrappedMasterKey.isNullOrBlank()
    }

    /**
     * Gets the recovery-wrapped master key payload (used for cloud storage / disaster recovery).
     */
    fun getRecoveryWrappedMasterKey(): String? {
        return preferences.recoveryWrappedMasterKey
    }

    /**
     * Sets the recovery-wrapped master key payload (e.g., when downloaded from cloud on a new device).
     */
    fun setRecoveryWrappedMasterKey(wrappedPayload: String) {
        preferences.recoveryWrappedMasterKey = wrappedPayload
    }

    /**
     * Unwraps the Cloud Master Key using device-local Android Keystore.
     * Throws IllegalStateException if local Keystore material is unavailable (e.g. after a factory reset).
     */
    fun getOrUnwrapMasterKey(): ByteArray {
        val wrapped = preferences.deviceWrappedMasterKey
            ?: throw IllegalStateException("No device-wrapped master key found. Recovery key required.")

        return keystoreHelper.unwrapMasterKey(wrapped)
    }

    /**
     * Recovers the Cloud Master Key using the user's high-entropy Recovery Key.
     * - Unwraps the master key from the recovery container.
     * - Immediately re-wraps the master key with the current device's Android Keystore.
     * - Persists the new device-wrapped master key.
     * - Returns the raw 32-byte master key.
     */
    fun recoverMasterKey(recoveryKey: String): ByteArray {
        val recoveryWrapped = preferences.recoveryWrappedMasterKey
            ?: throw IllegalStateException("No recovery-wrapped master key available to restore from.")

        val masterKey = RecoveryKeyHelper.unwrapMasterKey(recoveryWrapped, recoveryKey)
        try {
            // Re-wrap and protect for this new device's Keystore
            val newDeviceWrap = keystoreHelper.wrapMasterKey(masterKey)
            preferences.deviceWrappedMasterKey = newDeviceWrap
            preferences.cloudSyncEnabled = true
            return masterKey.copyOf()
        } finally {
            masterKey.fill(0)
        }
    }

    /**
     * Executes a sensitive cryptographic operation with the unwrapped master key,
     * ensuring the raw key bytes are zeroized immediately after execution.
     */
    fun <T> withMasterKey(block: (ByteArray) -> T): T {
        val masterKey = getOrUnwrapMasterKey()
        return try {
            block(masterKey)
        } finally {
            masterKey.fill(0)
        }
    }

    /**
     * Deletes the device-local Keystore key and device-wrapped payload (simulates factory reset or logout).
     */
    fun clearDeviceKeyMaterial() {
        keystoreHelper.deleteWrappingKey()
        preferences.deviceWrappedMasterKey = null
    }

    /**
     * Completely removes all cloud key material (both device and recovery wraps).
     */
    fun clearAllCloudKeys() {
        keystoreHelper.deleteWrappingKey()
        preferences.clearCloudKeys()
    }
}
