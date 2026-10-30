package com.kms.kmspricescanner

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// ✅ تم تصحيح اسم الجدول ليتطابق مع (products_table) الموجود في ProductDao
@Entity(
    tableName = "products_table",
    indices = [
        Index(value = ["name"]),           // فهرس لتسريع البحث بالاسم
        Index(value = ["globalBarcode"]),  // فهرس لتسريع البحث بالباركود الدولي
        Index(value = ["department"])      // فهرس لتسريع التصفية بالأقسام
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
    val branch: String
)