package com.kms.kmspricescanner

import android.content.Context

object ScanHistoryHelper {
    private const val PREF_NAME = "KMS_Scan_History"
    private const val KEY_HISTORY = "last_scanned_barcodes"
    private const val MAX_HISTORY_SIZE = 20 // عدد المنتجات المحفوظة

    // حفظ منتج في السجل
    fun saveProduct(context: Context, barcode: String) {
        if (barcode.isEmpty()) return

        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val savedString = prefs.getString(KEY_HISTORY, "") ?: ""

        // تحويل النص إلى قائمة قابلة للتعديل
        val historyList = savedString.split(",").filter { it.isNotEmpty() }.toMutableList()

        // حذف الباركود إذا كان موجوداً مسبقاً (لإعادته للأعلى)
        historyList.removeAll { it == barcode }

        // إضافة الباركود في بداية القائمة
        historyList.add(0, barcode)

        // حذف القديم إذا تجاوزنا الحد المسموح
        if (historyList.size > MAX_HISTORY_SIZE) {
            historyList.removeAt(historyList.lastIndex)
        }

        // الحفظ مرة أخرى
        prefs.edit().putString(KEY_HISTORY, historyList.joinToString(",")).apply()
    }

    // استرجاع السجل
    fun getHistory(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val savedString = prefs.getString(KEY_HISTORY, "") ?: ""
        return savedString.split(",").filter { it.isNotEmpty() }
    }
}