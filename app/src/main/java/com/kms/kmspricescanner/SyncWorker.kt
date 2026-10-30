package com.kms.kmspricescanner

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyncWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    // هذه الدالة تعمل تلقائياً في مسار خلفي (Background Thread) آمن تماماً
    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                // 1. الاتصال بقاعدة البيانات
                val db = AppDatabase.getDatabase(context)
                val repository = ProductRepository(db.productDao())

                // 2. محاولة تحميل ملف الأسعار الجديد من السيرفر
                val isDownloaded = repository.downloadAndSaveCsv(context, Config.CSV_URL, "kmsprices.csv")

                if (isDownloaded) {
                    // 3. تحديث قاعدة البيانات بالأسعار الجديدة
                    repository.refreshDatabaseFromCsv(context, "kmsprices.csv")

                    // إرجاع "نجاح" لكي يعلم النظام أن المهمة تمت
                    Result.success()
                } else {
                    // إرجاع "إعادة محاولة" إذا فشل التحميل (مثلاً الإنترنت ضعيف)
                    Result.retry()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Result.retry()
            }
        }
    }
}