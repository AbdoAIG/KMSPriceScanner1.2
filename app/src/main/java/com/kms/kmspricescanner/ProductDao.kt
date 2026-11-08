package com.kms.kmspricescanner

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface ProductDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<ProductEntity>)

    @Query("DELETE FROM products_table")
    suspend fun deleteAll()

    @Query("SELECT * FROM products_table")
    suspend fun getAllProducts(): List<ProductEntity>

    // ✅ محرك بحث صاروخي: يبحث في العمود الجاهز (normalizedName) مباشرة
    @Query("""
        SELECT * FROM products_table 
        WHERE normalizedName LIKE '%' || :searchQuery || '%' 
        OR localBarcode LIKE '%' || :searchQuery || '%' 
        OR globalBarcode LIKE '%' || :searchQuery || '%'
        OR (localBarcode != '' AND LENGTH(localBarcode) >= 3 AND :searchQuery LIKE '%' || localBarcode || '%')
        OR (globalBarcode != '' AND LENGTH(globalBarcode) >= 3 AND :searchQuery LIKE '%' || globalBarcode || '%')
    """)
    suspend fun searchProducts(searchQuery: String): List<ProductEntity>

    @Query("SELECT * FROM products_table WHERE department = :deptName")
    suspend fun getProductsByDepartment(deptName: String): List<ProductEntity>

    // دالة التحديث الآمنة
    @Transaction
    suspend fun replaceAllData(newProducts: List<ProductEntity>) {
        deleteAll()
        insertAll(newProducts)
    }

    @Query("SELECT * FROM products_table WHERE localBarcode IN (:barcodes) OR globalBarcode IN (:barcodes)")
    suspend fun getProductsByBarcodes(barcodes: List<String>): List<ProductEntity>
}