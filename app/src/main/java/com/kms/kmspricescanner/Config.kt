package com.kms.kmspricescanner

object Config {
    // 1. كلمة مرور المسؤول (محمية بتشفير SHA-256 أحادي الاتجاه)
    // هذه الشفرة تقابل الرقم السري الخاص بك، ويستحيل كسرها أو عكسها
    const val ADMIN_PASSWORD_HASH = "c374653c538d732f40320ff74b256c9b6f37f132c5ffa13628f76024078a3e48"

    // 2. رابط سكريبت جوجل (لإضافة المنتجات وجلب الصور والألوان)
    const val APPS_SCRIPT_URL = "https://script.google.com/macros/s/AKfycbwkdn_O3KfXTQwyirzUCy6jXjQvG6x6jtyQ33_pfQTuI2G0uWog7pBpSCKIklCEQy68/exec"

    // 3. رابط ملف الـ CSV (للقراءة السريعة وعرض الـ 64 ألف منتج)
    const val CSV_URL = "https://docs.google.com/spreadsheets/d/e/2PACX-1vQGDqnKrEKr0HrJUM7QMGKtLhoePdkZVsBtNZ3mW4mhxiqKUGW3fm1J4qbb6F0AWp3q9F1-xfaxOD3-/pub?gid=0&single=true&output=csv"
}