package com.kms.kmspricescanner

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.navigation.NavigationView
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors

// ---------------------------------------------------------
// كلاس البيانات
// ---------------------------------------------------------
data class Product(
    val localBarcode: String,
    val globalBarcode: String,
    val name: String,
    val pricePiece: String,
    val priceDozen: String,
    val priceCarton: String,
    val imageUrl: String,
    val department: String,
    val branch: String
)

class MainActivity : AppCompatActivity() {

    // تعريف العناصر
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView

    private lateinit var btnMenu: ImageButton
    private lateinit var btnHistory: ImageButton
    private lateinit var btnOpenCart: ImageButton

    private var effectOverlay: FrameLayout? = null

    private lateinit var etSearch: EditText
    private lateinit var btnScan: ExtendedFloatingActionButton
    private lateinit var btnUpdate: ImageButton

    private lateinit var txtStatus: TextView
    private lateinit var listViewProducts: ListView
    private lateinit var scrollBranches: HorizontalScrollView
    private lateinit var layoutBranchesContainer: LinearLayout

    private lateinit var cardBannerContainer: androidx.cardview.widget.CardView
    private lateinit var bannerViewPager: ViewPager2
    private val bannerImages = ArrayList<Any>()
    private val sliderHandler = Handler(Looper.getMainLooper())
    private lateinit var sliderRunnable: Runnable

    private val allProductsList = ArrayList<Product>()
    private var currentFilteredList = ArrayList<Product>()
    private var isHistoryMode = false

    // ✅ متغير لحفظ تاريخ التحديث القادم من السيرفر
    private var lastUpdateDateString: String = ""

    private val CSV_URL = Config.CSV_URL
    private val FILE_NAME = "kmsprices.csv"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. ربط العناصر
        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
        effectOverlay = findViewById(R.id.effect_overlay)

        btnMenu = findViewById(R.id.btn_menu)
        btnHistory = findViewById(R.id.btn_history)
        btnOpenCart = findViewById(R.id.btn_open_cart)

        etSearch = findViewById(R.id.et_search)
        etSearch.isEnabled = false
        etSearch.hint = "يرجى الانتظار..."

        btnScan = findViewById(R.id.btn_scan)
        btnUpdate = findViewById(R.id.btn_update_admin)
        txtStatus = findViewById(R.id.txt_status)
        listViewProducts = findViewById(R.id.list_view_products)

        scrollBranches = findViewById(R.id.scroll_branches)
        layoutBranchesContainer = findViewById(R.id.layout_branches_container)
        cardBannerContainer = findViewById(R.id.card_banner_container)

        // 2. إعداد الميزات
        setupBannerSlider()
        setupDrawerFooter()
        setupDrawerInteractions()
        applySavedTheme()
        fetchThemeFromServer()
        loadProductsToMemoryBackground()

        // ✅ فحص حالة السيرفر لجلب تاريخ التحديث
        checkServerStatus()

