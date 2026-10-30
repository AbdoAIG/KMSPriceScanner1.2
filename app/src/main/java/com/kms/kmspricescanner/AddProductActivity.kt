package com.kms.kmspricescanner

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddProductActivity : AppCompatActivity() {

    private lateinit var tvBarcode: TextView
    private lateinit var etName: EditText
    private lateinit var etPricePiece: EditText
    private lateinit var etPriceDozen: EditText
    private lateinit var etPriceCarton: EditText
    private lateinit var etDepartment: EditText
    private lateinit var etBranch: EditText
    private lateinit var btnSave: Button

    private var scannedCode: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_product)

        // 1. ربط عناصر الواجهة
        tvBarcode = findViewById(R.id.tv_barcode_value)
        etName = findViewById(R.id.et_product_name)
        etPricePiece = findViewById(R.id.et_price_piece)
        etPriceDozen = findViewById(R.id.et_price_dozen)
        etPriceCarton = findViewById(R.id.et_price_carton)
        etDepartment = findViewById(R.id.et_department)
        etBranch = findViewById(R.id.et_branch)
        btnSave = findViewById(R.id.btn_save_product)

        // 2. استقبال الباركود من الشاشة السابقة
        scannedCode = intent.getStringExtra("scanned_code") ?: ""
        tvBarcode.text = scannedCode

        // 3. زر الحفظ
        btnSave.setOnClickListener {
            if (validateInputs()) {
                addProductToSheet()
            }
        }
    }

    private fun validateInputs(): Boolean {
        if (etName.text.isEmpty()) {
            etName.error = "مطلوب"
            return false
        }
        if (etPricePiece.text.isEmpty()) {
            etPricePiece.error = "مطلوب"
            return false
        }
        return true
    }

    // الاعتماد على Retrofit و Coroutines بدلاً من Volley
    private fun addProductToSheet() {
        btnSave.isEnabled = false
        btnSave.text = "جاري الحفظ..."

        lifecycleScope.launch {
            try {
                // تجميع البيانات للإرسال
                val params = mapOf(
                    "action" to "add",
                    "no" to scannedCode,
                    "barcode" to scannedCode,
                    "localBarcode" to scannedCode,
                    "name" to etName.text.toString().trim(),
                    "pricePiece" to etPricePiece.text.toString().trim(),
                    "priceDozen" to etPriceDozen.text.toString().trim(),
                    "priceCarton" to etPriceCarton.text.toString().trim(),
                    "department" to etDepartment.text.toString().trim(),
                    "branch" to etBranch.text.toString().trim()
                )

                // إرسال الطلب في مسار الخلفية IO لمنع تجميد الشاشة
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.apiService.postToGoogleScript(Config.APPS_SCRIPT_URL, params)
                }

                if (response.contains("Success", ignoreCase = true)) {
                    Toast.makeText(this@AddProductActivity, "تم الحفظ بنجاح!", Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    Toast.makeText(this@AddProductActivity, "خطأ: $response", Toast.LENGTH_LONG).show()
                    btnSave.isEnabled = true
                    btnSave.text = "حفظ المنتج"
                }

            } catch (e: Exception) {
                Toast.makeText(this@AddProductActivity, "فشل الاتصال: ${e.message}", Toast.LENGTH_SHORT).show()
                btnSave.isEnabled = true
                btnSave.text = "حفظ المنتج"
            }
        }
    }
}