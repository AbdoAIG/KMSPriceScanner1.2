package com.kms.kmspricescanner

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object CartManager {

    // البيانات الحية للسلة: الشاشات ستراقب هذا المتغير للتحديث التلقائي
    private val _cartItems = MutableLiveData<MutableList<CartItem>>(mutableListOf())
    val cartItems: LiveData<MutableList<CartItem>> = _cartItems

    // ✅ محرك الحفظ (الجديد)
    private var sharedPreferences: SharedPreferences? = null
    private val gson = Gson()
    private const val PREF_NAME = "KMS_Cart_Prefs"
    private const val CART_KEY = "saved_cart_items"

    // ✅ دالة التهيئة: تُستدعى مرة واحدة عند فتح التطبيق لجلب السلة المحفوظة إن وجدت
    fun init(context: Context) {
        if (sharedPreferences == null) {
            // نستخدم applicationContext لمنع تسريب الذاكرة (Memory Leak)
            sharedPreferences = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            loadCart()
        }
    }

    // ✅ دالة الحفظ الصامت في الخلفية
    private fun saveCart(list: List<CartItem>) {
        val jsonString = gson.toJson(list)
        sharedPreferences?.edit()?.putString(CART_KEY, jsonString)?.apply()
    }

    // ✅ دالة استرجاع السلة من الذاكرة
    private fun loadCart() {
        val jsonString = sharedPreferences?.getString(CART_KEY, null)
        if (!jsonString.isNullOrEmpty()) {
            try {
                // تحويل النص المحفوظ (JSON) إلى قائمة من نوع CartItem
                val type = object : TypeToken<MutableList<CartItem>>() {}.type
                val savedList: MutableList<CartItem> = gson.fromJson(jsonString, type)
                _cartItems.postValue(savedList)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // 1. إضافة منتج للسلة
    fun addItem(product: ProductEntity, quantity: Int) {
        val currentList = _cartItems.value ?: mutableListOf()
        val existingItem = currentList.find { it.product.localBarcode == product.localBarcode }

        if (existingItem != null) {
            existingItem.quantity += quantity
        } else {
            currentList.add(CartItem(product, quantity))
        }

        _cartItems.postValue(currentList)
        saveCart(currentList) // ✅ حفظ التغيير فوراً
    }

    // 2. تحديث كمية منتج موجود
    fun updateQuantity(product: ProductEntity, newQuantity: Int) {
        val currentList = _cartItems.value ?: mutableListOf()
        val existingItem = currentList.find { it.product.localBarcode == product.localBarcode }

        if (existingItem != null) {
            if (newQuantity > 0) {
                existingItem.quantity = newQuantity
            } else {
                currentList.remove(existingItem) // حذف المنتج تماماً إذا أصبحت الكمية صفر
            }
            _cartItems.postValue(currentList)
            saveCart(currentList) // ✅ حفظ التغيير فوراً
        }
    }

    // 3. حذف منتج من السلة
    fun removeItem(product: ProductEntity) {
        val currentList = _cartItems.value ?: mutableListOf()
        currentList.removeAll { it.product.localBarcode == product.localBarcode }
        _cartItems.postValue(currentList)
        saveCart(currentList) // ✅ حفظ التغيير فوراً
    }

    // 4. تفريغ السلة بالكامل (بعد إتمام البيع مثلاً)
    fun clearCart() {
        val emptyList = mutableListOf<CartItem>()
        _cartItems.postValue(emptyList)
        saveCart(emptyList) // ✅ تفريغ الذاكرة الدائمة أيضاً
    }

    // 5. حساب إجمالي الفاتورة بالكامل
    fun getCartTotal(): Double {
        return _cartItems.value?.sumOf { it.getTotalPrice() } ?: 0.0
    }
}