package com.kms.kmspricescanner

import okhttp3.ResponseBody
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Url

interface KmsApiService {

    // 1. تحميل ملف الـ CSV كـ Stream لحماية الذاكرة العشوائية (RAM)
    @GET
    suspend fun downloadCsv(@Url url: String): ResponseBody

    // 2. فحص حالة السيرفر (تستقبل JSON كنص)
    @GET
    suspend fun checkServerStatus(@Url url: String): String

    // 3. أداة شاملة (Dynamic POST) لإرسال البيانات إلى Google Apps Script
    // (تُستخدم لجلب البانرات، الثيم، أو إضافة منتج جديد من الشاشة الأخرى)
    @FormUrlEncoded
    @POST
    suspend fun postToGoogleScript(
        @Url url: String,
        @FieldMap params: Map<String, String>
    ): String
}