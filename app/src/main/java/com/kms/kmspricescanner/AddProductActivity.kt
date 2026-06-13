package com.kms.kmspricescanner

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import java.nio.charset.Charset

class AddProductActivity : AppCompatActivity() {

    // تعريف عناصر الواجهة
    private lateinit var tvBarcode: TextView
    private lateinit var etName: EditText
    private lateinit var etPricePiece: EditText
    private lateinit var etPriceDozen: EditText
    private lateinit var etPriceCarton: EditText
    private lateinit var etDepartment: EditText
    private lateinit var etBranch: EditText
    private lateinit var btnSave: Button

    // متغير لحفظ الباركود
    private var scannedCode: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_product)

        // 1. ربط العناصر بملف التصميم (XML)
        tvBarcode = findViewById(R.id.tv_barcode_value)
        etName = findViewById(R.id.et_product_name)
        etPricePiece = findViewById(R.id.et_price_piece)
        etPriceDozen = findViewById(R.id.et_price_dozen)
        etPriceCarton = findViewById(R.id.et_price_carton)
        etDepartment = findViewById(R.id.et_department)
        etBranch = findViewById(R.id.et_branch)
        btnSave = findViewById(R.id.btn_save_product)

        // 2. استقبال الباركود من الصفحة السابقة
        scannedCode = intent.getStringExtra("scanned_code") ?: ""
        tvBarcode.text = scannedCode

        // 3. برمجة زر الحفظ
        btnSave.setOnClickListener {
            if (validateInputs()) {
                addProductToSheet()
            }
        }
    }

    // دالة للتحقق من أن الحقول ليست فارغة
    private fun validateInputs(): Boolean {
        if (etName.text.isEmpty()) {
            etName.error = "مطلوب اسم المنتج"
            return false
        }
        if (etPricePiece.text.isEmpty()) {
            etPricePiece.error = "مطلوب السعر"
            return false
        }
        return true
    }

    // دالة الإرسال إلى Google Sheets
    private fun addProductToSheet() {
        val queue = Volley.newRequestQueue(this)
        val url = Config.APPS_SCRIPT_URL

        btnSave.isEnabled = false
        btnSave.text = "جاري الحفظ..."

        val request = object : StringRequest(Request.Method.POST, url,
            { response ->
                btnSave.isEnabled = true
                btnSave.text = "حفظ المنتج"

                if (response.contains("Success", ignoreCase = true)) {
                    Toast.makeText(this, "✅ تم إضافة المنتج بنجاح!", Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    Toast.makeText(this, "تم الإرسال ولكن الرد غير متوقع: $response", Toast.LENGTH_LONG).show()
                }
            },
            { error ->
                btnSave.isEnabled = true
                btnSave.text = "حفظ المنتج"

                // كود معالجة الخطأ الوهمي (كما هو سابقاً)
                var handled = false
                if (error.networkResponse != null && error.networkResponse.data != null) {
                    try {
                        val errorData = String(error.networkResponse.data, Charset.forName("UTF-8"))
                        if (errorData.contains("Success", ignoreCase = true)) {
                            Toast.makeText(this, "✅ تم إضافة المنتج بنجاح!", Toast.LENGTH_LONG).show()
                            finish()
                            handled = true
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                }

                if (!handled) {
                    Toast.makeText(this, "فشل الاتصال: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            }) {

            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["action"] = "add"

                // إرسال الباركود بكل الأسماء المحتملة
                params["id"] = scannedCode
                params["no"] = scannedCode
                params["barcode"] = scannedCode
                params["localBarcode"] = scannedCode

                params["name"] = etName.text.toString().trim()
                params["pricePiece"] = etPricePiece.text.toString().trim()
                params["priceDozen"] = etPriceDozen.text.toString().trim()
                params["priceCarton"] = etPriceCarton.text.toString().trim()
                params["department"] = etDepartment.text.toString().trim()
                params["branch"] = etBranch.text.toString().trim()

                return params
            }
        }

        // ✅✅ هذا هو الجزء الجديد والمهم جداً لمنع التكرار ✅✅
        request.retryPolicy = com.android.volley.DefaultRetryPolicy(
            15000, // زمن الانتظار: 15 ثانية (بدلاً من 2.5 ثانية الافتراضية)
            0,     // عدد المحاولات: 0 (يعني لا تكرر الطلب أبداً)
            com.android.volley.DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )

        queue.add(request)
    }
}