package com.kms.kmspricescanner

import android.content.Context
import android.os.SystemClock
import android.widget.Toast

object AdminSecurityManager {
    private const val PREFS_NAME = "KMS_Security_Prefs"
    private const val KEY_ATTEMPTS = "failed_attempts"
    private const val KEY_LOCK_TIME = "lockout_time"

    // Ø§Ù„Ø¥Ø¹Ø¯Ø§Ø¯Ø§Øª: 3 Ù…Ø­Ø§ÙˆÙ„Ø§Øª Ù Ù‚Ø·ØŒ ÙˆØ§Ù„Ù‚Ù Ù„ Ù„Ù…Ø¯Ø© 5 Ø¯Ù‚Ø§Ø¦Ù‚ (300,000 Ù…Ù„Ù„ÙŠ Ø«Ø§Ù†ÙŠØ©)
    private const val MAX_ATTEMPTS = 3
    private const val LOCKOUT_DURATION_MS = 300000L

    // âœ… 1. Ù Ø­Øµ Ù…Ø§ Ø¥Ø°Ø§ ÙƒØ§Ù†Øª Ø§Ù„Ø´Ø§Ø´Ø© Ù…Ù‚Ù Ù„Ø© Ø­Ø§Ù„ÙŠØ§Ù‹
    fun isLockedOut(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lockTime = prefs.getLong(KEY_LOCK_TIME, 0)

        if (lockTime > 0) {
            val elapsedTime = SystemClock.elapsedRealtime() - lockTime
            if (elapsedTime < LOCKOUT_DURATION_MS) {
                val minutesLeft = ((LOCKOUT_DURATION_MS - elapsedTime) / 60000).toInt() + 1
                Toast.makeText(context, "Ø§Ù„Ù†Ø¸Ø§Ù… Ù…Ù‚Ù ÙˆÙ„! Ø§Ù†ØªØ¸Ø± $minutesLeft Ø¯Ù‚ÙŠÙ‚Ø©", Toast.LENGTH_LONG).show()
                return true
            } else {
                // Ø§Ù†ØªÙ‡Øª Ù ØªØ±Ø© Ø§Ù„Ø¹Ù‚ÙˆØ¨Ø©ØŒ Ù†ØµÙ Ø± Ø§Ù„Ø¹Ø¯Ø§Ø¯Ø§Øª
                resetAttempts(context)
            }
        }
        return false
    }

    // âœ… 2. ØªØ³Ø¬ÙŠÙ„ Ù…Ø­Ø§ÙˆÙ„Ø© Ø®Ø§Ø·Ø¦Ø© (ÙˆÙ‚Ù Ù„ Ø§Ù„Ù†Ø¸Ø§Ù… Ø¥Ø°Ø§ ØªØ¬Ø§ÙˆØ² Ø§Ù„Ø­Ø¯)
    fun recordFailedAttempt(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var attempts = prefs.getInt(KEY_ATTEMPTS, 0)
        attempts++

        if (attempts >= MAX_ATTEMPTS) {
            prefs.edit()
                .putInt(KEY_ATTEMPTS, attempts)
                .putLong(KEY_LOCK_TIME, SystemClock.elapsedRealtime())
                .apply()
            Toast.makeText(context, "ØªÙ… Ù‚Ù Ù„ Ø§Ù„Ù†Ø¸Ø§Ù… Ù„Ù…Ø¯Ø© 5 Ø¯Ù‚Ø§Ø¦Ù‚ Ø¨Ø³Ø¨Ø¨ ØªØ¹Ø¯Ø¯ Ø§Ù„Ù…Ø­Ø§ÙˆÙ„Ø§Øª", Toast.LENGTH_LONG).show()
        } else {
            prefs.edit().putInt(KEY_ATTEMPTS, attempts).apply()
            Toast.makeText(context, "ÙƒÙ„Ù…Ø© Ù…Ø±ÙˆØ± Ø®Ø§Ø·Ø¦Ø©! ØªØ¨Ù‚Ù‰ ${MAX_ATTEMPTS - attempts} Ù…Ø­Ø§ÙˆÙ„Ø§Øª", Toast.LENGTH_SHORT).show()
        }
    }

    // âœ… 3. ØªØµÙ ÙŠØ± Ø§Ù„Ø¹Ø¯Ø§Ø¯ Ø¹Ù†Ø¯ Ø¥Ø¯Ø®Ø§Ù„ ÙƒÙ„Ù…Ø© Ù…Ø±ÙˆØ± ØµØ­ÙŠØ­Ø©
    fun resetAttempts(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}