package com.kms.kmspricescanner

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ProductRepository

    // ✅ الترقية العظمى: مصدر الحقيقة الوحيد (Single Source of Truth) للشاشة
    private val _uiState = MutableLiveData<ProductUiState>()
    val uiState: LiveData<ProductUiState> = _uiState

    private val _departments = MutableLiveData<List<String>>()
    val departments: LiveData<List<String>> = _departments

    private val _bannerUrls = MutableLiveData<List<String>>()
    val bannerUrls: LiveData<List<String>> = _bannerUrls

    private val _themeData = MutableLiveData<String>()
    val themeData: LiveData<String> = _themeData

    private var totalProductsCount = 0

    init {
        val productDao = AppDatabase.getDatabase(application).productDao()
        repository = ProductRepository(productDao)
        initializeDatabase()
    }

    private fun initializeDatabase() {
        viewModelScope.launch {
            _uiState.value = ProductUiState.Loading // إرسال حالة التحميل للشاشة

            var allProducts = repository.getAllProducts()

            if (allProducts.isEmpty()) {
                _uiState.value = ProductUiState.Empty("جاري تهيئة قاعدة البيانات لأول مرة، يرجى الانتظار...")
                withContext(Dispatchers.IO) {
                    repository.refreshDatabaseFromCsv(getApplication(), "kmsprices.csv")
                }
                allProducts = repository.getAllProducts()
            }

            totalProductsCount = allProducts.size
            _departments.value = allProducts.map { it.department.trim() }
                .filter { it.isNotEmpty() && !it.contains("http") }.distinct().sorted()

            showEmptyScreen()
        }
    }

    fun showEmptyScreen() {
        _uiState.value = ProductUiState.Empty("العدد الكلي: $totalProductsCount صنف")
    }

    fun loadAllProducts() {
        viewModelScope.launch {
            val all = repository.getAllProducts()
            _uiState.value = ProductUiState.Success(all, "العدد الكلي: ${all.size} صنف")
        }
    }

    fun searchProducts(query: String) {
        viewModelScope.launch {
            val normalizedQuery = query.trim()
                .replace('٠', '0').replace('١', '1').replace('٢', '2')
                .replace('٣', '3').replace('٤', '4').replace('٥', '5')
                .replace('٦', '6').replace('٧', '7').replace('٨', '8')
                .replace('٩', '9').lowercase()

            if (normalizedQuery.isEmpty()) {
                showEmptyScreen()
            } else {
                val searchResults = repository.searchProducts(normalizedQuery)
                if (searchResults.isNotEmpty()) {
                    _uiState.value = ProductUiState.Success(searchResults, "تم العثور على (${searchResults.size}) نتيجة")
                } else {
                    _uiState.value = ProductUiState.Error("لا توجد نتائج للبحث عن: $query")
                }
            }
        }
    }

    fun filterByDepartment(deptName: String) {
        viewModelScope.launch {
            val deptProducts = repository.getProductsByDepartment(deptName)
            _uiState.value = ProductUiState.Success(deptProducts, "قسم: $deptName (${deptProducts.size})")
        }
    }

    fun updateDatabaseFromServer() {
        viewModelScope.launch {
            _uiState.value = ProductUiState.Loading // الشاشة ستفهم فوراً وتغلق أزرار البحث

            val success = withContext(Dispatchers.IO) {
                val downloaded = repository.downloadAndSaveCsv(getApplication(), Config.CSV_URL, "kmsprices.csv")
                if (downloaded) {
                    repository.refreshDatabaseFromCsv(getApplication(), "kmsprices.csv")
                    true
                } else false
            }

            if (success) {
                val allProducts = repository.getAllProducts()
                totalProductsCount = allProducts.size
                _departments.value = allProducts.map { it.department.trim() }
                    .filter { it.isNotEmpty() && !it.contains("http") }.distinct().sorted()

                _uiState.value = ProductUiState.Success(emptyList(), "تم التحديث بنجاح! العدد الكلي: ${allProducts.size} صنف\nيرجى البحث أو اختيار قسم")
            } else {
                _uiState.value = ProductUiState.Error("فشل التحديث: يرجى التحقق من الاتصال بالإنترنت")
            }
        }
    }

    fun loadServerData() {
        viewModelScope.launch(Dispatchers.IO) {
            val bannerResponse = repository.fetchBanners()
            if (bannerResponse != null && !bannerResponse.contains("Error")) {
                val prefs = getApplication<Application>().getSharedPreferences("KMS_Prefs", Context.MODE_PRIVATE)
                prefs.edit().putString("cached_banners", bannerResponse).apply()
                val urls = bannerResponse.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                _bannerUrls.postValue(urls)
            }

            val themeResponse = repository.fetchTheme()
            if (themeResponse != null) {
                val prefs = getApplication<Application>().getSharedPreferences("KMS_Theme", Context.MODE_PRIVATE)
                prefs.edit().putString("theme_data", themeResponse).apply()
                _themeData.postValue(themeResponse)
            }
        }
    }
    // ✅ دالة جلب السجل باحترافية (خوارزمية HashMap السريعة)
    fun loadHistoryProducts(historyBarcodes: List<String>) {
        viewModelScope.launch {
            _uiState.value = ProductUiState.Loading // إظهار حالة التحميل فوراً

            // نقل العمليات الحسابية المعقدة إلى مسار الخلفية (IO Thread) لمنع تجميد الشاشة
            val historyProducts = withContext(Dispatchers.IO) {
                val allProducts = repository.getAllProducts()

                // تحويل القائمة إلى "قاموس" (HashMap) ليكون البحث لحظياً O(1) بدلاً من البحث البطيء O(N)
                val productsMap = allProducts.associateBy { it.localBarcode.trim() }

                // جلب المنتجات المطابقة فقط وإهمال الباركودات التي لم تعد موجودة
                historyBarcodes.mapNotNull { productsMap[it.trim()] }
            }

            if (historyProducts.isEmpty()) {
                _uiState.value = ProductUiState.Empty("لم يتم فتح أي منتجات بعد")
            } else {
                _uiState.value = ProductUiState.Success(
                    historyProducts,
                    "سجل آخر العمليات (${historyProducts.size})"
                )
            }
        }
    }
}