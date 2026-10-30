package com.kms.kmspricescanner

import java.security.MessageDigest

object HashHelper {
    // دالة التشفير أحادي الاتجاه SHA-256
    fun sha256(input: String): String {
        val bytes = input.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        // تحويل النتيجة إلى نظام سداسي عشري (Hexadecimal)
        return digest.joinToString("") { "%02x".format(it) }
    }
}