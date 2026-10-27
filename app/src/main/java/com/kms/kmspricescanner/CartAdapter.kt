package com.kms.kmspricescanner

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.TextView

class CartAdapter(
    context: Context,
    private val items: List<CartItem>,
    private val onCartUpdated: () -> Unit
) : ArrayAdapter<CartItem>(context, 0, items) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var view = convertView
        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.list_item_cart, parent, false)
        }

        val item = items[position]

        val tvName = view!!.findViewById<TextView>(R.id.tv_cart_item_name)
        val tvQty = view.findViewById<TextView>(R.id.tv_cart_item_qty)
        val tvUnitPrice = view.findViewById<TextView>(R.id.tv_cart_item_unit_price)
        val tvTotal = view.findViewById<TextView>(R.id.tv_cart_item_total)

        // ربط الأزرار الثلاثة
        val btnIncrease = view.findViewById<ImageButton>(R.id.btn_increase_item) // ✅ الزر الجديد
        val btnDecrease = view.findViewById<ImageButton>(R.id.btn_decrease_item)
        val btnRemove = view.findViewById<ImageButton>(R.id.btn_remove_item)

        tvName.text = item.product.name
        tvQty.text = item.quantity.toString()
        tvUnitPrice.text = String.format("سعر الوحدة: %.2f ج.م", item.unitPrice)
        tvTotal.text = String.format("%.2f", item.totalPrice)

        // ✅ برمجة زر الزيادة (+)
        btnIncrease.setOnClickListener {
            CartManager.increaseQuantity(item.product.localBarcode)
            onCartUpdated() // تحديث الشاشة فوراً
        }

        // برمجة زر التقليل (-)
        btnDecrease.setOnClickListener {
            CartManager.decreaseQuantity(item.product.localBarcode)
            onCartUpdated()
        }

        // برمجة زر الحذف (🗑️)
        btnRemove.setOnClickListener {
            CartManager.removeItem(item.product.localBarcode)
            onCartUpdated()
        }

        return view
    }
}