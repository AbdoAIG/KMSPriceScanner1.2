package com.kms.kmspricescanner

import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

class ProductRepository(private val productDao: ProductDao) {
    // ✅ علامة مسجلة لتتبع الأخطاء في نظام أندرويد (Logcat)
    private val TAG = "KMS_Repository"

    suspend fun getAllProducts(): List<ProductEntity> = productDao.getAllProducts()
    suspend fun searchProducts(query: String): List<ProductEntity> = productDao.searchProducts(query)
    suspend fun getProductsByDepartment(deptName: String): List<ProductEntity> = productDao.getProductsByDepartment(deptName)

    suspend fun downloadAndSaveCsv(context: Context, url: String, fileName: String): Boolean {
        return try {
            val response = RetrofitClient.apiService.downloadCsv(url)
            val file = File(context.filesDir, fileName)

            response.byteStream().use { inputStream ->
                file.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            true
        } catch (e: IOException) {
            // ✅ تصنيف الخطأ: مشكلة في الشبكة أو انقطاع الإنترنت
            Log.e(TAG, "❌ Network Error: Failed to download CSV. Please check internet connection.", e)
            false
        } catch (e: Exception) {
            // ✅ تصنيف الخطأ: خطأ غير متوقع
            Log.e(TAG, "❌ Unknown Error: Something went wrong while downloading CSV.", e)
            false
        }
    }

    suspend fun fetchTheme(): String? {
        return try {
            val params = mapOf("action" to "getTheme", "token" to Config.SECRET_API_TOKEN)
            RetrofitClient.apiService.postToGoogleScript(Config.APPS_SCRIPT_URL, params)
        } catch (e: Exception) {
            Log.e(TAG, "❌ API Error: Failed to fetch theme.", e)
            null
        }
    }

    suspend fun fetchBanners(): String? {
        return try {
            val params = mapOf("action" to "getBanners", "token" to Config.SECRET_API_TOKEN)
            RetrofitClient.apiService.postToGoogleScript(Config.APPS_SCRIPT_URL, params)
        } catch (e: Exception) {
            Log.e(TAG, "❌ API Error: Failed to fetch banners.", e)
            null
        }
    }

    suspend fun checkServerStatus(url: String): String? {
        return try {
            val secureUrl = if (url.contains("?")) "$url&token=${Config.SECRET_API_TOKEN}" else "$url?token=${Config.SECRET_API_TOKEN}"
            RetrofitClient.apiService.checkServerStatus(secureUrl)
        } catch (e: Exception) {
            Log.e(TAG, "❌ API Error: Failed to check server status.", e)
            null
        }
    }

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
                    val productName = tokens[2]
                    tempProductsList.add(
                        ProductEntity(
                            localBarcode = tokens[0],
                            globalBarcode = tokens[1],
                            name = productName,
                            pricePiece = tokens[3],
                            priceDozen = tokens[4],
                            priceCarton = tokens[5],
                            imageUrl = tokens[6],
                            department = tokens[7],
                            branch = if (tokens.size > 8) tokens[8] else "عام",
                            stock = if (tokens.size > 9 && tokens[9].isNotBlank()) tokens[9] else "0",
                            normalizedName = productName.normalizeArabicForSearch()
                        )
                    )
                }
            }

            if (tempProductsList.isNotEmpty()) {
                productDao.replaceAllData(tempProductsList)
            }
        } catch (e: IOException) {
            Log.e(TAG, "❌ File Error: Could not read CSV file from storage.", e)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Parsing Error: Failed to parse CSV data. Data format might be corrupted.", e)
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

    suspend fun getProductsForHistory(barcodes: List<String>): List<ProductEntity> {
        if (barcodes.isEmpty()) return emptyList()
        return productDao.getProductsByBarcodes(barcodes)
    }
}