package com.kms.kmspricescanner

import android.os.Bundle
import android.widget.ImageButton
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class CartActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var tvCartGrandTotal: TextView
    private lateinit var btnBack: ImageButton
    private lateinit var btnClear: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart)

        listView = findViewById(R.id.list_view_cart)
        tvCartGrandTotal = findViewById(R.id.tv_cart_grand_total)
        btnBack = findViewById(R.id.btn_back_cart)
        btnClear = findViewById(R.id.btn_clear_cart)

        // عرض البيانات
        updateCartView()

        btnBack.setOnClickListener { finish() }

        btnClear.setOnClickListener {
            if (CartManager.getItems().isEmpty()) {
                Toast.makeText(this, "السلة فارغة بالفعل", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            AlertDialog.Builder(this)
                .setTitle("إفراغ السلة")
                .setMessage("هل أنت متأكد من حذف جميع المنتجات من الفاتورة؟")
                .setPositiveButton("نعم") { _, _ ->
                    CartManager.clearCart()
                    updateCartView()
                    Toast.makeText(this, "تم إفراغ السلة", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("إلغاء", null)
                .show()
        }
    }

    // ✅ دالة التحديث (تم تعديلها لاستقبال الأوامر من الـ Adapter)
    private fun updateCartView() {
        val items = CartManager.getItems()

        // تمرير دالة تحديث الشاشة للمحول لكي يناديها عند الحذف أو التقليل
        val adapter = CartAdapter(this, items) {
            // هذا الكود سيعمل عندما يضغط المستخدم على (-) أو (🗑️)
            updateCartView()
        }
        listView.adapter = adapter

        // تحديث الإجمالي النهائي
        tvCartGrandTotal.text = String.format("%.2f ج.م", CartManager.getGrandTotal())

        // إذا أصبحت السلة فارغة بعد الحذف
        if (items.isEmpty()) {
            Toast.makeText(this, "السلة فارغة الآن", Toast.LENGTH_SHORT).show()
        }
    }
}