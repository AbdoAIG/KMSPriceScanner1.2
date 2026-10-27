package com.kms.kmspricescanner

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageButton
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CartActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var tvCartGrandTotal: TextView
    private lateinit var btnBack: ImageButton
    private lateinit var btnClear: ImageButton
    private lateinit var btnShare: ImageButton
    private lateinit var btnPrint: ImageButton // ✅ زر الطباعة

    // متغير لحفظ واجهة الويب المؤقتة لمنع مسحها من الذاكرة قبل إتمام الطباعة
    private var printWeb: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart)

        listView = findViewById(R.id.list_view_cart)
        tvCartGrandTotal = findViewById(R.id.tv_cart_grand_total)
        btnBack = findViewById(R.id.btn_back_cart)
        btnClear = findViewById(R.id.btn_clear_cart)
        btnShare = findViewById(R.id.btn_share_cart)

        // ✅ ربط زر الطباعة الجديد
        btnPrint = findViewById(R.id.btn_print_cart)

        updateCartView()

        btnBack.setOnClickListener { finish() }

        btnShare.setOnClickListener { shareInvoice() }

        // ✅ برمجة زر الطباعة
        btnPrint.setOnClickListener { printInvoice() }

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

    private fun updateCartView() {
        val items = CartManager.getItems()
        val adapter = CartAdapter(this, items) {
            updateCartView()
        }
        listView.adapter = adapter
        tvCartGrandTotal.text = String.format("%.2f ج.م", CartManager.getGrandTotal())
    }

    // ---------------------------------------------------------
    // ✅ 1. دالة مشاركة الفاتورة كنص (واتساب وغيرها)
    // ---------------------------------------------------------
    private fun shareInvoice() {
        val items = CartManager.getItems()
        if (items.isEmpty()) {
            Toast.makeText(this, "لا يمكن مشاركة فاتورة فارغة!", Toast.LENGTH_SHORT).show()
            return
        }

        val sdf = SimpleDateFormat("yyyy-MM-dd | hh:mm a", Locale("ar"))
        val currentDate = sdf.format(Date())

        val invoice = StringBuilder()
        invoice.append("🧾 *فاتورة مشتريات* 🧾\n")
        invoice.append("🏢 *كمال سعد للمستلزمات*\n")
        invoice.append("==========================\n")
        invoice.append("📅 التاريخ: $currentDate\n")
        invoice.append("--------------------------\n")

        for (item in items) {
            val priceStr = String.format("%.2f", item.unitPrice)
            val totalStr = String.format("%.2f", item.totalPrice)
            invoice.append("🔸 *${item.product.name}*\n")
            invoice.append("   الكمية: ${item.quantity} × $priceStr = *$totalStr ج.م*\n")
            invoice.append("--------------------------\n")
        }

        invoice.append("==========================\n")
        val grandTotal = String.format("%.2f", CartManager.getGrandTotal())
        invoice.append("💰 *الإجمالي الكلي: $grandTotal ج.م*\n")
        invoice.append("==========================\n")
        invoice.append("🙏 شكراً لتعاملكم معنا 🙏")

        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "فاتورة مشتريات - كمال سعد")
        shareIntent.putExtra(Intent.EXTRA_TEXT, invoice.toString())
        startActivity(Intent.createChooser(shareIntent, "مشاركة الفاتورة عبر:"))
    }

    // ---------------------------------------------------------
    // ✅ 2. الدالة الجديدة: تصميم وتجهيز الفاتورة للطباعة الحقيقية
    // ---------------------------------------------------------
    private fun printInvoice() {
        val items = CartManager.getItems()
        if (items.isEmpty()) {
            Toast.makeText(this, "لا توجد منتجات لطباعتها!", Toast.LENGTH_SHORT).show()
            return
        }

        val sdf = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale("ar"))
        val currentDate = sdf.format(Date())

        // 📝 هنا نقوم بتصميم الفاتورة مثل فواتير الكاشير الحقيقية (HTML & CSS)
        var htmlBody = """
            <!DOCTYPE html>
            <html dir="rtl">
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; color: #000; padding: 20px; }
                    .header { text-align: center; margin-bottom: 20px; border-bottom: 2px dashed #000; padding-bottom: 10px; }
                    .header h1 { margin: 0; font-size: 26px; }
                    .header p { margin: 5px 0; font-size: 16px; color: #333; }
                    table { width: 100%; border-collapse: collapse; margin-bottom: 20px; }
                    th { border-bottom: 2px solid #000; padding: 8px; text-align: right; }
                    td { border-bottom: 1px dashed #ccc; padding: 8px; text-align: right; }
                    .total-box { text-align: left; font-size: 22px; font-weight: bold; margin-top: 20px; border-top: 2px solid #000; padding-top: 10px; }
                    .footer { text-align: center; margin-top: 40px; font-size: 16px; font-weight: bold; }
                </style>
            </head>
            <body>
                <div class="header">
                    <h1>كمال سعد واولاده</h1>
                    <p>فاتورة مبيعات</p>
                    <p>التاريخ: $currentDate</p>
                </div>
                <table>
                    <tr>
                        <th>المنتج</th>
                        <th>السعر</th>
                        <th>الكمية</th>
                        <th>المجموع</th>
                    </tr>
        """.trimIndent()

        // إضافة المنتجات للجدول
        for (item in items) {
            val priceStr = String.format("%.2f", item.unitPrice)
            val totalStr = String.format("%.2f", item.totalPrice)
            htmlBody += """
                    <tr>
                        <td>${item.product.name}</td>
                        <td>$priceStr</td>
                        <td>${item.quantity}</td>
                        <td>$totalStr</td>
                    </tr>
            """.trimIndent()
        }

        // إغلاق الجدول وإضافة الإجمالي
        val grandTotal = String.format("%.2f", CartManager.getGrandTotal())
        htmlBody += """
                </table>
                <div class="total-box">الإجمالي الكلي: $grandTotal ج.م</div>
                <div class="footer">شكراً لزيارتكم! 🛒</div>
            </body>
            </html>
        """.trimIndent()

        // 🌐 تجهيز متصفح وهمي مخفي لترجمة التصميم للورق
        val webView = WebView(this)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                // بمجرد انتهاء رسم التصميم، نرسله لمدير الطباعة
                createWebPrintJob(view)
            }
        }
        webView.loadDataWithBaseURL(null, htmlBody, "text/HTML", "UTF-8", null)
        printWeb = webView
    }

    // 🖨️ إرسال الأمر لنظام الأندرويد لفتح نافذة الطباعة
    private fun createWebPrintJob(webView: WebView) {
        val printManager = getSystemService(Context.PRINT_SERVICE) as PrintManager
        val printAdapter = webView.createPrintDocumentAdapter("Invoice_${System.currentTimeMillis()}")
        val jobName = getString(R.string.app_name) + " Invoice"

        // إعداد الصفحة (بدون تحديد حجم ثابت لتتناسب مع A4 أو طابعة الكاشير الحرارية)
        val builder = PrintAttributes.Builder()
        builder.setMediaSize(PrintAttributes.MediaSize.UNKNOWN_PORTRAIT)

        printManager.print(jobName, printAdapter, builder.build())
    }
}