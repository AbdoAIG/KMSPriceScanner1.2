package com.kms.kmspricescanner

import android.content.Context
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

class ProductRepository(private val productDao: ProductDao) {

    suspend fun getAllProducts(): List<ProductEntity> = productDao.getAllProducts()
    suspend fun searchProducts(query: String): List<ProductEntity> = productDao.searchProducts(query)
    suspend fun getProductsByDepartment(deptName: String): List<ProductEntity> = productDao.getProductsByDepartment(deptName)

    // ✅ دالة جديدة: تحميل ملف الـ CSV باستخدام Retrofit وحفظه في الذاكرة
    suspend fun downloadAndSaveCsv(context: Context, url: String, fileName: String): Boolean {
        return try {
            val response = RetrofitClient.apiService.downloadCsv(url)
            val fileOutputStream = context.openFileOutput(fileName, Context.MODE_PRIVATE)
            fileOutputStream.write(response.bytes())
            fileOutputStream.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // ✅ دوال الاتصال بالسيرفر للحصول على السمات (Theme) والبانرات وحالة السيرفر
    suspend fun fetchTheme(): String? {
        return try {
            RetrofitClient.apiService.postToGoogleScript(Config.APPS_SCRIPT_URL, mapOf("action" to "getTheme"))
        } catch (e: Exception) { null }
    }

    suspend fun fetchBanners(): String? {
        return try {
            RetrofitClient.apiService.postToGoogleScript(Config.APPS_SCRIPT_URL, mapOf("action" to "getBanners"))
        } catch (e: Exception) { null }
    }

    suspend fun checkServerStatus(url: String): String? {
        return try {
            RetrofitClient.apiService.checkServerStatus(url)
        } catch (e: Exception) { null }
    }

    // دالة تحديث قاعدة البيانات من الملف المحلي
    suspend fun refreshDatabaseFromCsv(context: Context, fileName: String) {
        val tempProductsList = ArrayList<ProductEntity>(70000)
        var fileContent: BufferedReader? = null

        try {
            val privateFile = File(context.filesDir, fileName)
            fileContent = if (privateFile.exists()) {
                BufferedReader(InputStreamReader(privateFile.inputStream(), StandardCharsets.UTF_8))
            } else {
                BufferedReader(InputStreamReader(context.assets.open(fileName), StandardCharsets.UTF_8))
            }

            var line: String?
            while (fileContent.readLine().also { line = it } != null) {
                val tokens = fastCsvSplit(line!!)
                if (tokens.size >= 8) {
                    tempProductsList.add(
                        ProductEntity(
                            localBarcode = tokens[0],
                            globalBarcode = tokens[1],
                            name = tokens[2],
                            pricePiece = tokens[3],
                            priceDozen = tokens[4],
                            priceCarton = tokens[5],
                            imageUrl = tokens[6],
                            department = tokens[7],
                            branch = if (tokens.size > 8) tokens[8] else "عام"
                        )
                    )
                }
            }

            if (tempProductsList.isNotEmpty()) {
                productDao.deleteAll()
                productDao.insertAll(tempProductsList)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            fileContent?.close()
        }
    }

    private fun fastCsvSplit(line: String): List<String> {
        val result = ArrayList<String>(10)
        var start = 0
        var inQuotes = false
        for (i in 0 until line.length) {
            val c = line[i]
            if (c == '\"') {
                inQuotes = !inQuotes
            } else if (c == ',' && !inQuotes) {
                result.add(line.substring(start, i).replace("\"", "").trim())
                start = i + 1
            }
        }
        result.add(line.substring(start).replace("\"", "").trim())
        return result
    }
}