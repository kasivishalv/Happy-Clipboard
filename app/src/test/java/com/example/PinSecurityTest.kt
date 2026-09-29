package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.core.security.CryptoHelper
import com.example.core.security.SecurityPreferences
import com.example.data.repository.ClipboardRepository
import com.example.ui.MainViewModel
import com.example.ui.components.AppLockOverlay
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [34])
class PinSecurityTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var preferences: SecurityPreferences

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val prefs = context.getSharedPreferences("happy_clipboard_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        preferences = SecurityPreferences(context)
    }

    @Test
    fun testFreshInstall_NoPinExistsInitially_DefaultPinCannotUnlock() {
        // 1. Fresh installation: No PIN exists initially
        assertFalse("PIN should not be set on fresh install", preferences.isPinSet)
        assertEquals("appLockPinHash should be empty initially", "", preferences.appLockPinHash)

        // 2. Default PIN 1234 or any other arbitrary PIN must NEVER authenticate
        assertFalse("Default PIN '1234' must never authenticate", preferences.verifyPin("1234"))
        assertFalse("Arbitrary PIN '0000' must never authenticate", preferences.verifyPin("0000"))
        assertFalse("Arbitrary PIN '9999' must never authenticate", preferences.verifyPin("9999"))
    }

    @Test
    fun testPinSetup_WrongConfirmationRejects_CorrectConfirmationSavesSaltedHash() {
        val app = ApplicationProvider.getApplicationContext<HappyClipboardApp>()
        val viewModel = MainViewModel(app.repository, preferences)

        // Attempting to set PIN with mismatching confirmation must fail
        val mismatchSuccess = viewModel.setAppPin("4826", "9999")
        assertFalse("Wrong confirmation must be rejected", mismatchSuccess)
        assertFalse("PIN must not be set after failed confirmation", preferences.isPinSet)

        // Setting PIN with matching confirmation succeeds
        val setupSuccess = viewModel.setAppPin("4826", "4826")
        assertTrue("Matching PIN setup must succeed", setupSuccess)
        assertTrue("PIN must be set after successful confirmation", preferences.isPinSet)

        // Credential must be stored as salted PBKDF2 hash, NEVER plaintext
        val storedHash = preferences.appLockPinHash
        assertTrue("Stored hash must start with PIN_V1:", storedHash.startsWith("PIN_V1:"))
        assertNotEquals("PIN must NEVER be stored in plaintext", "4826", storedHash)

        // The configured PIN authenticates; default 1234 fails
        assertTrue("Configured PIN must authenticate", preferences.verifyPin("4826"))
        assertFalse("Default PIN '1234' must NOT authenticate", preferences.verifyPin("1234"))
    }

    @Test
    fun testPinAuthentication_CorrectPinUnlocks_WrongPinRemainsLocked() {
        val app = ApplicationProvider.getApplicationContext<HappyClipboardApp>()
        val viewModel = MainViewModel(app.repository, preferences)

        viewModel.setAppPin("7391", "7391")
        viewModel.lockApp()
        assertTrue("App should be locked", viewModel.isAppLocked)

        // Attempt wrong PIN
        val wrongPinUnlock = viewModel.unlockApp("0000")
        assertFalse("Wrong PIN must fail to unlock", wrongPinUnlock)
        assertTrue("App must remain locked after wrong PIN", viewModel.isAppLocked)

        // Attempt default PIN "1234"
        val defaultPinUnlock = viewModel.unlockApp("1234")
        assertFalse("Default PIN '1234' must fail to unlock", defaultPinUnlock)
        assertTrue("App must remain locked after default PIN attempt", viewModel.isAppLocked)

        // Attempt correct PIN
        val correctPinUnlock = viewModel.unlockApp("7391")
        assertTrue("Correct PIN must unlock the app", correctPinUnlock)
        assertFalse("App must be unlocked after correct PIN", viewModel.isAppLocked)
    }

    @Test
    fun testAppRestart_ConfiguredPinRemains() {
        // Configure PIN
        preferences.setPin("6152")
        preferences.isAppLockEnabled = true
        assertTrue("PIN should be set", preferences.isPinSet)

        // Simulate app restart by instantiating new SecurityPreferences from the same Context
        val reloadedPreferences = SecurityPreferences(context)
        assertTrue("PIN must remain set after app restart", reloadedPreferences.isPinSet)
        assertTrue("Configured PIN must unlock after restart", reloadedPreferences.verifyPin("6152"))
        assertFalse("Default PIN '1234' must not unlock after restart", reloadedPreferences.verifyPin("1234"))
        assertFalse("Wrong PIN must not unlock after restart", reloadedPreferences.verifyPin("1111"))
    }

    @Test
    fun testChangingPin_OldPinStopsWorking_NewPinWorks() {
        val app = ApplicationProvider.getApplicationContext<HappyClipboardApp>()
        val viewModel = MainViewModel(app.repository, preferences)

        // Initially configure PIN to "1111"
        viewModel.setAppPin("1111", "1111")
        assertTrue(preferences.verifyPin("1111"))

        // Attempt change with incorrect current PIN
        val wrongCurrentSuccess = viewModel.changePin("9999", "2222", "2222")
        assertFalse("Change with wrong current PIN must fail", wrongCurrentSuccess)
        assertTrue("Original PIN must still be active", preferences.verifyPin("1111"))

        // Attempt change with mismatching new confirmation
        val mismatchSuccess = viewModel.changePin("1111", "2222", "3333")
        assertFalse("Change with mismatching confirmation must fail", mismatchSuccess)
        assertTrue("Original PIN must still be active", preferences.verifyPin("1111"))

        // Valid change to "2222"
        val validChangeSuccess = viewModel.changePin("1111", "2222", "2222")
        assertTrue("Valid PIN change must succeed", validChangeSuccess)

        // Verify: Old PIN stops working!
        assertFalse("Old PIN '1111' must no longer work", preferences.verifyPin("1111"))
        assertFalse("Default PIN '1234' must not work", preferences.verifyPin("1234"))

        // Verify: New PIN works!
        assertTrue("New PIN '2222' must unlock", preferences.verifyPin("2222"))
    }

    @Test
    fun testLegacyDefaultPin_PurgedOnFirstAccess() {
        // Simulate a legacy install that had the old hardcoded "1234" in SharedPreferences
        val prefs = context.getSharedPreferences("happy_clipboard_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("app_lock_pin", "1234").commit()

        val secPrefs = SecurityPreferences(context)
        // Accessing isPinSet must purge legacy "1234" and report no PIN set
        assertFalse("Legacy default 1234 must NOT be treated as a valid PIN", secPrefs.isPinSet)
        assertFalse("Legacy default 1234 must not unlock", secPrefs.verifyPin("1234"))
        assertFalse("Legacy key should be removed from SharedPreferences", prefs.contains("app_lock_pin"))
    }

    @Test
    fun testAppLockOverlayUI_SetupAndUnlockFlow_NoDefaultPinInUI() {
        var unlockedPin: String? = null
        var setPinCandidate: Pair<String, String>? = null

        // 1. UI in Setup Mode (!isPinSet)
        composeTestRule.setContent {
            MyApplicationTheme {
                AppLockOverlay(
                    isPinSet = false,
                    onUnlock = { pin ->
                        unlockedPin = pin
                        true
                    },
                    onSetPin = { pin, confirm ->
                        setPinCandidate = Pair(pin, confirm)
                        true
                    }
                )
            }
        }

        // Verify UI starts in "Create a 4-digit security PIN"
        composeTestRule.onNodeWithTag("pin_subtitle").assertTextContains("Create a 4-digit security PIN")

        // Verify "Default is 1234" and "Use Default PIN" do NOT exist anywhere in UI
        composeTestRule.onNodeWithText("Default is 1234").assertDoesNotExist()
        composeTestRule.onNodeWithText("Use Default PIN (1234)").assertDoesNotExist()
        composeTestRule.onNodeWithText("Use Default PIN").assertDoesNotExist()

        // Enter 4 digits: 1, 2, 3, 4
        composeTestRule.onNodeWithTag("keypad_1").performClick()
        composeTestRule.onNodeWithTag("keypad_2").performClick()
        composeTestRule.onNodeWithTag("keypad_3").performClick()
        composeTestRule.onNodeWithTag("keypad_4").performClick()
        composeTestRule.waitForIdle()

        // Subtitle transitions to "Confirm your 4-digit PIN"
        composeTestRule.onNodeWithTag("pin_subtitle").assertTextContains("Confirm your 4-digit PIN")

        // Enter mismatching confirm digits: 9, 9, 9, 9
        composeTestRule.onNodeWithTag("keypad_9").performClick()
        composeTestRule.onNodeWithTag("keypad_9").performClick()
        composeTestRule.onNodeWithTag("keypad_9").performClick()
        composeTestRule.onNodeWithTag("keypad_9").performClick()
        composeTestRule.waitForIdle()

        // Error message displayed and resets back to setup
        composeTestRule.onNodeWithTag("pin_error_message").assertIsDisplayed()
        composeTestRule.onNodeWithTag("pin_error_message").assertTextContains("PINs do not match. Try again.")
        composeTestRule.onNodeWithTag("pin_subtitle").assertTextContains("Create a 4-digit security PIN")

        // Now enter matching PIN setup: 3, 4, 5, 6
        composeTestRule.onNodeWithTag("keypad_3").performClick()
        composeTestRule.onNodeWithTag("keypad_4").performClick()
        composeTestRule.onNodeWithTag("keypad_5").performClick()
        composeTestRule.onNodeWithTag("keypad_6").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("pin_subtitle").assertTextContains("Confirm your 4-digit PIN")

        composeTestRule.onNodeWithTag("keypad_3").performClick()
        composeTestRule.onNodeWithTag("keypad_4").performClick()
        composeTestRule.onNodeWithTag("keypad_5").performClick()
        composeTestRule.onNodeWithTag("keypad_6").performClick()
        composeTestRule.waitForIdle()

        // Verify onSetPin was invoked with 3456 and 3456
        assertEquals(Pair("3456", "3456"), setPinCandidate)
    }

    @Test
    fun testAppLockOverlayUI_UnlockMode_IncorrectPinShowsErrorWithoutDefaultHint() {
        var unlockAttempt: String? = null

        composeTestRule.setContent {
            MyApplicationTheme {
                AppLockOverlay(
                    isPinSet = true,
                    onUnlock = { pin ->
                        unlockAttempt = pin
                        pin == "9876" // only 9876 unlocks
                    }
                )
            }
        }

        // Verify UI starts in "Enter your 4-digit security PIN"
        composeTestRule.onNodeWithTag("pin_subtitle").assertTextContains("Enter your 4-digit security PIN")

        // Enter wrong PIN: 1, 2, 3, 4
        composeTestRule.onNodeWithTag("keypad_1").performClick()
        composeTestRule.onNodeWithTag("keypad_2").performClick()
        composeTestRule.onNodeWithTag("keypad_3").performClick()
        composeTestRule.onNodeWithTag("keypad_4").performClick()
        composeTestRule.waitForIdle()

        assertEquals("1234", unlockAttempt)
        // Error message must say strictly "Incorrect PIN" - NEVER "Default is 1234"
        composeTestRule.onNodeWithTag("pin_error_message").assertIsDisplayed()
        composeTestRule.onNodeWithTag("pin_error_message").assertTextContains("Incorrect PIN")
        composeTestRule.onNodeWithText("Default is 1234").assertDoesNotExist()
        composeTestRule.onNodeWithText("Use Default PIN (1234)").assertDoesNotExist()
    }
}
