package com.kms.kmspricescanner

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // إعدادات احترافية تمنح الخادم 30 ثانية للرد قبل إعلان فشل الاتصال، بدلاً من التعليق اللانهائي
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // بناء محرك Retrofit باستخدام محول النصوص (Scalars) لأن Google Sheets تُرجع نصوصاً
    val apiService: KmsApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://script.google.com/") // الرابط الأساسي، سيتم تخطيه ديناميكياً عند استخدام @Url
            .client(okHttpClient)
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
            .create(KmsApiService::class.java)
    }
}