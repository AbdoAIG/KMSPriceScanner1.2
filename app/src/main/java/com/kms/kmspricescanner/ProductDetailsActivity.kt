package com.kms.kmspricescanner

import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProductDetailsActivity : AppCompatActivity() {

    private var currentQuantity = 1
    private var unitPrice = 0.0

    // حفظ المتغيرات على مستوى الكلاس للوصول إليها داخل دوال التعديل
    private var productName = ""
    private var localBarcode = ""
    private var globalBarcode = ""
    private var department = ""
    private var branch = ""
    private var imageUrl = ""
    private var pricePieceStr = ""
    private var priceDozenStr = ""
    private var priceCartonStr = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_details)

        // 1. استلام البيانات
        productName = intent.getStringExtra("name") ?: ""
        pricePieceStr = intent.getStringExtra("pricePiece") ?: "0"
        priceDozenStr = intent.getStringExtra("priceDozen") ?: "0"
        priceCartonStr = intent.getStringExtra("priceCarton") ?: "0"
        localBarcode = intent.getStringExtra("localBarcode") ?: "-"
        globalBarcode = intent.getStringExtra("globalBarcode") ?: "-"
        department = intent.getStringExtra("department") ?: ""
        branch = intent.getStringExtra("branch") ?: ""
        imageUrl = intent.getStringExtra("imageUrl") ?: ""

        unitPrice = pricePieceStr.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0

        // 2. ربط العناصر
        val btnBack = findViewById<ImageButton>(R.id.btn_back)
        val imgProduct = findViewById<ImageView>(R.id.img_product)
        val tvDepartment = findViewById<TextView>(R.id.tv_department)
        val tvProductName = findViewById<TextView>(R.id.tv_product_name)
        val tvBranch = findViewById<TextView>(R.id.tv_branch)
        val tvPricePiece = findViewById<TextView>(R.id.tv_price_piece)
        val tvPriceDozen = findViewById<TextView>(R.id.tv_price_dozen)
        val tvPriceCarton = findViewById<TextView>(R.id.tv_price_carton)
        val tvBarcodeLocal = findViewById<TextView>(R.id.tv_barcode_local)
        val tvBarcodeGlobal = findViewById<TextView>(R.id.tv_barcode_global)

        val btnDecreaseQty = findViewById<ImageButton>(R.id.btn_decrease_qty)
        val btnIncreaseQty = findViewById<ImageButton>(R.id.btn_increase_qty)
        val etQuantity = findViewById<EditText>(R.id.et_quantity)
        val tvTotalPrice = findViewById<TextView>(R.id.tv_total_price)
        val btnAddToCart = findViewById<MaterialButton>(R.id.btn_add_to_cart)
        val btnEditPriceFab = findViewById<FloatingActionButton>(R.id.btn_edit_price_fab)

        // 3. التعبئة
        tvProductName.text = productName
        tvPricePiece.text = pricePieceStr
        tvPriceDozen.text = priceDozenStr
        tvPriceCarton.text = priceCartonStr
        tvDepartment.text = department
        tvBranch.text = branch
        tvBarcodeLocal.text = localBarcode
        tvBarcodeGlobal.text = if (globalBarcode.isNotEmpty()) globalBarcode else "-"

        if (imageUrl.isNotEmpty()) {
            Glide.with(this)
                .load(imageUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.company_logo)
                .into(imgProduct)
        } else {
            imgProduct.setImageResource(R.drawable.company_logo)
        }

        updateTotalPrice(tvTotalPrice)

        // 4. أزرار التحكم
        btnDecreaseQty.setOnClickListener {
            if (currentQuantity > 1) {
                currentQuantity--
                etQuantity.setText(currentQuantity.toString())
                updateTotalPrice(tvTotalPrice)
            }
        }

        btnIncreaseQty.setOnClickListener {
            currentQuantity++
            etQuantity.setText(currentQuantity.toString())
            updateTotalPrice(tvTotalPrice)
        }

        etQuantity.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val input = s.toString()
                if (input.isNotEmpty()) {
                    val qty = input.toIntOrNull()
                    if (qty != null && qty > 0) {
                        currentQuantity = qty
                        updateTotalPrice(tvTotalPrice)
                    }
                } else {
                    tvTotalPrice.text = "0.00"
                }
            }
        })

        btnBack.setOnClickListener { finish() }

        btnAddToCart.setOnClickListener {
            val productEntity = ProductEntity(
                localBarcode = localBarcode,
                globalBarcode = globalBarcode,
                name = productName,
                pricePiece = pricePieceStr,
                priceDozen = priceDozenStr,
                priceCarton = priceCartonStr,
                imageUrl = imageUrl,
                department = department,
                branch = branch
            )
            CartManager.addItem(productEntity, currentQuantity)
            Toast.makeText(this, "تمت الإضافة للسلة", Toast.LENGTH_SHORT).show()
            finish()
        }

        // ✅ تفعيل الزر العائم لتعديل السعر
        btnEditPriceFab.setOnClickListener {
            showAdminPasswordDialog()
        }
    }

    private fun updateTotalPrice(tvTotal: TextView) {
        val total = currentQuantity * unitPrice
        tvTotal.text = String.format(java.util.Locale.US, "%.2f", total)
    }

    // --- دوال تعديل الأسعار الأمنة ---

    private fun showAdminPasswordDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("إذن المسؤول")
        builder.setMessage("أدخل كلمة المرور لتعديل الأسعار:")

        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        input.hint = "****"
        builder.setView(input)

        builder.setPositiveButton("دخول") { _, _ ->
            // ✅ التحديث الأمني: نشفر ما أدخله المستخدم ونقارنه بالشفرة المحفوظة
            val enteredHash = HashHelper.sha256(input.text.toString())
            if (enteredHash == Config.ADMIN_PASSWORD_HASH) {
                showEditPriceDialog()
            } else {
                Toast.makeText(this, "كلمة المرور خاطئة!", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("إلغاء", null)
        builder.show()
    }

    private fun showEditPriceDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_price, null)

        val etPiece = dialogView.findViewById<TextInputEditText>(R.id.et_edit_price_piece)
        val etDozen = dialogView.findViewById<TextInputEditText>(R.id.et_edit_price_dozen)
        val etCarton = dialogView.findViewById<TextInputEditText>(R.id.et_edit_price_carton)
        val btnSave = dialogView.findViewById<MaterialButton>(R.id.btn_save_edit)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btn_cancel_edit)

        // تعبئة البيانات الحالية ليسهل تعديلها
        etPiece.setText(pricePieceStr.replace(Regex("[^0-9.]"), ""))
        etDozen.setText(priceDozenStr.replace(Regex("[^0-9.]"), ""))
        etCarton.setText(priceCartonStr.replace(Regex("[^0-9.]"), ""))

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        // جعل خلفية الـ Dialog شفافة ليظهر الـ MaterialCardView بشكل أنيق
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val newPiece = etPiece.text.toString().trim()
            val newDozen = etDozen.text.toString().trim()
            val newCarton = etCarton.text.toString().trim()

            if (newPiece.isEmpty()) {
                etPiece.error = "مطلوب"
                return@setOnClickListener
            }

            btnSave.isEnabled = false
            btnSave.text = "جاري الإرسال..."
            updatePriceOnServer(newPiece, newDozen, newCarton, dialog)
        }

        dialog.show()
    }

    private fun updatePriceOnServer(newPiece: String, newDozen: String, newCarton: String, dialog: AlertDialog) {
        lifecycleScope.launch {
            try {
                // إعداد البيانات المرسلة للسكربت (تأكد أن السكربت الخاص بك يدعم action = edit)
                val params = mapOf(
                    "action" to "edit",
                    "barcode" to localBarcode,
                    "pricePiece" to newPiece,
                    "priceDozen" to newDozen,
                    "priceCarton" to newCarton
                )

                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.apiService.postToGoogleScript(Config.APPS_SCRIPT_URL, params)
                }

                if (response.contains("Success", ignoreCase = true)) {
                    Toast.makeText(this@ProductDetailsActivity, "تم التعديل! اضغط تحديث في الرئيسية لاحقاً لتثبيت التغيير.", Toast.LENGTH_LONG).show()

                    // تحديث الشاشة الحالية فورياً دون الحاجة لإغلاقها
                    val tvPricePiece = findViewById<TextView>(R.id.tv_price_piece)
                    val tvPriceDozen = findViewById<TextView>(R.id.tv_price_dozen)
                    val tvPriceCarton = findViewById<TextView>(R.id.tv_price_carton)

                    pricePieceStr = newPiece
                    priceDozenStr = newDozen
                    priceCartonStr = newCarton

                    tvPricePiece.text = newPiece
                    tvPriceDozen.text = newDozen
                    tvPriceCarton.text = newCarton

                    unitPrice = newPiece.toDoubleOrNull() ?: 0.0
                    updateTotalPrice(findViewById(R.id.tv_total_price))

                    dialog.dismiss()
                } else {
                    Toast.makeText(this@ProductDetailsActivity, "خطأ من السيرفر: $response", Toast.LENGTH_LONG).show()
                    dialog.dismiss()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ProductDetailsActivity, "فشل الاتصال بالإنترنت!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }
    }
}