package com.kms.kmspricescanner

// كلاس يمثل عنصراً واحداً داخل السلة (المنتج + الكمية + السعر)
data class CartItem(
    val product: Product,
    var quantity: Int,
    var unitPrice: Double
) {
    val totalPrice: Double get() = quantity * unitPrice
}

object CartManager {
    private val items = mutableListOf<CartItem>()

    fun addItem(product: Product, quantity: Int, price: Double) {
        val existingItem = items.find { it.product.localBarcode == product.localBarcode }
        if (existingItem != null) {
            existingItem.quantity += quantity
        } else {
            items.add(CartItem(product, quantity, price))
        }
    }

    // ✅ دالة جديدة: تقليل الكمية
    fun decreaseQuantity(barcode: String) {
        val item = items.find { it.product.localBarcode == barcode }
        if (item != null) {
            if (item.quantity > 1) {
                item.quantity-- // تقليل حبة واحدة
            } else {
                items.remove(item) // إذا كانت حبة واحدة، نحذفها تماماً
            }
        }
    }
    fun increaseQuantity(barcode: String) {
        val item = items.find { it.product.localBarcode == barcode }
        if (item != null) {
            item.quantity++ // زيادة حبة واحدة
        }
    }

    // ✅ دالة جديدة: حذف المنتج بالكامل
    fun removeItem(barcode: String) {
        items.removeAll { it.product.localBarcode == barcode }
    }

    fun getItems(): List<CartItem> = items

    fun getGrandTotal(): Double {
        return items.sumOf { it.totalPrice }
    }

    fun clearCart() {
        items.clear()
    }
}