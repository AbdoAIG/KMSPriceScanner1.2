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

    private val bannerImages = ArrayList<Any>()
    private val sliderHandler = Handler(Looper.getMainLooper())
    private lateinit var sliderRunnable: Runnable

    private var ignoreSearchWatcher = false
    private var searchJob: Job? = null

    private var currentDepartmentProducts = ArrayList<ProductEntity>()
    private var productAdapter: ProductRecyclerAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        initViews()
        setupBannerSlider()
        setupDrawerFooter()
        setupDrawerInteractions()

        applySavedTheme()
        loadCachedBanners()

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

        recyclerViewProducts.layoutManager = LinearLayoutManager(this)
    }

    private fun observeViewModel() {
        // ✅ هذا هو النمط المعماري المتطور: معالجة حالة الشاشة رياضياً
        viewModel.uiState.observe(this) { state ->
            when (state) {
                is ProductUiState.Loading -> {
                    etSearch.isEnabled = false
                    btnScan.isEnabled = false
                    etSearch.hint = "جاري التحميل..."
                    txtStatus.text = "جاري الاتصال وتحديث البيانات..."
                    txtStatus.setTextColor(Color.parseColor("#1565C0")) // لون أزرق للتحميل
                }
                is ProductUiState.Empty -> {
                    etSearch.isEnabled = true
                    btnScan.isEnabled = true
                    etSearch.hint = "ابحث عن منتج..."
                    updateListView(emptyList()) // تفريغ الشاشة
                    txtStatus.text = state.message
                    txtStatus.setTextColor(Color.parseColor("#2E7D32")) // لون أخضر داكن
                }
                is ProductUiState.Success -> {
                    etSearch.isEnabled = true
                    btnScan.isEnabled = true
                    etSearch.hint = "ابحث عن منتج..."
                    updateListView(state.products)
                    txtStatus.text = state.statusMessage
                    txtStatus.setTextColor(Color.parseColor("#008000")) // لون أخضر فاتح (نجاح)
                }
                is ProductUiState.Error -> {
                    etSearch.isEnabled = true
                    btnScan.isEnabled = true
                    etSearch.hint = "ابحث عن منتج..."
                    updateListView(emptyList())
                    txtStatus.text = state.errorMessage
                    txtStatus.setTextColor(Color.RED) // لون أحمر (للأخطاء)
                }
            }
        }

        viewModel.departments.observe(this) { depts ->
            populateSideMenu(depts)
        }

        viewModel.bannerUrls.observe(this) { urls ->
            bannerImages.clear()
            bannerImages.addAll(urls)
            bannerViewPager.adapter?.notifyDataSetChanged()
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
            if (selectedDept == "عرض الكل") {
                ignoreSearchWatcher = true
                etSearch.setText("")
                ignoreSearchWatcher = false

                viewModel.loadAllProducts()
                scrollBranches.visibility = View.GONE
                cardBannerContainer.visibility = View.VISIBLE
            } else {
                viewModel.filterByDepartment(selectedDept)
                cardBannerContainer.visibility = View.GONE

                lifecycleScope.launch {
                    val db = AppDatabase.getDatabase(this@MainActivity)
                    val repo = ProductRepository(db.productDao())
                    currentDepartmentProducts = ArrayList(repo.getProductsByDepartment(selectedDept))
                    setupBranchButtons(selectedDept)
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun setupSearchBox() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.isNullOrEmpty()) {
                    etSearch.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
                } else {
                    etSearch.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, android.R.drawable.ic_menu_close_clear_cancel, 0)
                }

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
                    val touchArea = 30
                    val isRtl = view.layoutDirection == View.LAYOUT_DIRECTION_RTL
                    val isHit = if (isRtl) {
                        event.x <= (etSearch.compoundPaddingLeft + touchArea)
                    } else {
                        event.x >= (etSearch.width - etSearch.compoundPaddingRight - touchArea)
                    }
                    if (isHit) {
                        etSearch.setText("")
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

        // إخفاء لوحة المفاتيح إذا كانت مفتوحة
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(etSearch.windowToken, 0)

        // تفريغ مربع البحث بذكاء لتجنب تفعيل دالة البحث التلقائي
        ignoreSearchWatcher = true
        etSearch.setText("")
        ignoreSearchWatcher = false

        // إخفاء البانرات وأزرار الأقسام
        scrollBranches.visibility = View.GONE
        cardBannerContainer.visibility = View.GONE

        // ✅ توجيه الأمر للعقل المدبر (ViewModel) ليقوم بالعمل الثقيل في الخلفية
        viewModel.loadHistoryProducts(historyBarcodes)
    }

    private fun openProductDetailsPage(p: ProductEntity) {
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
            val enteredHash = HashHelper.sha256(input.text.toString())
            if (enteredHash == Config.ADMIN_PASSWORD_HASH) {
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
                viewModel.updateDatabaseFromServer()
                viewModel.loadServerData()
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun setupBranchButtons(deptName: String) {
        val branches = currentDepartmentProducts.map { it.branch.trim() }
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
            if (isAll) {
                updateListView(currentDepartmentProducts)
                txtStatus.text = "قسم: $deptName (${currentDepartmentProducts.size})"
            } else {
                val branchFiltered = currentDepartmentProducts.filter { it.branch.equals(branchName, ignoreCase = true) }
                updateListView(branchFiltered)
                txtStatus.text = "$deptName > $branchName (${branchFiltered.size})"

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
    }

    private fun loadCachedBanners() {
        val prefs = getSharedPreferences("KMS_Prefs", Context.MODE_PRIVATE)
        val cachedString = prefs.getString("cached_banners", null)
        bannerImages.clear()
        if (!cachedString.isNullOrEmpty()) {
            val urls = cachedString.split(",")
            for (url in urls) {
                if (url.trim().isNotEmpty()) bannerImages.add(url.trim())
            }
        } else {
            bannerImages.add(R.drawable.company_logo)
        }
        bannerViewPager.adapter?.notifyDataSetChanged()
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
            "KmsDailySync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
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
}