        // 3. برمجة الأزرار
        btnMenu.setOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                drawerLayout.openDrawer(GravityCompat.START)
            }
        }

        btnHistory.setOnClickListener { showScanHistory() }

        navView.setNavigationItemSelectedListener { menuItem ->
            val selectedDept = menuItem.title.toString()
            if (selectedDept == "عرض الكل") {
                filterProductsList("")
                etSearch.setText("")
                scrollBranches.visibility = View.GONE
                cardBannerContainer.visibility = View.VISIBLE
            } else {
                filterByDepartment(selectedDept)
                cardBannerContainer.visibility = View.GONE
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        btnOpenCart.setOnClickListener {
            val intent = Intent(this, CartActivity::class.java)
            startActivity(intent)
        }

        // البحث الحي
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!isHistoryMode) {
                    filterProductsList(s.toString())
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        listViewProducts.setOnItemClickListener { _, _, position, _ ->
            if (position < currentFilteredList.size) {
                openProductDetailsPage(currentFilteredList[position])
            }
        }

        btnScan.setOnClickListener { scanCode() }
        btnUpdate.setOnClickListener { showConfirmationDialog() }

        // ✅ اعتراض زر الرجوع في الهاتف لمنع الخروج المفاجئ
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // إذا كانت القائمة الجانبية مفتوحة، قم بإغلاقها أولاً
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                }
                // إذا لم تكن مفتوحة، أظهر رسالة تأكيد الخروج
                else {
                    showExitConfirmationDialog()
                }
            }
        })
    }

    // ---------------------------------------------------------
    // ✅ وظيفة جلب وعرض تاريخ آخر تحديث
    // ---------------------------------------------------------
    private fun checkServerStatus() {
        // قمنا بإضافة الأكشن مباشرة في الرابط (طريقة GET)
        val url = Config.APPS_SCRIPT_URL + "?action=check"
        val queue = Volley.newRequestQueue(this)

        val request = object : StringRequest(Request.Method.GET, url,
            { response ->
                val cleanResponse = response.trim()

                try {
                    val jsonObject = org.json.JSONObject(cleanResponse)
                    if (jsonObject.getString("status") == "success") {
                        val date = jsonObject.optString("last_update", "")
                        if (date.isNotEmpty()) {
                            lastUpdateDateString = "🕒 $date"
                        }
                        updateMainStatusText()
                    }
                } catch (e: Exception) {
                    // إذا ظهر هذا الخطأ، فهذا يؤكد أن المشكلة في حساب جوجل (الإيميل)
                    Toast.makeText(this@MainActivity, "رد السيرفر غير مفهوم، يرجى مراجعة إعدادات حساب جوجل", Toast.LENGTH_LONG).show()
                }
            }, { error ->
                Toast.makeText(this@MainActivity, "فشل الاتصال: ${error.message}", Toast.LENGTH_SHORT).show()
            }) {
            // مسحنا دالة getParams لأننا أرسلنا البيانات في الرابط مباشرة
        }
        queue.add(request)
    }

    private fun updateMainStatusText() {
        runOnUiThread {
            if (lastUpdateDateString.isNotEmpty()) {
                txtStatus.text = "العدد الكلي: ${allProductsList.size} صنف | $lastUpdateDateString"
            } else {
                txtStatus.text = "العدد الكلي: ${allProductsList.size} صنف"
            }
            txtStatus.setTextColor(Color.parseColor("#2E7D32")) // لون أخضر داكن
        }
    }

    // ---------------------------------------------------------
    // وظيفة عرض السجل
    // ---------------------------------------------------------
    private fun showScanHistory() {
        val historyBarcodes = ScanHistoryHelper.getHistory(this)
        val historyProducts = ArrayList<Product>()
        for (code in historyBarcodes) {
            val product = allProductsList.find { it.localBarcode.trim() == code.trim() }
            if (product != null) historyProducts.add(product)
        }

        if (historyProducts.isEmpty()) {
            Toast.makeText(this, "لم يتم فتح أي منتجات بعد", Toast.LENGTH_SHORT).show()
            return
        }

        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(etSearch.windowToken, 0)

        isHistoryMode = true
        etSearch.setText("")
        isHistoryMode = false

        currentFilteredList = historyProducts
        updateListView(currentFilteredList)

        txtStatus.text = "سجل آخر العمليات (${historyProducts.size})"
        txtStatus.setTextColor(Color.parseColor("#E65100"))

        scrollBranches.visibility = View.GONE
        cardBannerContainer.visibility = View.GONE

        Toast.makeText(this, "تم عرض السجل", Toast.LENGTH_SHORT).show()
    }

    // ---------------------------------------------------------
    // إعدادات أخرى
    // ---------------------------------------------------------
    private fun setupBannerSlider() {
        bannerViewPager = findViewById(R.id.banner_view_pager)
        val adapter = BrandsAdapter(bannerImages)
        bannerViewPager.adapter = adapter
        bannerViewPager.clipToPadding = false
        bannerViewPager.clipChildren = false
        bannerViewPager.offscreenPageLimit = 3
        bannerViewPager.getChildAt(0).overScrollMode = RecyclerView.OVER_SCROLL_NEVER

        sliderRunnable = Runnable {
            if (bannerImages.size > 1) {
                var nextItem = bannerViewPager.currentItem + 1
                if (nextItem >= bannerImages.size) nextItem = 0
                bannerViewPager.setCurrentItem(nextItem, true)
            }
            sliderHandler.postDelayed(sliderRunnable, 4000)
        }
        loadCachedBanners(adapter)
        fetchBannersFromSheet(adapter)
    }

    private fun loadCachedBanners(adapter: BrandsAdapter) {
        val prefs = getSharedPreferences("KMS_Prefs", Context.MODE_PRIVATE)
        val cachedString = prefs.getString("cached_banners", null)
        if (cachedString != null && cachedString.isNotEmpty()) {
            val urls = cachedString.split(",")
            bannerImages.clear()
            for (url in urls) if (url.isNotEmpty()) bannerImages.add(url)
            adapter.notifyDataSetChanged()
        } else {
            bannerImages.add(R.drawable.company_logo)
            adapter.notifyDataSetChanged()
        }
    }

    private fun saveBannersToCache(response: String) {
        val prefs = getSharedPreferences("KMS_Prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("cached_banners", response).apply()
    }

    private fun fetchBannersFromSheet(adapter: BrandsAdapter) {
        val url = Config.APPS_SCRIPT_URL
        val queue = Volley.newRequestQueue(this)
        val request = object : StringRequest(Request.Method.POST, url,
            { response ->
                if (response.isNotEmpty() && !response.contains("Error")) {
                    saveBannersToCache(response)
                    val urls = response.split(",")
                    if (urls.isNotEmpty()) {
                        bannerImages.clear()
                        for (imgUrl in urls) if (imgUrl.trim().isNotEmpty()) bannerImages.add(imgUrl.trim())
                        adapter.notifyDataSetChanged()
                    }
                }
            },
            { }) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["action"] = "getBanners"
                return params
            }
        }
        queue.add(request)
    }

    private fun setupDrawerFooter() {
        try {
            val versionTextView = findViewById<TextView>(R.id.tv_app_version)
            if (versionTextView != null) {
                val pInfo = packageManager.getPackageInfo(packageName, 0)
                versionTextView.text = "الإصدار: ${pInfo.versionName}"
            }
        } catch (e: Exception) {}
    }

    private fun setupDrawerInteractions() {
        drawerLayout.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                if (effectOverlay != null) {
                    effectOverlay!!.alpha = 1 - slideOffset
                    effectOverlay!!.visibility = if (slideOffset > 0.0f) View.INVISIBLE else View.VISIBLE
                }
            }
            override fun onDrawerOpened(drawerView: View) {
                super.onDrawerOpened(drawerView)
                try {
                    val devText = findViewById<TextView>(R.id.tv_developer_name)
                    if (devText != null) {
                        devText.alpha = 0f
                        devText.translationY = 50f
                        devText.animate().alpha(1f).translationY(0f).setDuration(800).start()
                    }
                } catch (e: Exception) {}
            }
            override fun onDrawerClosed(drawerView: View) {
                super.onDrawerClosed(drawerView)
                if (effectOverlay != null) {
                    effectOverlay!!.visibility = View.VISIBLE
                    effectOverlay!!.alpha = 1f
                }
            }
        })
    }

    private fun loadProductsToMemoryBackground() {
        runOnUiThread {
            updateMainStatusText() // ✅ تحديث النص
            etSearch.isEnabled = true
            etSearch.hint = "ابحث عن منتج..."
            updateListView(ArrayList())
            populateSideMenu()
        }
        val executor = Executors.newSingleThreadExecutor()
        executor.execute {
            allProductsList.clear()
            var fileContent: BufferedReader? = null
            try {
                val privateFile = File(filesDir, FILE_NAME)
                fileContent = if (privateFile.exists()) {
                    BufferedReader(InputStreamReader(privateFile.inputStream(), StandardCharsets.UTF_8))
                } else {
                    BufferedReader(InputStreamReader(assets.open(FILE_NAME), StandardCharsets.UTF_8))
                }

                var line: String?
                val csvSplitRegex = ",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)".toRegex()
                while (fileContent.readLine().also { line = it } != null) {
                    val tokens = line!!.split(csvSplitRegex)
                    if (tokens.size >= 8) {
                        fun clean(value: String): String = value.replace("\"", "").replace(",", "").trim()
                        allProductsList.add(Product(
                            localBarcode = clean(tokens[0]),
                            globalBarcode = clean(tokens[1]),
                            name = tokens[2].replace("\"", "").trim(),
                            pricePiece = clean(tokens[3]),
                            priceDozen = clean(tokens[4]),
                            priceCarton = clean(tokens[5]),
                            imageUrl = clean(tokens[6]),
                            department = clean(tokens[7]),
                            branch = if (tokens.size > 8) clean(tokens[8]) else "عام"
                        ))
                    }
                }
                runOnUiThread {
                    updateMainStatusText() // ✅ تحديث النص
                    updateListView(ArrayList())
                    populateSideMenu()
                }
            } catch (e: Exception) {
                runOnUiThread { txtStatus.text = "خطأ: ${e.message}" }
            } finally {
                fileContent?.close()
            }
        }
    }

    private fun populateSideMenu() {
        val menu = navView.menu
        menu.clear()
        val itemAll = menu.add(0, 0, 0, "عرض الكل")
        itemAll.setIcon(android.R.drawable.ic_menu_sort_by_size)
        val departments = allProductsList.map { it.department.trim() }
            .filter { it.isNotEmpty() && !it.contains("http") }.distinct().sorted()
        for ((index, dept) in departments.withIndex()) {
            menu.add(1, index + 1, index + 1, dept).setIcon(android.R.drawable.star_off)
        }
        navView.invalidate()
    }

    private fun openProductDetailsPage(p: Product) {
        ScanHistoryHelper.saveProduct(this, p.localBarcode)

        val intent = Intent(this, ProductDetailsActivity::class.java)
        intent.putExtra("name", p.name)
        intent.putExtra("pricePiece", p.pricePiece)
        intent.putExtra("priceDozen", p.priceDozen)
        intent.putExtra("priceCarton", p.priceCarton)
        intent.putExtra("localBarcode", p.localBarcode)
        intent.putExtra("globalBarcode", p.globalBarcode)
        intent.putExtra("department", p.department)
        intent.putExtra("branch", p.branch)
        intent.putExtra("imageUrl", p.imageUrl)
        startActivity(intent)
    }

    private fun filterByDepartment(deptName: String) {
        etSearch.setText("")
        currentFilteredList = allProductsList.filter {
            it.department.equals(deptName, ignoreCase = true)
        } as ArrayList<Product>
        txtStatus.text = "قسم: $deptName (${currentFilteredList.size})"
        txtStatus.setTextColor(Color.BLUE)
        updateListView(currentFilteredList)
        setupBranchButtons(deptName)
    }

    private fun filterProductsList(query: String) {
        val searchText = query.trim().lowercase()
        if (searchText.isEmpty()) {
            currentFilteredList = ArrayList()
            updateListView(currentFilteredList)
            updateMainStatusText() // ✅ استدعاء النص المُحدث مع التاريخ
            scrollBranches.visibility = View.GONE
            cardBannerContainer.visibility = View.VISIBLE
            return
        }
        cardBannerContainer.visibility = View.GONE
        currentFilteredList = allProductsList.filter { product ->
            product.name.lowercase().contains(searchText) ||
                    product.localBarcode.contains(searchText) ||
                    product.globalBarcode.contains(searchText)
        } as ArrayList<Product>

        if (currentFilteredList.isNotEmpty()) {
            txtStatus.text = "تم العثور على (${currentFilteredList.size}) نتيجة"
            txtStatus.setTextColor(Color.parseColor("#008000"))
        } else {
            txtStatus.text = "لا توجد نتائج"
            txtStatus.setTextColor(Color.RED)
        }
        updateListView(currentFilteredList)
    }

    private fun updateListView(products: List<Product>) {
        val adapter = ProductAdapter(this, products)
        listViewProducts.adapter = adapter
    }

    private fun scanCode() {
        val intent = Intent(this, ScannerActivity::class.java)
        scannerLauncher.launch(intent)
    }

    private val scannerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val scannedCode = result.data?.getStringExtra("scanned_code")
            if (scannedCode != null) {
                val foundProduct = allProductsList.find { product ->
                    product.localBarcode == scannedCode || product.globalBarcode == scannedCode
                }
                if (foundProduct != null) {
                    openProductDetailsPage(foundProduct)
                    etSearch.setText("")
                } else {
                    showAddProductDialog(scannedCode)
                }
            }
        }
    }

    private fun showAddProductDialog(scannedCode: String) {
        AlertDialog.Builder(this)
            .setTitle("منتج غير مسجل")
            .setMessage("الباركود: $scannedCode\nغير موجود.\nهل تريد إضافته؟")
            .setPositiveButton("إضافة") { _, _ -> showPasswordDialog(scannedCode) }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun showPasswordDialog(scannedCode: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("إذن المسؤول")
        builder.setMessage("أدخل كلمة المرور:")
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        input.hint = "****"
        builder.setView(input)
        builder.setPositiveButton("دخول") { _, _ ->
            if (input.text.toString() == Config.ADMIN_PASSWORD) {
                val intent = Intent(this, AddProductActivity::class.java)
                intent.putExtra("scanned_code", scannedCode)
                startActivity(intent)
            } else {
                Toast.makeText(this, "كلمة المرور خاطئة", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("إلغاء", null)
        builder.show()
    }

    private fun showConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("تحديث البيانات")
            .setMessage("هل تريد تحميل أحدث الأسعار من السيرفر؟")
            .setPositiveButton("نعم") { _, _ ->
                downloadCSVFromSheet()
                fetchThemeFromServer()
                checkServerStatus() // ✅ فحص التاريخ مجدداً عند التحديث
                Toast.makeText(this, "جاري التحديث...", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun downloadCSVFromSheet() {
        txtStatus.text = "جاري الاتصال..."
        val queue = Volley.newRequestQueue(this)
        val url = Config.CSV_URL.ifEmpty { CSV_URL }
        val stringRequest = object : StringRequest(Request.Method.GET, url,
            { response ->
                try {
                    val correctedResponse = String(response.toByteArray(Charsets.ISO_8859_1), Charsets.UTF_8)
                    val fileOutputStream: FileOutputStream = openFileOutput(FILE_NAME, Context.MODE_PRIVATE)
                    fileOutputStream.write(correctedResponse.toByteArray(StandardCharsets.UTF_8))
                    fileOutputStream.close()
                    loadProductsToMemoryBackground()
                    Toast.makeText(this, "تم التحديث بنجاح!", Toast.LENGTH_LONG).show()
                } catch (e: Exception) { txtStatus.text = "خطأ: ${e.message}" }
            },
            { error -> Toast.makeText(this, "فشل الاتصال: ${error.message}", Toast.LENGTH_LONG).show() }) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Cache-Control"] = "no-cache"
                headers["Content-Type"] = "text/csv; charset=utf-8"
                return headers
            }
        }
        queue.add(stringRequest)
    }

    private fun setupBranchButtons(deptName: String) {
        val branches = allProductsList.filter { it.department.equals(deptName, ignoreCase = true) }
            .map { it.branch.trim() }.filter { it.isNotEmpty() && it != "عام" }.distinct().sorted()

        if (branches.isEmpty()) {
            scrollBranches.visibility = View.GONE
            return
        }
        scrollBranches.visibility = View.VISIBLE
        layoutBranchesContainer.removeAllViews()
        addBranchButton("الكل", deptName, true)
        for (branch in branches) addBranchButton(branch, deptName, false)
    }

    private fun addBranchButton(branchName: String, deptName: String, isAll: Boolean) {
        val btn = Button(this)
        btn.text = branchName
        btn.textSize = 14f
        btn.minHeight = 0
        btn.minimumHeight = 0
        btn.setPadding(40, 20, 40, 20)
        val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        params.setMargins(8, 0, 8, 0)
        btn.layoutParams = params
        btn.stateListAnimator = null
        val gradientDrawable = GradientDrawable()
        gradientDrawable.shape = GradientDrawable.RECTANGLE
        gradientDrawable.cornerRadius = 50f
        if (isAll) {
            gradientDrawable.setColor(Color.parseColor("#1565C0"))
            btn.setTextColor(Color.WHITE)
        } else {
            gradientDrawable.setColor(Color.WHITE)
            gradientDrawable.setStroke(2, Color.parseColor("#E0E0E0"))
            btn.setTextColor(Color.parseColor("#37474F"))
        }
        btn.background = gradientDrawable
        btn.setOnClickListener {
            if (isAll) {
                filterByDepartment(deptName)
            } else {
                filterByBranch(deptName, branchName)
                resetBranchButtonsColors()
                val selectedBg = GradientDrawable()
                selectedBg.shape = GradientDrawable.RECTANGLE
                selectedBg.cornerRadius = 50f
                selectedBg.setColor(Color.parseColor("#FF9800"))
                btn.background = selectedBg
                btn.setTextColor(Color.WHITE)
            }
        }
        layoutBranchesContainer.addView(btn)
    }

    private fun filterByBranch(deptName: String, branchName: String) {
        currentFilteredList = allProductsList.filter {
            it.department.equals(deptName, ignoreCase = true) &&
                    it.branch.equals(branchName, ignoreCase = true)
        } as ArrayList<Product>
        txtStatus.text = "$deptName > $branchName (${currentFilteredList.size})"
        updateListView(currentFilteredList)
    }

    private fun resetBranchButtonsColors() {
        for (i in 0 until layoutBranchesContainer.childCount) {
            val view = layoutBranchesContainer.getChildAt(i)
            if (view is Button) {
                val bg = GradientDrawable()
                bg.shape = GradientDrawable.RECTANGLE
                bg.cornerRadius = 50f
                if (view.text == "الكل") {
                    bg.setColor(Color.parseColor("#1565C0"))
                    view.setTextColor(Color.WHITE)
                } else {
                    bg.setColor(Color.WHITE)
                    bg.setStroke(2, Color.parseColor("#E0E0E0"))
                    view.setTextColor(Color.parseColor("#37474F"))
                }
                view.background = bg
            }
        }
    }

    private fun fetchThemeFromServer() {
        val url = Config.APPS_SCRIPT_URL
        val queue = Volley.newRequestQueue(this)
        val request = object : StringRequest(Request.Method.POST, url,
            { response ->
                try {
                    val prefs = getSharedPreferences("KMS_Theme", Context.MODE_PRIVATE)
                    prefs.edit().putString("theme_data", response).apply()
                    applySavedTheme()
                } catch (e: Exception) {}
            }, { }) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["action"] = "getTheme"
                return params
            }
        }
        queue.add(request)
    }

    private fun applySavedTheme() {
        val prefs = getSharedPreferences("KMS_Theme", Context.MODE_PRIVATE)
        val jsonString = prefs.getString("theme_data", "{}")
        if (jsonString == "{}" || jsonString == null) return
        try {
            val theme = org.json.JSONObject(jsonString)
            if (theme.has("logoUrl")) {
                val url = theme.getString("logoUrl")
                if (url.isNotEmpty()) {
                    val imgHeader = findViewById<android.widget.ImageView>(R.id.img_header_logo)
                    if (imgHeader != null) {
                        Glide.with(this).load(url).fitCenter().placeholder(R.drawable.company_logo).into(imgHeader)
                    }
                    val navView = findViewById<NavigationView>(R.id.nav_view)
                    if (navView != null && navView.headerCount > 0) {
                        val headerView = navView.getHeaderView(0)
                        val imgNav = headerView.findViewById<android.widget.ImageView>(R.id.img_nav_header_logo)
                        if (imgNav != null) {
                            Glide.with(this).load(url).fitCenter().placeholder(R.drawable.company_logo).into(imgNav)
                        }
                    }
                }
            }
            if (theme.has("headerColor")) {
                val colorCode = theme.getString("headerColor")
                try {
                    val color = Color.parseColor(colorCode)
                    val btnScan = findViewById<ExtendedFloatingActionButton>(R.id.btn_scan)
                    if (btnScan != null) btnScan.backgroundTintList = android.content.res.ColorStateList.valueOf(color)
                } catch (e: Exception) {}
            }
            val imgHeader = findViewById<android.widget.ImageView>(R.id.img_header_logo)
            imgHeader?.post {
                if (theme.has("logoAnim")) {
                    val animType = theme.getString("logoAnim")
                    LogoAnimator.applyAnimation(imgHeader, animType)
                }
            }
        } catch (e: Exception) {}
    }

    override fun dispatchTouchEvent(ev: android.view.MotionEvent?): Boolean {
        if (ev?.action == android.view.MotionEvent.ACTION_DOWN) {
            val prefs = getSharedPreferences("KMS_Theme", Context.MODE_PRIVATE)
            val jsonString = prefs.getString("theme_data", "{}")
            var effectType = ""
            try {
                val theme = org.json.JSONObject(jsonString)
                if (theme.has("touchEffect")) effectType = theme.getString("touchEffect")
            } catch (e: Exception) {}

            if (effectType.isNotEmpty()) {
                if (effectOverlay == null) effectOverlay = findViewById(R.id.effect_overlay)
                if (effectOverlay != null) showThemeEffect(ev.rawX, ev.rawY, effectType)
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun showThemeEffect(x: Float, y: Float, type: String) {
        val iconRes = when (type.lowercase().trim()) {
            "snow" -> R.drawable.ic_effect_snow
            "ramadan" -> R.drawable.ic_effect_ramadan
            "fanoos" -> R.drawable.ic_effect_fanoos
            "heart" -> R.drawable.ic_effect_heart
            else -> return
        }
        val random = java.util.Random()
        val location = IntArray(2)
        effectOverlay!!.getLocationOnScreen(location)
        val overlayX = location[0].toFloat()
        val overlayY = location[1].toFloat()
        for (i in 0..7) {
            val particle = android.widget.ImageView(this)
            particle.setImageResource(iconRes)
            val size = 40 + random.nextInt(30)
            val params = FrameLayout.LayoutParams(size, size)
            params.gravity = Gravity.TOP or Gravity.START
            particle.layoutParams = params
            particle.x = (x - overlayX) - (size / 2)
            particle.y = (y - overlayY) - (size / 2)
            effectOverlay!!.addView(particle)
            val destX = particle.x + (random.nextInt(300) - 150)
            val destY = particle.y + (random.nextInt(400) + 100)
            particle.animate().x(destX).y(destY).rotation(random.nextInt(360).toFloat()).alpha(0f)
                .setDuration(800 + random.nextInt(400).toLong())
                .withEndAction { effectOverlay!!.removeView(particle) }.start()
        }
    }
    // ---------------------------------------------------------
    // ✅ وظيفة تأكيد الخروج من التطبيق
    // ---------------------------------------------------------
    private fun showExitConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("تأكيد الخروج")
            .setMessage("هل أنت متأكد أنك تريد إغلاق التطبيق؟")
            .setPositiveButton("نعم") { _, _ ->
                finishAffinity() // هذا الأمر يغلق التطبيق بالكامل ويخرج منه
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }
}