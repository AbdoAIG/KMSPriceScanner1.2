package com.kms.kmspricescanner

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
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
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.kms.kmspricescanner.databinding.ActivityMainBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: MainViewModel
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
    private lateinit var recyclerViewProducts: RecyclerView
    private lateinit var scrollBranches: HorizontalScrollView
    private lateinit var layoutBranchesContainer: LinearLayout
    private lateinit var cardBannerContainer: androidx.cardview.widget.CardView
    private lateinit var bannerViewPager: ViewPager2
    private lateinit var btnFilter: TextView

    private var ignoreSearchWatcher = false
    private var searchJob: Job? = null
    private var currentDepartmentProducts = ArrayList<ProductEntity>()
    private var productAdapter: ProductRecyclerAdapter? = null
    private var activeBranchFilter: String = "الكل"
    private var currentSelectedDept: String = "عرض الكل"

    private lateinit var binding: ActivityMainBinding

    // ✅ الاعتماد على مدير البانر المستقل لتقليل حجم هذه الشاشة
    private lateinit var bannerManager: BannerManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        CartManager.init(this)

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        initViews()

        // ✅ تشغيل مدير البانر النظيف
        bannerManager = BannerManager(this, bannerViewPager)
        bannerManager.loadCachedBanners()
        bannerManager.startSlider()

        setupDrawerFooter()
        setupDrawerInteractions()

        applySavedTheme()

        viewModel.loadServerData()

        observeViewModel()
        setupClickListeners()
        setupSearchBox()

        setupBackgroundSync()
    }

    private fun initViews() {
        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
        effectOverlay = findViewById(R.id.effect_overlay)
        btnMenu = findViewById(R.id.btn_menu)
        btnHistory = findViewById(R.id.btn_history)
        btnOpenCart = findViewById(R.id.btn_open_cart)
        etSearch = findViewById(R.id.et_search)
        btnScan = findViewById(R.id.btn_scan)
        btnUpdate = findViewById(R.id.btn_update_admin)
        txtStatus = findViewById(R.id.txt_status)
        recyclerViewProducts = findViewById(R.id.recycler_view_products)
        scrollBranches = findViewById(R.id.scroll_branches)
        layoutBranchesContainer = findViewById(R.id.layout_branches_container)
        cardBannerContainer = findViewById(R.id.card_banner_container)
        btnFilter = findViewById(R.id.btnFilter)
        bannerViewPager = findViewById(R.id.banner_view_pager)

        recyclerViewProducts.layoutManager = LinearLayoutManager(this)
    }

    private fun updateFilterButtonVisibility() {
        val isSearchEmpty = etSearch.text.isNullOrEmpty()
        if (isSearchEmpty && currentSelectedDept == "عرض الكل") {
            btnFilter.visibility = View.GONE
        } else {
            btnFilter.visibility = View.VISIBLE
        }
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(this) { state ->
            when (state) {
                is ProductUiState.Loading -> {
                    etSearch.isEnabled = false
                    btnScan.isEnabled = false
                    etSearch.hint = "جاري التحميل..."
                    txtStatus.text = "جاري الاتصال وتحديث البيانات..."
                    txtStatus.setTextColor(Color.parseColor("#1565C0"))
                }
                is ProductUiState.Empty -> {
                    etSearch.isEnabled = true
                    btnScan.isEnabled = true
                    etSearch.hint = "ابحث عن منتج..."
                    updateListView(emptyList())
                    txtStatus.text = state.message
                    txtStatus.setTextColor(Color.parseColor("#2E7D32"))
                }
                is ProductUiState.Success -> {
                    etSearch.isEnabled = true
                    btnScan.isEnabled = true
                    etSearch.hint = "ابحث عن منتج..."
                    currentDepartmentProducts = ArrayList(state.products)
                    if (activeBranchFilter != "الكل") {
                        val branchFiltered = currentDepartmentProducts.filter { it.branch.equals(activeBranchFilter, ignoreCase = true) }
                        updateListView(branchFiltered)
                        txtStatus.text = "مفلتر > $activeBranchFilter (${branchFiltered.size})"
                    } else {
                        updateListView(state.products)
                        txtStatus.text = state.statusMessage
                    }
                    txtStatus.setTextColor(Color.parseColor("#008000"))
                }
                is ProductUiState.Error -> {
                    etSearch.isEnabled = true
                    btnScan.isEnabled = true
                    etSearch.hint = "ابحث عن منتج..."
                    updateListView(emptyList())
                    txtStatus.text = state.errorMessage
                    txtStatus.setTextColor(Color.RED)
                }
            }
        }

        viewModel.departments.observe(this) { depts ->
            populateSideMenu(depts)
        }

        viewModel.bannerUrls.observe(this) { urls ->
            // ✅ تمرير التحديث لمدير البانر
            bannerManager.updateBanners(urls)
        }

        viewModel.themeData.observe(this) {
            applySavedTheme()
        }
    }

    private fun setupClickListeners() {
        btnMenu.setOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) drawerLayout.closeDrawer(GravityCompat.START)
            else drawerLayout.openDrawer(GravityCompat.START)
        }

        btnHistory.setOnClickListener { showScanHistory() }
        btnUpdate.setOnClickListener { showConfirmationDialog() }
        btnOpenCart.setOnClickListener { startActivity(Intent(this, CartActivity::class.java)) }
        btnScan.setOnClickListener { scanCode() }

        navView.setNavigationItemSelectedListener { menuItem ->
            val selectedDept = menuItem.title.toString()
            currentSelectedDept = selectedDept
            activeBranchFilter = "الكل"

            ignoreSearchWatcher = true
            etSearch.setText("")
            ignoreSearchWatcher = false

            if (selectedDept == "عرض الكل") {
                viewModel.loadAllProducts()
                scrollBranches.visibility = View.GONE
                cardBannerContainer.visibility = View.VISIBLE
            } else {
                viewModel.filterByDepartment(selectedDept)
                cardBannerContainer.visibility = View.GONE

                lifecycleScope.launch {
                    val db = AppDatabase.getDatabase(this@MainActivity)
                    val repo = ProductRepository(db.productDao())
                    val rawDeptProducts = repo.getProductsByDepartment(selectedDept)
                    setupBranchButtons(selectedDept, rawDeptProducts)
                }
            }

            updateFilterButtonVisibility()
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun setupSearchBox() {
        btnFilter.setOnClickListener {
            showFilterDialog()
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.isNullOrEmpty()) {
                    etSearch.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
                } else {
                    etSearch.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, android.R.drawable.ic_menu_close_clear_cancel, 0)
                    currentSelectedDept = "عرض الكل"
                }

                updateFilterButtonVisibility()

                if (ignoreSearchWatcher) return

                searchJob?.cancel()
                searchJob = lifecycleScope.launch {
                    delay(150)
                    if (s.isNullOrEmpty()) {
                        scrollBranches.visibility = View.GONE
                        cardBannerContainer.visibility = View.VISIBLE
                    } else {
                        cardBannerContainer.visibility = View.GONE
                    }
                    viewModel.searchProducts(s.toString())
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        etSearch.setOnTouchListener { view, event ->
            if (event.action == android.view.MotionEvent.ACTION_UP) {
                if (etSearch.compoundDrawablesRelative[2] != null) {
                    val touchArea = 70
                    val isRtl = view.layoutDirection == View.LAYOUT_DIRECTION_RTL
                    val isClearHit = if (isRtl) {
                        event.x <= (etSearch.compoundPaddingLeft + touchArea)
                    } else {
                        event.x >= (etSearch.width - etSearch.compoundPaddingRight - touchArea)
                    }

                    if (isClearHit) {
                        etSearch.setText("")
                        viewModel.applyFilters(null, null, SortOrder.NONE, "")
                        updateFilterButtonVisibility()
                        return@setOnTouchListener true
                    }
                }
            }
            false
        }
    }

    private fun populateSideMenu(departments: List<String>) {
        val menu = navView.menu
        menu.clear()
        val itemAll = menu.add(0, 0, 0, "عرض الكل")
        itemAll.setIcon(android.R.drawable.ic_menu_sort_by_size)

        for ((index, dept) in departments.withIndex()) {
            menu.add(1, index + 1, index + 1, dept).setIcon(android.R.drawable.star_off)
        }
        navView.invalidate()
    }

    private fun updateListView(products: List<ProductEntity>) {
        if (productAdapter == null) {
            productAdapter = ProductRecyclerAdapter(products) { selectedProduct ->
                openProductDetailsPage(selectedProduct)
            }
            recyclerViewProducts.adapter = productAdapter
        } else {
            productAdapter?.updateData(products)
        }
    }

    private fun scanCode() {
        val intent = Intent(this, ScannerActivity::class.java)
        scannerLauncher.launch(intent)
    }

    private val scannerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val scannedCode = result.data?.getStringExtra("scanned_code")
            if (scannedCode != null) {
                showAddProductDialog(scannedCode)
            }
        }
    }

    private fun showScanHistory() {
        val historyBarcodes = ScanHistoryHelper.getHistory(this)
        if (historyBarcodes.isEmpty()) {
            Toast.makeText(this, "لم يتم فتح أي منتجات بعد", Toast.LENGTH_SHORT).show()
            return
        }

        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(etSearch.windowToken, 0)

        ignoreSearchWatcher = true
        etSearch.setText("")
        ignoreSearchWatcher = false

        scrollBranches.visibility = View.GONE
        cardBannerContainer.visibility = View.GONE
        currentSelectedDept = "عرض الكل"
        updateFilterButtonVisibility()

        viewModel.loadHistoryProducts(historyBarcodes)
    }

    private fun openProductDetailsPage(p: ProductEntity) {
        val validBarcode = if (p.localBarcode.isNotBlank()) p.localBarcode else p.globalBarcode
        ScanHistoryHelper.saveProduct(this, validBarcode)

        val intent = Intent(this, ProductDetailsActivity::class.java).apply {
            putExtra("name", p.name)
            putExtra("pricePiece", p.pricePiece)
            putExtra("priceDozen", p.priceDozen)
            putExtra("priceCarton", p.priceCarton)
            putExtra("localBarcode", p.localBarcode)
            putExtra("globalBarcode", p.globalBarcode)
            putExtra("department", p.department)
            putExtra("branch", p.branch)
            putExtra("imageUrl", p.imageUrl)
            putExtra("stock", p.stock)
        }
        startActivity(intent)
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
        AdminSecurityManager.showAuthDialog(this) {
            val intent = Intent(this, AddProductActivity::class.java)
            intent.putExtra("scanned_code", scannedCode)
            startActivity(intent)
        }
    }

    private fun showConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("تحديث البيانات")
            .setMessage("هل تريد تحميل أحدث الأسعار من السيرفر؟")
            .setPositiveButton("نعم") { _, _ ->
                viewModel.updateDatabaseFromServer()
                viewModel.loadServerData()
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun setupBranchButtons(deptName: String, rawProducts: List<ProductEntity>) {
        val branches = rawProducts.map { it.branch.trim() }
            .filter { it.isNotEmpty() && it != "عام" }.distinct().sorted()

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
            activeBranchFilter = branchName
            if (isAll) {
                updateListView(currentDepartmentProducts)
                txtStatus.text = "قسم: $deptName (${currentDepartmentProducts.size})"
            } else {
                val branchFiltered = currentDepartmentProducts.filter { it.branch.equals(branchName, ignoreCase = true) }
                updateListView(branchFiltered)
                txtStatus.text = "$deptName > $branchName (${branchFiltered.size})"
            }

            resetBranchButtonsColors()
            val selectedBg = GradientDrawable()
            selectedBg.shape = GradientDrawable.RECTANGLE
            selectedBg.cornerRadius = 50f
            selectedBg.setColor(Color.parseColor("#FF9800"))
            btn.background = selectedBg
            btn.setTextColor(Color.WHITE)
        }
        layoutBranchesContainer.addView(btn)
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
                    imgHeader?.let {
                        Glide.with(this).load(url).diskCacheStrategy(DiskCacheStrategy.ALL).thumbnail(0.1f).fitCenter().placeholder(R.drawable.company_logo).into(it)
                    }
                    val navView = findViewById<NavigationView>(R.id.nav_view)
                    if (navView != null && navView.headerCount > 0) {
                        val headerView = navView.getHeaderView(0)
                        val imgNav = headerView.findViewById<android.widget.ImageView>(R.id.img_nav_header_logo)
                        imgNav?.let {
                            Glide.with(this).load(url).diskCacheStrategy(DiskCacheStrategy.ALL).thumbnail(0.1f).fitCenter().placeholder(R.drawable.company_logo).into(it)
                        }
                    }
                }
            }
            if (theme.has("headerColor")) {
                val colorCode = theme.getString("headerColor")
                try {
                    val color = Color.parseColor(colorCode)
                    val btnScan = findViewById<ExtendedFloatingActionButton>(R.id.btn_scan)
                    btnScan?.backgroundTintList = android.content.res.ColorStateList.valueOf(color)
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
                effectOverlay?.let {
                    it.alpha = 1f - slideOffset
                    it.visibility = if (slideOffset > 0.0f) View.INVISIBLE else View.VISIBLE
                }
            }
            override fun onDrawerOpened(drawerView: View) {
                super.onDrawerOpened(drawerView)
                try {
                    val devText = findViewById<TextView>(R.id.tv_developer_name)
                    devText?.let {
                        it.alpha = 0f
                        it.translationY = 50f
                        it.animate().alpha(1f).translationY(0f).setDuration(800).start()
                    }
                } catch (e: Exception) {}
            }
            override fun onDrawerClosed(drawerView: View) {
                super.onDrawerClosed(drawerView)
                effectOverlay?.let {
                    it.visibility = View.VISIBLE
                    it.alpha = 1f
                }
            }
        })
    }

    private fun setupBackgroundSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(12, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "KmsDailySync", ExistingPeriodicWorkPolicy.KEEP, syncRequest
        )
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
                effectOverlay?.let { showThemeEffect(ev.rawX, ev.rawY, effectType) }
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

    private fun showFilterDialog() {
        val currentFilters = viewModel.getFilters()
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 40, 60, 40)
        }
        val etMin = EditText(this).apply {
            hint = "الحد الأدنى للسعر (جنيه)"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            currentFilters.minPrice?.let { setText(it.toString()) }
        }
        val etMax = EditText(this).apply {
            hint = "الحد الأقصى للسعر (جنيه)"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            currentFilters.maxPrice?.let { setText(it.toString()) }
            val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            params.setMargins(0, 20, 0, 20)
            layoutParams = params
        }
        val titleSort = TextView(this).apply {
            text = "ترتيب النتائج:"
            textSize = 16f
            setTextColor(Color.BLACK)
            setPadding(0, 20, 0, 10)
        }
        val radioGroup = RadioGroup(this)
        val rbNone = RadioButton(this).apply { text = "الترتيب الافتراضي" }
        val rbAsc = RadioButton(this).apply { text = "السعر: من الأقل للأعلى" }
        val rbDesc = RadioButton(this).apply { text = "السعر: من الأعلى للأقل" }
        radioGroup.addView(rbNone)
        radioGroup.addView(rbAsc)
        radioGroup.addView(rbDesc)

        when (currentFilters.sortOrder) {
            SortOrder.PRICE_ASC -> rbAsc.isChecked = true
            SortOrder.PRICE_DESC -> rbDesc.isChecked = true
            SortOrder.NONE -> rbNone.isChecked = true
        }

        layout.addView(etMin)
        layout.addView(etMax)
        layout.addView(titleSort)
        layout.addView(radioGroup)

        AlertDialog.Builder(this)
            .setTitle("تصفية وترتيب البحث 🔍")
            .setView(layout)
            .setPositiveButton("تطبيق") { _, _ ->
                val min = etMin.text.toString().toDoubleOrNull()
                val max = etMax.text.toString().toDoubleOrNull()
                val sort = when (radioGroup.checkedRadioButtonId) {
                    rbAsc.id -> SortOrder.PRICE_ASC
                    rbDesc.id -> SortOrder.PRICE_DESC
                    else -> SortOrder.NONE
                }
                viewModel.applyFilters(min, max, sort, etSearch.text.toString())
                Toast.makeText(this, "تم تطبيق الفلتر بنجاح", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("إلغاء", null)
            .setNeutralButton("مسح الفلاتر") { _, _ ->
                viewModel.applyFilters(null, null, SortOrder.NONE, etSearch.text.toString())
                Toast.makeText(this, "تم مسح الفلاتر", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    // 🔥 ضمان إغلاق المهام المفتوحة لتجنب استنزاف البطارية وتسرب الذاكرة
    override fun onDestroy() {
        super.onDestroy()
        if (::bannerManager.isInitialized) {
            bannerManager.stopSlider()
        }
        searchJob?.cancel()
    }
}