package com.kms.kmspricescanner

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

object CartManager {

    // البيانات الحية للسلة: الشاشات ستراقب هذا المتغير للتحديث التلقائي
    private val _cartItems = MutableLiveData<MutableList<CartItem>>(mutableListOf())
    val cartItems: LiveData<MutableList<CartItem>> = _cartItems

    // 1. إضافة منتج للسلة
    fun addItem(product: ProductEntity, quantity: Int) {
        val currentList = _cartItems.value ?: mutableListOf()

        // التحقق الذكي: إذا كان المنتج موجوداً مسبقاً، نزيد كميته فقط ولا نكرره في القائمة
        val existingItem = currentList.find { it.product.localBarcode == product.localBarcode }

        if (existingItem != null) {
            existingItem.quantity += quantity
        } else {
            currentList.add(CartItem(product, quantity))
        }

        // إبلاغ الشاشات بتحديث البيانات (Reactive Programming)
        _cartItems.postValue(currentList)
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
        }
    }

    // 3. حذف منتج من السلة
    fun removeItem(product: ProductEntity) {
        val currentList = _cartItems.value ?: mutableListOf()
        currentList.removeAll { it.product.localBarcode == product.localBarcode }
        _cartItems.postValue(currentList)
    }

    // 4. تفريغ السلة بالكامل (بعد إتمام البيع مثلاً)
    fun clearCart() {
        _cartItems.postValue(mutableListOf())
    }

    // 5. حساب إجمالي الفاتورة بالكامل
    fun getCartTotal(): Double {
        return _cartItems.value?.sumOf { it.getTotalPrice() } ?: 0.0
    }
}