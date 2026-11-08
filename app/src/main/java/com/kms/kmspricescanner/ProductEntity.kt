package com.kms.kmspricescanner

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "products_table",
    indices = [
        Index(value = ["name"]),
        Index(value = ["globalBarcode"]),
        Index(value = ["department"])
    ]
)
data class ProductEntity(
    @PrimaryKey
    val localBarcode: String,
    val globalBarcode: String,
    val name: String,
    val pricePiece: String,
    val priceDozen: String,
    val priceCarton: String,
    val imageUrl: String,
    val department: String,
    val branch: String,
    val stock: String, // تمت إضافة حقل المخزون هنا
    // ✅ العمود السري الجديد لتسريع البحث (بدون التأثير على الاسم الأصلي المعروض للمستخدم)
    val normalizedName: String = ""
)

// ✅ أداة سحرية لتوحيد الحروف العربية لتسريع البحث
fun String.normalizeArabicForSearch(): String {
    return this.replace("ة", "ه")
        .replace("أ", "ا")
        .replace("إ", "ا")
        .replace("آ", "ا")
        .replace("ى", "ي")
}