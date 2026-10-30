package com.kms.kmspricescanner

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CartActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvGrandTotal: TextView
    private var cartAdapter: CartRecyclerAdapter? = null

    // متغير للاحتفاظ بـ WebView في الذاكرة أثناء الطباعة حتى لا يغلقه النظام
    private var printWebView: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart)

        val btnBack = findViewById<ImageButton>(R.id.btn_back_cart)
        val btnClear = findViewById<ImageButton>(R.id.btn_clear_cart)
        val btnShare = findViewById<ImageButton>(R.id.btn_share_cart)
        val btnPrint = findViewById<ImageButton>(R.id.btn_print_cart)

        recyclerView = findViewById(R.id.recycler_view_cart)
        tvGrandTotal = findViewById(R.id.tv_cart_grand_total)

        recyclerView.layoutManager = LinearLayoutManager(this)

        // المراقبة التلقائية للسلة
        CartManager.cartItems.observe(this) { items ->
            if (cartAdapter == null) {
                cartAdapter = CartRecyclerAdapter(items) { cartItem, newQty ->
                    CartManager.updateQuantity(cartItem.product, newQty)
                }
                recyclerView.adapter = cartAdapter
            } else {
                cartAdapter?.updateData(items)
            }

            val total = CartManager.getCartTotal()
            tvGrandTotal.text = String.format(Locale.US, "%.2f ج.م", total)
        }

        btnBack.setOnClickListener { finish() }

        btnClear.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("تفريغ السلة")
                .setMessage("هل أنت متأكد أنك تريد حذف كل المنتجات من السلة؟")
                .setPositiveButton("نعم") { _, _ -> CartManager.clearCart() }
                .setNegativeButton("إلغاء", null)
                .show()
        }

        // ✅ تفعيل ميزة المشاركة (Share)
        btnShare.setOnClickListener {
            val items = CartManager.cartItems.value
            if (!items.isNullOrEmpty()) {
                shareInvoiceAsText(items)
            } else {
                Toast.makeText(this, "الفاتورة فارغة!", Toast.LENGTH_SHORT).show()
            }
        }

        // ✅ تفعيل ميزة الطباعة (Print)
        btnPrint.setOnClickListener {
            val items = CartManager.cartItems.value
            if (!items.isNullOrEmpty()) {
                printInvoice(items)
            } else {
                Toast.makeText(this, "الفاتورة فارغة!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ------------------ دوال المشاركة والطباعة ------------------

    private fun shareInvoiceAsText(items: List<CartItem>) {
        val sb = StringBuilder()
        sb.append("🛒 فاتورة مشتريات مبدئية\n")
        sb.append("التاريخ: ${SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault()).format(Date())}\n")
        sb.append("------------------------\n")

        for (item in items) {
            sb.append("▪ ${item.product.name}\n")
            val itemTotal = String.format(Locale.US, "%.2f", item.getTotalPrice())
            sb.append("  الكمية: ${item.quantity} | السعر: $itemTotal ج.م\n")
        }

        sb.append("------------------------\n")
        val grandTotal = String.format(Locale.US, "%.2f", CartManager.getCartTotal())
        sb.append("الإجمالي الكلي: $grandTotal ج.م\n")

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, sb.toString())
            type = "text/plain"
        }
        startActivity(Intent.createChooser(sendIntent, "مشاركة الفاتورة عبر:"))
    }

    private fun printInvoice(items: List<CartItem>) {
        val webView = WebView(this)
        printWebView = webView // حفظ المرجع

        // توليد كود HTML منسق ليكون شكل الفاتورة احترافياً عند طباعته
        val htmlContent = generateHtmlInvoice(items)

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                // بمجرد انتهاء تحميل الـ HTML، نرسله لخدمة الطباعة
                val printManager = getSystemService(Context.PRINT_SERVICE) as PrintManager
                val jobName = "فاتورة_${System.currentTimeMillis()}"

                val printAdapter = view.createPrintDocumentAdapter(jobName)
                printManager.print(
                    jobName,
                    printAdapter,
                    PrintAttributes.Builder().build()
                )
            }
        }
        webView.loadDataWithBaseURL(null, htmlContent, "text/HTML", "UTF-8", null)
    }

    private fun generateHtmlInvoice(items: List<CartItem>): String {
        val grandTotal = String.format(Locale.US, "%.2f", CartManager.getCartTotal())
        val dateStr = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault()).format(Date())

        val sb = StringBuilder()
        sb.append("""
            <html dir="rtl">
            <head>
                <style>
                    body { font-family: Arial, sans-serif; padding: 20px; color: #000; font-size: 14px; }
                    .header { text-align: center; margin-bottom: 20px; border-bottom: 2px dashed #000; padding-bottom: 10px; }
                    .header h2 { margin: 0 0 5px 0; font-size: 22px; }
                    .header p { margin: 0; font-size: 12px; color: #555; }
                    table { width: 100%; border-collapse: collapse; margin-bottom: 20px; }
                    th, td { text-align: right; padding: 8px 0; border-bottom: 1px solid #EEE; }
                    th { font-size: 13px; color: #777; }
                    td { font-size: 14px; font-weight: bold; }
                    .product-name { font-weight: normal; font-size: 13px; color: #333; margin-bottom: 4px; display: block;}
                    .footer { text-align: center; border-top: 2px dashed #000; padding-top: 15px; margin-top: 10px; }
                    .total { font-size: 20px; font-weight: bold; }
                </style>
            </head>
            <body>
                <div class="header">
                    <h2>كمال سعد</h2>
                    <p>للأدوات المكتبية والمدرسية</p>
                    <p>تاريخ الفاتورة: $dateStr</p>
                </div>
                <table>
                    <tr><th>الصنف / الكمية</th><th>الإجمالي</th></tr>
        """.trimIndent())

        for (item in items) {
            val itemTotal = String.format(Locale.US, "%.2f", item.getTotalPrice())
            sb.append("""
                <tr>
                    <td>
                        <span class="product-name">${item.product.name}</span>
                        <span>الكمية: ${item.quantity}</span>
                    </td>
                    <td>$itemTotal</td>
                </tr>
            """.trimIndent())
        }

        sb.append("""
                </table>
                <div class="footer">
                    <p class="total">الإجمالي: $grandTotal ج.م</p>
                    <p style="font-size:12px; margin-top:10px;">شكراً لزيارتكم</p>
                </div>
            </body>
            </html>
        """.trimIndent())

        return sb.toString()
    }
}