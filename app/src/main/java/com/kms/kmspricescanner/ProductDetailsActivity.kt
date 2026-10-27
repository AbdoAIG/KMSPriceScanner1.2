package com.kms.kmspricescanner

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.FitCenter
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.button.MaterialButton // استيراد زر Material

class ProductDetailsActivity : AppCompatActivity() {

    // تعريف العناصر الأساسية
    private lateinit var imgProduct: ImageView
    private lateinit var tvName: TextView
    private lateinit var tvPricePiece: TextView
    private lateinit var tvPriceDozen: TextView
    private lateinit var tvPriceCarton: TextView
    private lateinit var tvLocalBarcode: TextView
    private lateinit var tvGlobalBarcode: TextView
    private lateinit var tvDepartment: TextView
    private lateinit var tvBranch: TextView
    private lateinit var btnBack: ImageButton
    private lateinit var btnEdit: FloatingActionButton

    // ✅ تعريف زر الإضافة للسلة
    private lateinit var btnAddToCart: MaterialButton

    // متغيرات لحفظ البيانات الحالية
    private var currentName: String = ""
    private var currentPricePiece: String = ""
    private var currentPriceDozen: String = ""
    private var currentPriceCarton: String = ""
    private var currentLocalBarcode: String = ""
    private var currentGlobalBarcode: String = ""
    private var currentDepartment: String = ""
    private var currentBranch: String = ""
    private var currentImageUrl: String = ""

    // متغيرات حاسبة الكميات
    private var calculatorQuantity = 1
    private var calculatorUnitPrice = 0.0

