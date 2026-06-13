package com.kms.kmspricescanner

object Config {
    // 1. كلمة مرور المسؤول (للحماية)
    const val ADMIN_PASSWORD = "159955" // غيرها كما تحب

    // 2. رابط سكريبت جوجل (Google Apps Script Web App URL)
    // هذا الرابط الذي حصلت عليه بعد عمل Deploy للسكربت

    // 3. رابط ملف الـ CSV (الذي كان يسبب الخطأ)
    // استبدل هذا الرابط برابط الشيت الخاص بك (تأكد أنه ينتهي بـ output=csv)
    const val CSV_URL = "https://docs.google.com/spreadsheets/d/e/2PACX-1vQGDqnKrEKr0HrJUM7QMGKtLhoePdkZVsBtNZ3mW4mhxiqKUGW3fm1J4qbb6F0AWp3q9F1-xfaxOD3-/pub?gid=0&single=true&output=csv"
}