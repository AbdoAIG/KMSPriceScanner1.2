package com.kms.kmspricescanner

object Config {
    // 1. كلمة مرور المسؤول (محمية بتشفير SHA-256 أحادي الاتجاه)
    const val ADMIN_PASSWORD_HASH = "c374653c538d732f40320ff74b256c9b6f37f132c5ffa13628f76024078a3e48"

    // 2. رابط سكربت جوجل والمفتاح السري (مخفيان باحترافية داخل BuildConfig)
    val APPS_SCRIPT_URL = BuildConfig.APPS_SCRIPT_URL
    val SECRET_API_TOKEN = BuildConfig.SECRET_API_TOKEN

    // 3. رابط ملف الـ CSV
    const val CSV_URL = "https://docs.google.com/spreadsheets/d/e/2PACX-1vQGDqnKrEKr0HrJUM7QMGKtLhoePdkZVsBtNZ3mW4mhxiqKUGW3fm1J4qbb6F0AWp3q9F1-xfaxOD3-/pub?gid=0&single=true&output=csv"
}