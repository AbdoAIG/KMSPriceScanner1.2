package com.kms.kmspricescanner

import android.content.Context
import android.os.SystemClock
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

object AdminSecurityManager {
    private const val PREFS_NAME = "KMS_Security_Prefs"
    private const val KEY_ATTEMPTS = "failed_attempts"
    private const val KEY_LOCK_TIME = "lockout_time"

    private const val MAX_ATTEMPTS = 3
    private const val LOCKOUT_DURATION_MS = 300000L // 5 دقائق

    // فحص القفل
    private fun isLockedOut(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lockTime = prefs.getLong(KEY_LOCK_TIME, 0)

        if (lockTime > 0) {
            val elapsedTime = SystemClock.elapsedRealtime() - lockTime
            if (elapsedTime < LOCKOUT_DURATION_MS) {
                val minutesLeft = ((LOCKOUT_DURATION_MS - elapsedTime) / 60000).toInt() + 1
                Toast.makeText(context, "النظام مقفول! انتظر $minutesLeft دقيقة", Toast.LENGTH_LONG).show()
                return true
            } else {
                resetAttempts(context)
            }
        }
        return false
    }

    private fun recordFailedAttempt(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var attempts = prefs.getInt(KEY_ATTEMPTS, 0)
        attempts++

        if (attempts >= MAX_ATTEMPTS) {
            prefs.edit()
                .putInt(KEY_ATTEMPTS, attempts)
                .putLong(KEY_LOCK_TIME, SystemClock.elapsedRealtime())
                .apply()
            Toast.makeText(context, "تم قفل النظام لمدة 5 دقائق بسبب تعدد المحاولات", Toast.LENGTH_LONG).show()
        } else {
            prefs.edit().putInt(KEY_ATTEMPTS, attempts).apply()
            Toast.makeText(context, "كلمة مرور خاطئة! تبقى ${MAX_ATTEMPTS - attempts} محاولات", Toast.LENGTH_SHORT).show()
        }
    }

    private fun resetAttempts(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    // ✅ الدالة المعمارية الجديدة: تعرض المربع، تفحص الأمان، وتنفذ الكود المطلوب فقط إذا نجح
    fun showAuthDialog(context: Context, onSuccess: () -> Unit) {
        if (isLockedOut(context)) return

        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_admin_password, null)

        val etPassword = dialogView.findViewById<TextInputEditText>(R.id.et_admin_password)
        val btnConfirm = dialogView.findViewById<MaterialButton>(R.id.btn_confirm_auth)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btn_cancel_auth)

        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        // شفافية الخلفية لكي تظهر الزوايا الدائرية للكارت
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnConfirm.setOnClickListener {
            val enteredText = etPassword.text.toString()
            if (enteredText.isEmpty()) {
                etPassword.error = "مطلوب"
                return@setOnClickListener
            }

            val enteredHash = HashHelper.sha256(enteredText)
            if (enteredHash == Config.ADMIN_PASSWORD_HASH) {
                resetAttempts(context)
                dialog.dismiss()
                onSuccess() // 🔥 تنفيذ الأكشن (مثل فتح شاشة التعديل)
            } else {
                recordFailedAttempt(context)
                etPassword.setText("")
            }
        }
        dialog.show()
    }
}