    // عنصر الكتابة الجديد
    private lateinit var etQuantity: EditText
    private lateinit var tvTotal: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_details)

        // 1. ربط العناصر بالتصميم
        imgProduct = findViewById(R.id.img_product)
        tvName = findViewById(R.id.tv_product_name)
        tvPricePiece = findViewById(R.id.tv_price_piece)
        tvPriceDozen = findViewById(R.id.tv_price_dozen)
        tvPriceCarton = findViewById(R.id.tv_price_carton)
        tvLocalBarcode = findViewById(R.id.tv_barcode_local)
        tvGlobalBarcode = findViewById(R.id.tv_barcode_global)
        tvDepartment = findViewById(R.id.tv_department)
        tvBranch = findViewById(R.id.tv_branch)
        btnBack = findViewById(R.id.btn_back)
        btnEdit = findViewById(R.id.btn_edit_price_fab)

        // ✅ ربط زر الإضافة للسلة من التصميم
        btnAddToCart = findViewById(R.id.btn_add_to_cart)

        // 2. استقبال البيانات
        val intent = intent
        currentName = intent.getStringExtra("name") ?: ""
        currentPricePiece = intent.getStringExtra("pricePiece") ?: "0"
        currentPriceDozen = intent.getStringExtra("priceDozen") ?: "0"
        currentPriceCarton = intent.getStringExtra("priceCarton") ?: "0"
        currentLocalBarcode = intent.getStringExtra("localBarcode") ?: ""
        currentGlobalBarcode = intent.getStringExtra("globalBarcode") ?: ""
        currentDepartment = intent.getStringExtra("department") ?: ""
        currentBranch = intent.getStringExtra("branch") ?: ""
        currentImageUrl = intent.getStringExtra("imageUrl") ?: ""

        // 3. عرض البيانات النصية
        tvName.text = currentName
        tvPricePiece.text = currentPricePiece
        tvPriceDozen.text = currentPriceDozen
        tvPriceCarton.text = currentPriceCarton
        tvLocalBarcode.text = currentLocalBarcode
        tvGlobalBarcode.text = currentGlobalBarcode
        tvDepartment.text = currentDepartment
        tvBranch.text = currentBranch

        // ---------------------------------------------------------
        // ✅ منطق حاسبة الكميات (معدل للكتابة المباشرة)
        // ---------------------------------------------------------
        calculatorUnitPrice = parsePrice(currentPricePiece)

        // ربط العناصر الجديدة
        etQuantity = findViewById(R.id.et_quantity)
        tvTotal = findViewById(R.id.tv_total_price)
        val btnIncrease = findViewById<android.view.View>(R.id.btn_increase_qty)
        val btnDecrease = findViewById<android.view.View>(R.id.btn_decrease_qty)

        // حساب مبدئي
        calculateTotal()

        // أ) مراقبة الكتابة داخل المربع
        etQuantity.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val input = s.toString()
                if (input.isNotEmpty()) {
                    try {
                        calculatorQuantity = input.toInt()
                        calculateTotal()
                    } catch (e: NumberFormatException) {
                        // رقم كبير جداً
                    }
                } else {
                    calculatorQuantity = 0
                    tvTotal.text = "0.00"
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // ب) زر الزيادة (+)
        btnIncrease.setOnClickListener {
            updateQuantityFromInput()
            calculatorQuantity++
            etQuantity.setText(calculatorQuantity.toString())
            etQuantity.setSelection(etQuantity.text.length)
        }

        // ج) زر النقصان (-)
        btnDecrease.setOnClickListener {
            updateQuantityFromInput()
            if (calculatorQuantity > 1) {
                calculatorQuantity--
                etQuantity.setText(calculatorQuantity.toString())
                etQuantity.setSelection(etQuantity.text.length)
            }
        }

        // ---------------------------------------------------------
        // ✅ برمجة إضافة المنتج إلى السلة 🛒
        // ---------------------------------------------------------
        btnAddToCart.setOnClickListener {
            // التحقق من أن الكمية صحيحة
            if (calculatorQuantity <= 0) {
                Toast.makeText(this, "يرجى تحديد كمية صحيحة أولاً", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // إنشاء كائن للمنتج الحالي
            val currentProduct = Product(
                localBarcode = currentLocalBarcode,
                globalBarcode = currentGlobalBarcode,
                name = currentName,
                pricePiece = currentPricePiece,
                priceDozen = currentPriceDozen,
                priceCarton = currentPriceCarton,
                imageUrl = currentImageUrl,
                department = currentDepartment,
                branch = currentBranch
            )

            // إضافته لمدير السلة
            CartManager.addItem(currentProduct, calculatorQuantity, calculatorUnitPrice)

            // إظهار رسالة تفيد بنجاح الإضافة
            val totalInCart = String.format("%.2f", CartManager.getGrandTotal())
            Toast.makeText(this, "✅ تمت الإضافة! إجمالي السلة: $totalInCart ج.م", Toast.LENGTH_LONG).show()

            // العودة للشاشة الرئيسية
            finish()
        }
        // ---------------------------------------------------------

        // ---------------------------------------------------------
        // ✅ منطق عرض الصورة
        // ---------------------------------------------------------
        val prefs = getSharedPreferences("KMS_Theme", Context.MODE_PRIVATE)
        val themeJson = prefs.getString("theme_data", "{}")
        var themeLogoUrl = ""
        try {
            val theme = org.json.JSONObject(themeJson)
            if (theme.has("logoUrl")) themeLogoUrl = theme.getString("logoUrl")
        } catch (e: Exception) {}

        val imageToLoad: String
        val isRealProductImage: Boolean

        if (currentImageUrl.isNotEmpty() && currentImageUrl != "null") {
            imageToLoad = currentImageUrl
            isRealProductImage = true
        } else if (themeLogoUrl.isNotEmpty()) {
            imageToLoad = themeLogoUrl
            isRealProductImage = false
        } else {
            imageToLoad = ""
            isRealProductImage = false
        }

        if (imageToLoad.isNotEmpty()) {
            var glideRequest = Glide.with(this).load(imageToLoad)
            glideRequest = if (isRealProductImage) {
                glideRequest.transform(CenterCrop(), RoundedCorners(28))
            } else {
                glideRequest.transform(FitCenter(), RoundedCorners(28))
            }
            glideRequest
                .placeholder(R.drawable.company_logo)
                .error(R.drawable.company_logo)
                .into(imgProduct)
        } else {
            imgProduct.scaleType = ImageView.ScaleType.FIT_CENTER
            imgProduct.setImageResource(R.drawable.company_logo)
        }

        btnBack.setOnClickListener { finish() }
        btnEdit.setOnClickListener { showPasswordDialog() }
    }

    // ---------------------------------------------------------
    // ✅ دوال مساعدة للحاسبة
    // ---------------------------------------------------------
    private fun calculateTotal() {
        val total = calculatorUnitPrice * calculatorQuantity
        tvTotal.text = String.format("%.2f", total)
    }

    private fun updateQuantityFromInput() {
        val text = etQuantity.text.toString()
        if (text.isNotEmpty()) {
            try {
                calculatorQuantity = text.toInt()
            } catch (e: Exception) {
                calculatorQuantity = 1
            }
        } else {
            calculatorQuantity = 0
        }
    }

    private fun parsePrice(priceStr: String): Double {
        return try {
            val cleanStr = priceStr.replace(Regex("[^0-9.]"), "")
            if (cleanStr.isEmpty()) 0.0 else cleanStr.toDouble()
        } catch (e: Exception) {
            0.0
        }
    }

    // ---------------------------------------------------------
    // دوال التعديل
    // ---------------------------------------------------------
    private fun showPasswordDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("وضع التعديل")
        builder.setMessage("أدخل كلمة مرور المسؤول لتعديل السعر:")

        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        input.hint = "****"
        builder.setView(input)

        builder.setPositiveButton("دخول") { _, _ ->
            if (input.text.toString() == Config.ADMIN_PASSWORD) {
                showEditDataDialog()
            } else {
                Toast.makeText(this, "كلمة المرور خاطئة!", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("إلغاء", null)
        builder.show()
    }

    private fun showEditDataDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("تعديل بيانات المنتج")

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 40, 50, 10)

        val scrollView = ScrollView(this)

        val inputName = createStyledEditText("اسم المنتج", currentName)
        val inputPricePiece = createStyledEditText("سعر القطعة", currentPricePiece, true)
        val inputPriceDozen = createStyledEditText("سعر الدستة", currentPriceDozen, true)
        val inputPriceCarton = createStyledEditText("سعر الكرتونة", currentPriceCarton, true)
        val inputBranch = createStyledEditText("الفرع", currentBranch)

        layout.addView(inputName)
        layout.addView(inputPricePiece)
        layout.addView(inputPriceDozen)
        layout.addView(inputPriceCarton)
        layout.addView(inputBranch)

        scrollView.addView(layout)
        builder.setView(scrollView)

        builder.setPositiveButton("حفظ التعديلات") { _, _ ->
            updateProductOnServer(
                inputName.text.toString(),
                inputPricePiece.text.toString(),
                inputPriceDozen.text.toString(),
                inputPriceCarton.text.toString(),
                inputBranch.text.toString()
            )
        }
        builder.setNegativeButton("إلغاء", null)
        builder.show()
    }

    private fun createStyledEditText(hint: String, value: String, isNumber: Boolean = false): EditText {
        val editText = EditText(this)
        editText.hint = hint
        editText.setText(value)
        if (isNumber) {
            editText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(0, 0, 0, 30)
        editText.layoutParams = params
        return editText
    }

    private fun updateProductOnServer(name: String, pPiece: String, pDozen: String, pCarton: String, branch: String) {
        val queue = Volley.newRequestQueue(this)
        val url = "https://docs.google.com/spreadsheets/d/e/2PACX-1vQGDqnKrEKr0HrJUM7QMGKtLhoePdkZVsBtNZ3mW4mhxiqKUGW3fm1J4qbb6F0AWp3q9F1-xfaxOD3-/pubhtml"

        btnEdit.isEnabled = false
        Toast.makeText(this, "جارٍ الاتصال بالسيرفر...", Toast.LENGTH_LONG).show()

        val request = object : StringRequest(Request.Method.POST, url,
            { response ->
                btnEdit.isEnabled = true
                if (response.contains("Success", ignoreCase = true)) {
                    Toast.makeText(this, "✅ تم التعديل بنجاح!", Toast.LENGTH_LONG).show()
                    tvName.text = name
                    tvPricePiece.text = pPiece
                    tvPriceDozen.text = pDozen
                    tvPriceCarton.text = pCarton
                    tvBranch.text = branch

                    // تحديث الحاسبة
                    calculatorUnitPrice = parsePrice(pPiece)
                    calculatorQuantity = 1
                    etQuantity.setText("1")
                    calculateTotal()

                } else {
                    Toast.makeText(this, "حدث خطأ: $response", Toast.LENGTH_LONG).show()
                }
            },
            { error ->
                btnEdit.isEnabled = true
                var handled = false
                if (error.networkResponse?.data != null) {
                    val errorString = String(error.networkResponse.data)
                    if (errorString.contains("Success", ignoreCase = true)) {
                        Toast.makeText(this, "✅ تم التعديل بنجاح!", Toast.LENGTH_LONG).show()
                        tvName.text = name
                        tvPricePiece.text = pPiece
                        tvPriceDozen.text = pDozen
                        tvPriceCarton.text = pCarton
                        tvBranch.text = branch
                        handled = true
                    }
                }
                if (!handled) Toast.makeText(this, "فشل الاتصال", Toast.LENGTH_LONG).show()
            }) {

            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["action"] = "update"
                params["id"] = currentLocalBarcode
                params["barcode"] = currentLocalBarcode
                params["globalBarcode"] = currentGlobalBarcode
                params["name"] = name
                params["pricePiece"] = pPiece
                params["priceDozen"] = pDozen
                params["priceCarton"] = pCarton
                params["branch"] = branch
                return params
            }
        }
        request.retryPolicy = com.android.volley.DefaultRetryPolicy(45000, 0, com.android.volley.DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        queue.add(request)
    }
}