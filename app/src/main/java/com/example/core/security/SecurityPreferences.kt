package com.example.core.security

import android.content.Context
import android.content.SharedPreferences

class SecurityPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("happy_clipboard_prefs", Context.MODE_PRIVATE)

    var isAutoCaptureEnabled: Boolean
        get() = prefs.getBoolean("auto_capture", true)
        set(value) = prefs.edit().putBoolean("auto_capture", value).apply()

    var duplicateHandling: String // "MERGE", "KEEP_ALL"
        get() = prefs.getString("duplicate_handling", "MERGE") ?: "MERGE"
        set(value) = prefs.edit().putString("duplicate_handling", value).apply()

    var maxHistoryCapacity: String
        get() {
            val str = prefs.getString("max_history_capacity", null)
            if (str != null) return str
            if (prefs.contains("max_history")) {
                val legacyInt = prefs.getInt("max_history", 500)
                return if (legacyInt <= 0) CAPACITY_UNLIMITED else legacyInt.toString()
            }
            return "500"
        }
        set(value) = prefs.edit().putString("max_history_capacity", value).apply()

    val isCapacityUnlimited: Boolean
        get() = maxHistoryCapacity.equals(CAPACITY_UNLIMITED, ignoreCase = true)

    val maxHistoryLimitOrNull: Int?
        get() = if (isCapacityUnlimited) null else maxHistoryCapacity.toIntOrNull()

    var retentionDays: Int // 0 for unlimited, 7, 30, 90
        get() = prefs.getInt("retention_days", 30)
        set(value) = prefs.edit().putInt("retention_days", value).apply()

    var sensitiveHandling: String // "SAVE_AND_MASK", "DONT_SAVE", "SAVE_ALL"
        get() = prefs.getString("sensitive_handling", "SAVE_AND_MASK") ?: "SAVE_AND_MASK"
        set(value) = prefs.edit().putString("sensitive_handling", value).apply()

    var otpAutoDeleteMinutes: Int // 0 for never, 1, 5, 15
        get() = prefs.getInt("otp_auto_delete_minutes", 5)
        set(value) = prefs.edit().putInt("otp_auto_delete_minutes", value).apply()

    var isAppLockEnabled: Boolean
        get() = prefs.getBoolean("app_lock_enabled", false)
        set(value) = prefs.edit().putBoolean("app_lock_enabled", value).apply()

    var isBiometricEnabled: Boolean
        get() = prefs.getBoolean("biometric_enabled", false)
        set(value) = prefs.edit().putBoolean("biometric_enabled", value).apply()

    var excludedPackages: Set<String>
        get() = prefs.getStringSet("excluded_packages", setOf("com.android.settings", "com.google.android.keep")) ?: emptySet()
        set(value) = prefs.edit().putStringSet("excluded_packages", value).apply()

    var appLockPinHash: String
        get() = prefs.getString("app_lock_pin_hash", "") ?: ""
        set(value) = prefs.edit().putString("app_lock_pin_hash", value).apply()

    val isPinSet: Boolean
        get() {
            val hash = appLockPinHash
            if (hash.isNotBlank()) return true
            // Legacy migration: check if a user previously configured a custom PIN (strictly excluding 1234)
            val legacy = prefs.getString("app_lock_pin", null)
            if (!legacy.isNullOrBlank() && legacy != "1234" && legacy.length == 4 && legacy.all { it.isDigit() }) {
                val newHash = CryptoHelper.hashPin(legacy)
                appLockPinHash = newHash
                prefs.edit().remove("app_lock_pin").apply()
                return true
            } else if (legacy == "1234") {
                // Purge legacy default PIN completely
                prefs.edit().remove("app_lock_pin").apply()
            }
            return false
        }

    fun setPin(pin: String) {
        require(pin.length == 4 && pin.all { it.isDigit() }) { "PIN must be exactly 4 digits" }
        appLockPinHash = CryptoHelper.hashPin(pin)
        prefs.edit().remove("app_lock_pin").apply()
    }

    fun verifyPin(candidatePin: String): Boolean {
        if (!isPinSet) return false
        return CryptoHelper.verifyPin(candidatePin, appLockPinHash)
    }

    fun clearPin() {
        prefs.edit().remove("app_lock_pin_hash").remove("app_lock_pin").apply()
    }

    @Deprecated("Use setPin / verifyPin")
    var appLockPin: String
        get() = ""
        set(value) {
            if (value.length == 4 && value.all { it.isDigit() } && value != "1234") {
                setPin(value)
            }
        }

    var isSecureScreensEnabled: Boolean
        get() = prefs.getBoolean("secure_screens", false)
        set(value) = prefs.edit().putBoolean("secure_screens", value).apply()

    var themeMode: String // "SYSTEM", "LIGHT", "DARK"
        get() = prefs.getString("theme_mode", "SYSTEM") ?: "SYSTEM"
        set(value) = prefs.edit().putString("theme_mode", value).apply()

    var isCompactView: Boolean
        get() = prefs.getBoolean("compact_view", false)
        set(value) = prefs.edit().putBoolean("compact_view", value).apply()

    var initialDataSeeded: Boolean
        get() = prefs.getBoolean("initial_data_seeded", false)
        set(value) = prefs.edit().putBoolean("initial_data_seeded", value).apply()

    // Encrypted Cloud Sync & Key Management
    var deviceWrappedMasterKey: String?
        get() = prefs.getString("cloud_master_key_device_wrapped", null)
        set(value) = prefs.edit().putString("cloud_master_key_device_wrapped", value).apply()

    var recoveryWrappedMasterKey: String?
        get() = prefs.getString("cloud_master_key_recovery_wrapped", null)
        set(value) = prefs.edit().putString("cloud_master_key_recovery_wrapped", value).apply()

    var cloudMasterKeyVersion: Int
        get() = prefs.getInt("cloud_master_key_version", 1)
        set(value) = prefs.edit().putInt("cloud_master_key_version", value).apply()

    var cloudSyncEnabled: Boolean
        get() = prefs.getBoolean("cloud_sync_enabled", false)
        set(value) = prefs.edit().putBoolean("cloud_sync_enabled", value).apply()

    var cloudSyncDeviceId: String
        get() {
            var id = prefs.getString("cloud_sync_device_id", null)
            if (id.isNullOrBlank()) {
                id = java.util.UUID.randomUUID().toString()
                prefs.edit().putString("cloud_sync_device_id", id).apply()
            }
            return id
        }
        set(value) = prefs.edit().putString("cloud_sync_device_id", value).apply()

    fun clearCloudKeys() {
        prefs.edit()
            .remove("cloud_master_key_device_wrapped")
            .remove("cloud_master_key_recovery_wrapped")
            .remove("cloud_master_key_version")
            .remove("cloud_sync_enabled")
            .apply()
    }

    companion object {
        const val CAPACITY_UNLIMITED = "UNLIMITED"
        val CAPACITY_OPTIONS = listOf("200", "500", "1000", "5000", "10000", "25000", "50000", CAPACITY_UNLIMITED)
    }
}
