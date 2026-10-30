package com.kms.kmspricescanner

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ProductDao {

    // إضافة المنتجات الجديدة دفعة واحدة، وإذا كان هناك تكرار يقوم بتحديثه
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<ProductEntity>)

    // حذف كل البيانات القديمة قبل وضع التحديث الجديد
    @Query("DELETE FROM products_table")
    suspend fun deleteAll()

    // جلب كل المنتجات
    @Query("SELECT * FROM products_table")
    suspend fun getAllProducts(): List<ProductEntity>

    // محرك البحث الفائق السرعة: يبحث في الباركود والاسم متجاهلاً حالة الأحرف
    @Query("""
        SELECT * FROM products_table 
        WHERE name LIKE '%' || :searchQuery || '%' 
        OR localBarcode LIKE '%' || :searchQuery || '%' 
        OR globalBarcode LIKE '%' || :searchQuery || '%'
    """)
    suspend fun searchProducts(searchQuery: String): List<ProductEntity>

    // فلترة الأقسام
    @Query("SELECT * FROM products_table WHERE department = :deptName")
    suspend fun getProductsByDepartment(deptName: String): List<ProductEntity>
}