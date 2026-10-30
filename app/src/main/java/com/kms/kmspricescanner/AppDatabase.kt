package com.kms.kmspricescanner

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// ✅ التحديث الهندسي: رفعنا الإصدار (version) من 1 إلى 2 بسبب إضافة الفهارس
@Database(entities = [ProductEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "kms_database"
                )
                    // 🛡️ حماية ضد الانهيار: إذا اختلف إصدار قاعدة البيانات، سيقوم بمسح الكاش القديم وبنائه من جديد
                    // (هذا آمن جداً لأننا نجلب بياناتنا من ملف CSV على أي حال)
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}