package com.kms.kmspricescanner

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ProductDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<ProductEntity>)

    @Query("DELETE FROM products_table")
    suspend fun deleteAll()

    @Query("SELECT * FROM products_table")
    suspend fun getAllProducts(): List<ProductEntity>

    @Query("""
        SELECT * FROM products_table 
        WHERE REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(name, 'ة', 'ه'), 'أ', 'ا'), 'إ', 'ا'), 'آ', 'ا'), 'ى', 'ي') LIKE '%' || :searchQuery || '%' 
        OR localBarcode LIKE '%' || :searchQuery || '%' 
        OR globalBarcode LIKE '%' || :searchQuery || '%'
        OR (localBarcode != '' AND LENGTH(localBarcode) >= 3 AND :searchQuery LIKE '%' || localBarcode || '%')
        OR (globalBarcode != '' AND LENGTH(globalBarcode) >= 3 AND :searchQuery LIKE '%' || globalBarcode || '%')
    """)
    suspend fun searchProducts(searchQuery: String): List<ProductEntity>

    @Query("SELECT * FROM products_table WHERE department = :deptName")
    suspend fun getProductsByDepartment(deptName: String): List<ProductEntity>
}