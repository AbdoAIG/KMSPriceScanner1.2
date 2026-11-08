package com.kms.kmspricescanner

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object CartManager {

    // جعلنا القائمة المعروضة List عادية (غير قابلة للتعديل من الخارج) لمزيد من الأمان المعماري
    private val _cartItems = MutableLiveData<List<CartItem>>(emptyList())
    val cartItems: LiveData<List<CartItem>> = _cartItems

    private var sharedPreferences: SharedPreferences? = null
    private val gson = Gson()
    private const val PREF_NAME = "KMS_Cart_Prefs"
    private const val CART_KEY = "saved_cart_items"

    fun init(context: Context) {
        if (sharedPreferences == null) {
            sharedPreferences = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            loadCart()
        }
    }

    private fun saveCart(list: List<CartItem>) {
        val jsonString = gson.toJson(list)
        sharedPreferences?.edit()?.putString(CART_KEY, jsonString)?.apply()
    }

    private fun loadCart() {
        val jsonString = sharedPreferences?.getString(CART_KEY, null)
        if (!jsonString.isNullOrEmpty()) {
            try {
                val type = object : TypeToken<List<CartItem>>() {}.type
                val savedList: List<CartItem> = gson.fromJson(jsonString, type)
                _cartItems.value = savedList // استخدام value بدلاً من postValue للتحديث الفوري
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // 1. إضافة منتج للسلة
    fun addItem(product: ProductEntity, quantity: Int) {
        // ✅ السحر هنا: نأخذ نسخة جديدة تماماً باستخدام toMutableList()
        val updatedList = _cartItems.value?.toMutableList() ?: mutableListOf()
        val existingItem = updatedList.find { it.product.localBarcode == product.localBarcode }

        if (existingItem != null) {
            // نأخذ نسخة من العنصر نفسه لتغيير الكمية (لكي تلاحظ الواجهة التغيير)
            val index = updatedList.indexOf(existingItem)
            updatedList[index] = existingItem.copy(quantity = existingItem.quantity + quantity)
        } else {
            updatedList.add(CartItem(product, quantity))
        }

        // تحديث الواجهة بالنسخة الجديدة تماماً
        _cartItems.value = updatedList
        saveCart(updatedList)
    }

    // 2. تحديث كمية منتج موجود
    fun updateQuantity(product: ProductEntity, newQuantity: Int) {
        val updatedList = _cartItems.value?.toMutableList() ?: mutableListOf()
        val existingItem = updatedList.find { it.product.localBarcode == product.localBarcode }

        if (existingItem != null) {
            if (newQuantity > 0) {
                val index = updatedList.indexOf(existingItem)
                updatedList[index] = existingItem.copy(quantity = newQuantity)
            } else {
                updatedList.remove(existingItem)
            }
            _cartItems.value = updatedList
            saveCart(updatedList)
        }
    }

    // 3. حذف منتج من السلة
    fun removeItem(product: ProductEntity) {
        val updatedList = _cartItems.value?.toMutableList() ?: mutableListOf()
        updatedList.removeAll { it.product.localBarcode == product.localBarcode }

        _cartItems.value = updatedList
        saveCart(updatedList)
    }

    // 4. تفريغ السلة بالكامل
    fun clearCart() {
        val emptyList = emptyList<CartItem>()
        _cartItems.value = emptyList
        saveCart(emptyList)
    }

    // 5. حساب إجمالي الفاتورة
    fun getCartTotal(): Double {
        return _cartItems.value?.sumOf { it.getTotalPrice() } ?: 0.0
    }
}