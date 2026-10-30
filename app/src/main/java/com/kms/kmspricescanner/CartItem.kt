package com.kms.kmspricescanner

// كلاس ذكي يربط المنتج بالكمية، ويحسب السعر الإجمالي للعنصر الواحد تلقائياً
data class CartItem(
    val product: ProductEntity,
    var quantity: Int
) {
    fun getTotalPrice(): Double {
        // تنظيف السعر من أي نصوص (مثل ج.م) وتحويله لرقم لإجراء العملية الحسابية
        val price = product.pricePiece.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0
        return price * quantity
    }
}