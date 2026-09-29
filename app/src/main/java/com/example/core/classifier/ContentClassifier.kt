package com.example.core.classifier

import com.example.data.model.ContentType
import java.security.MessageDigest
import java.util.regex.Pattern

data class ClassificationResult(
    val contentType: ContentType,
    val isSensitive: Boolean,
    val expiresAt: Long? = null
)

object ContentClassifier {

    private val URL_REGEX = Pattern.compile(
        "^(https?://|www\\.)[a-zA-Z0-9+&@#/%?=~_|!:,.;\\-]*[a-zA-Z0-9+&@#/%=~_|]",
        Pattern.CASE_INSENSITIVE
    )

    private val EMAIL_REGEX = Pattern.compile(
        "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    )

    private val PHONE_REGEX = Pattern.compile(
        "^(\\+?\\d{1,4}[ -]?)?(\\(?\\d{2,4}\\)?[ -]?)?\\d{3,4}[ -]?\\d{3,4}$"
    )

    private val HEX_COLOR_REGEX = Pattern.compile(
        "^#(?:[0-9a-fA-F]{3}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8})$"
    )

    private val RGB_COLOR_REGEX = Pattern.compile(
        "^rgba?\\(\\s*\\d{1,3}\\s*,\\s*\\d{1,3}\\s*,\\s*\\d{1,3}(?:\\s*,\\s*(?:0|1|0?\\.\\d+))?\\s*\\)$",
        Pattern.CASE_INSENSITIVE
    )

    private val NUMBER_REGEX = Pattern.compile(
        "^-?\\d+(\\.\\d+)?$"
    )

    // OTP detection: standalone 4 to 8 digit numbers, or containing OTP/verification code
    private val OTP_STANDALONE_REGEX = Pattern.compile("^\\d{4,8}$")
    private val OTP_TEXT_REGEX = Pattern.compile(
        "(?:otp|verification code|security code|verify code|code is|pin is)[^\\d]{0,10}(\\d{4,8})",
        Pattern.CASE_INSENSITIVE
    )

    // Sensitive patterns: API keys, tokens, JWT, credit cards, private keys
    private val API_KEY_REGEX = Pattern.compile(
        "(?:sk_live_[0-9a-zA-Z]{24,}|AIzaSy[0-9a-zA-Z_-]{33}|ghp_[0-9a-zA-Z]{36}|Bearer\\s+[A-Za-z0-9\\-_=.]+|eyJ[A-Za-z0-9-_]+\\.eyJ[A-Za-z0-9-_]+\\.[A-Za-z0-9-_]+)",
        Pattern.CASE_INSENSITIVE
    )

    private val CREDIT_CARD_REGEX = Pattern.compile(
        "\\b(?:4[0-9]{12}(?:[0-9]{3})?|5[1-5][0-9]{14}|3[47][0-9]{13}|3(?:0[0-5]|[68][0-9])[0-9]{11}|6(?:011|5[0-9]{2})[0-9]{12}|(?:2131|1800|35\\d{3})\\d{11})\\b"
    )

    private val PRIVATE_KEY_REGEX = Pattern.compile(
        "-----BEGIN (?:RSA )?PRIVATE KEY-----",
        Pattern.CASE_INSENSITIVE
    )

    // Code & Commands indicators
    private val CODE_PREFIXES = listOf(
        "docker ", "docker-compose ", "kubectl ", "git ", "npm ", "npx ", "yarn ", "pnpm ",
        "curl ", "wget ", "sudo ", "systemctl ", "apt ", "brew ", "pip ", "python ", "cargo ",
        "go run ", "ssh ", "tar -", "grep ", "cd ", "mkdir ", "chmod ", "chown "
    )

    private val SQL_KEYWORDS = listOf(
        "SELECT ", "INSERT INTO ", "UPDATE ", "DELETE FROM ", "CREATE TABLE ", "ALTER TABLE ",
        "DROP TABLE ", "JOIN ", "WHERE ", "GROUP BY ", "ORDER BY "
    )

    private val CODE_SYNTAX_KEYWORDS = listOf(
        "fun ", "val ", "var ", "const ", "class ", "def ", "import ", "public static void",
        "interface ", "namespace ", "struct ", "fn ", "let mut ", "async fn ", "#include "
    )

    fun classify(text: String, otpAutoDeleteMinutes: Int = 5): ClassificationResult {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            return ClassificationResult(ContentType.TEXT, false)
        }

        // 1. Check for Sensitive Content first
        val isOtpStandalone = OTP_STANDALONE_REGEX.matcher(trimmed).matches()
        val isOtpText = OTP_TEXT_REGEX.matcher(trimmed).find()
        if (isOtpStandalone || isOtpText) {
            val expiry = if (otpAutoDeleteMinutes > 0) {
                System.currentTimeMillis() + (otpAutoDeleteMinutes * 60 * 1000L)
            } else null
            return ClassificationResult(
                contentType = ContentType.SENSITIVE,
                isSensitive = true,
                expiresAt = expiry
            )
        }

        if (API_KEY_REGEX.matcher(trimmed).find() ||
            CREDIT_CARD_REGEX.matcher(trimmed).find() ||
            PRIVATE_KEY_REGEX.matcher(trimmed).find()
        ) {
            return ClassificationResult(ContentType.SENSITIVE, isSensitive = true)
        }

        // 2. Color codes
        if (HEX_COLOR_REGEX.matcher(trimmed).matches() || RGB_COLOR_REGEX.matcher(trimmed).matches()) {
            return ClassificationResult(ContentType.COLOR, isSensitive = false)
        }

        // 3. URLs
        if (URL_REGEX.matcher(trimmed).matches()) {
            return ClassificationResult(ContentType.URL, isSensitive = false)
        }

        // 4. Emails
        if (EMAIL_REGEX.matcher(trimmed).matches()) {
            return ClassificationResult(ContentType.EMAIL, isSensitive = false)
        }

        // 5. Phone numbers
        if (PHONE_REGEX.matcher(trimmed).matches() && trimmed.length >= 7 && trimmed.count { it.isDigit() } >= 7) {
            return ClassificationResult(ContentType.PHONE, isSensitive = false)
        }

        // 6. JSON Objects or Arrays
        if ((trimmed.startsWith("{") && trimmed.endsWith("}")) ||
            (trimmed.startsWith("[") && trimmed.endsWith("]"))
        ) {
            if (trimmed.length > 3 && (trimmed.contains(":") || trimmed.contains(","))) {
                return ClassificationResult(ContentType.CODE, isSensitive = false)
            }
        }

        // 7. Command line / Code snippets
        val lower = trimmed.lowercase()
        val isCli = CODE_PREFIXES.any { lower.startsWith(it) || lower.contains("\n$it") }
        val isSql = SQL_KEYWORDS.any { trimmed.contains(it, ignoreCase = true) }
        val isCode = CODE_SYNTAX_KEYWORDS.any { trimmed.contains(it) }

        if (isCli || isSql || isCode) {
            return ClassificationResult(ContentType.CODE, isSensitive = false)
        }

        // 8. Plain numbers
        if (NUMBER_REGEX.matcher(trimmed).matches()) {
            return ClassificationResult(ContentType.NUMBER, isSensitive = false)
        }

        // 9. Default to TEXT
        return ClassificationResult(ContentType.TEXT, isSensitive = false)
    }

    fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
