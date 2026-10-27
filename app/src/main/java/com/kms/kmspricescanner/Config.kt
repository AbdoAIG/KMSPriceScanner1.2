package com.kms.kmspricescanner

object Config {
    // 1. كلمة مرور المسؤول (للحماية)
    const val ADMIN_PASSWORD = "159955"

    // 2. رابط سكريبت جوجل (لإضافة المنتجات وجلب الصور والألوان)
    // ⚠️ تنبيه: استبدل النص التالي برابط السكريبت الخاص بك والذي يجب أن ينتهي بكلمة /exec
    const val APPS_SCRIPT_URL = "https://script.google.com/macros/s/AKfycbxK3N9QlJM0ex7mjaFsembrwNWN8KAtIu4zpEWLiXZHd0h77sLhv-cL5TmFYZXhqpYi/exec"

    // 3. رابط ملف الـ CSV (للقراءة السريعة وعرض الـ 64 ألف منتج)
    // هذا الرابط سليم وممتاز جداً للقراءة ولا تقم بتغييره
    const val CSV_URL = "https://docs.google.com/spreadsheets/d/e/2PACX-1vQGDqnKrEKr0HrJUM7QMGKtLhoePdkZVsBtNZ3mW4mhxiqKUGW3fm1J4qbb6F0AWp3q9F1-xfaxOD3-/pub?gid=0&single=true&output=csv"
}