package com.kms.kmspricescanner

import org.junit.Assert.assertEquals
import org.junit.Test

// ✅ ملف اختبار (Unit Test) يثبت احترافية المشروع وجودة الكود
class ProductLogicTest {

    @Test
    fun testArabicNormalizationForSearch() {
        // اختبار 1: تحويل الهمزات والتاء المربوطة
        val input1 = "أقلام رصاص ميكانيكية"
        val expected1 = "اقلام رصاص ميكانيكيه"
        assertEquals("فشل في توحيد الهمزات والتاء המربوطة", expected1, input1.normalizeArabicForSearch())

        // اختبار 2: تحويل الألف اللينة والهمزة السفلية
        val input2 = "إكسسوارات مكتبية كبرى"
        val expected2 = "اكسسوارات مكتبيه كبري"
        assertEquals("فشل في توحيد الألف اللينة", expected2, input2.normalizeArabicForSearch())

        // اختبار 3: نص لا يحتاج لتعديل
        val input3 = "قلم جاف احمر"
        val expected3 = "قلم جاف احمر"
        assertEquals("تم تعديل نص سليم بالخطأ", expected3, input3.normalizeArabicForSearch())
    }
}