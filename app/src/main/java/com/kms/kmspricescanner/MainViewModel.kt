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

enum class SortOrder { NONE, PRICE_ASC, PRICE_DESC }
data class SearchFilterState(
    val minPrice: Double? = null,
    val maxPrice: Double? = null,
    val sortOrder: SortOrder = SortOrder.NONE
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ProductRepository

    private val _uiState = MutableLiveData<ProductUiState>()
    val uiState: LiveData<ProductUiState> = _uiState

    private val _departments = MutableLiveData<List<String>>()
    val departments: LiveData<List<String>> = _departments

    private val _bannerUrls = MutableLiveData<List<String>>()
    val bannerUrls: LiveData<List<String>> = _bannerUrls

    private val _themeData = MutableLiveData<String>()
    val themeData: LiveData<String> = _themeData

    private var totalProductsCount = 0
    private var currentFilter = SearchFilterState()
    private var currentDepartment = ""

    init {
        val productDao = AppDatabase.getDatabase(application).productDao()
        repository = ProductRepository(productDao)
        initializeDatabase()
    }

    private fun extractPriceFast(priceStr: String): Double {
        if (priceStr.isEmpty()) return 0.0
        val sb = java.lang.StringBuilder()
        for (i in 0 until priceStr.length) {
            val c = priceStr[i]
            if (c in '0'..'9' || c == '.') {
                sb.append(c)
            } else if (c in '٠'..'٩') {
                sb.append(c - '٠')
            }
        }
        return sb.toString().toDoubleOrNull() ?: 0.0
    }

    private fun applyFiltersToList(list: List<ProductEntity>): List<ProductEntity> {
        if (currentFilter.minPrice == null && currentFilter.maxPrice == null && currentFilter.sortOrder == SortOrder.NONE) {
            return list
        }

        var processedList = list.map { it to extractPriceFast(it.pricePiece) }

        if (currentFilter.minPrice != null || currentFilter.maxPrice != null) {
            val min = currentFilter.minPrice ?: 0.0
            val max = currentFilter.maxPrice ?: Double.MAX_VALUE
            processedList = processedList.filter { it.second in min..max }
        }

        processedList = when (currentFilter.sortOrder) {
            SortOrder.PRICE_ASC -> processedList.sortedBy { it.second }
            SortOrder.PRICE_DESC -> processedList.sortedByDescending { it.second }
            SortOrder.NONE -> processedList
        }

        return processedList.map { it.first }
    }

    fun applyFilters(min: Double?, max: Double?, sort: SortOrder, currentQuery: String) {
        currentFilter = SearchFilterState(min, max, sort)
        if (currentQuery.isNotEmpty()) {
            searchProducts(currentQuery)
        } else if (currentDepartment.isNotEmpty()) {
            getProductsByDepartment(currentDepartment)
        } else {
            loadAllProducts()
        }
    }

    fun getFilters(): SearchFilterState = currentFilter

    private fun initializeDatabase() {
        viewModelScope.launch {
            _uiState.value = ProductUiState.Loading
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
        _uiState.value = ProductUiState.Empty("العدد الكلي: $totalProductsCount صنف\nيرجى البحث أو اختيار قسم من القائمة")
    }

    fun loadAllProducts() {
        currentDepartment = ""
        viewModelScope.launch {
            val all = repository.getAllProducts()
            val finalResults = applyFiltersToList(all)
            _uiState.value = ProductUiState.Success(finalResults, "العدد الكلي: ${finalResults.size} صنف (مفلتر)")
        }
    }

    private var searchJob: kotlinx.coroutines.Job? = null

    fun searchProducts(query: String) {
        searchJob?.cancel()
        currentDepartment = ""

        searchJob = viewModelScope.launch {
            kotlinx.coroutines.delay(250) // للحد من الإرسال السريع جداً أثناء الكتابة

            // ✅ تبسيط البحث: نستخدم نفس أداة التوحيد الذكية على الكلمة التي يكتبها الموظف
            var normalizedQuery = query.trim()
                .replace('٠', '0').replace('١', '1').replace('٢', '2')
                .replace('٣', '3').replace('٤', '4').replace('٥', '5')
                .replace('٦', '6').replace('٧', '7').replace('٨', '8')
                .replace('٩', '9')
                .normalizeArabicForSearch() // استدعاء الأداة السحرية
                .lowercase()

            val isBarcode = normalizedQuery.any { it.isDigit() } && !normalizedQuery.any { it.isLetter() }

            if (isBarcode) {
                val digitsOnly = normalizedQuery.filter { it.isDigit() }
                if (digitsOnly.isNotEmpty()) {
                    normalizedQuery = digitsOnly.trimStart('0')
                    if (normalizedQuery.isEmpty()) normalizedQuery = "0"
                }
            }

            if (normalizedQuery.isEmpty()) {
                showEmptyScreen()
            } else {
                val rawResults = withContext(Dispatchers.IO) {
                    repository.searchProducts(normalizedQuery) // يرسل الكلمة المبسطة للقاعدة السريعة
                }
                val finalResults = applyFiltersToList(rawResults)

                if (finalResults.isNotEmpty()) {
                    _uiState.value = ProductUiState.Success(finalResults, "تم العثور على (${finalResults.size}) نتيجة")
                } else {
                    _uiState.value = ProductUiState.Error("لا توجد نتائج مطابقة للبحث أو الفلتر")
                }
            }
        }
    }

    fun filterByDepartment(deptName: String) {
        getProductsByDepartment(deptName)
    }

    fun getProductsByDepartment(deptName: String) {
        currentDepartment = deptName

        viewModelScope.launch {
            val rawResults = withContext(Dispatchers.IO) {
                repository.getProductsByDepartment(deptName)
            }

            val finalResults = applyFiltersToList(rawResults)

            if (finalResults.isNotEmpty()) {
                _uiState.value = ProductUiState.Success(
                    finalResults,
                    "تم العثور على (${finalResults.size}) منتج"
                )
            } else {
                _uiState.value = ProductUiState.Error("لا توجد منتجات مطابقة للفلتر في هذا القسم")
            }
        }
    }

    fun loadHistoryProducts(historyBarcodes: List<String>) {
        currentDepartment = ""
        viewModelScope.launch {
            _uiState.value = ProductUiState.Loading

            if (historyBarcodes.isEmpty()) {
                _uiState.value = ProductUiState.Success(emptyList<ProductEntity>(), "سجل آخر العمليات (0)")
                return@launch
            }

            val historyProducts = withContext(Dispatchers.IO) {
                repository.getProductsForHistory(historyBarcodes)
            }

            val finalResults = applyFiltersToList(historyProducts)

            if (finalResults.isEmpty()) {
                _uiState.value = ProductUiState.Empty("لا توجد منتجات في السجل تطابق الفلتر")
            } else {
                _uiState.value = ProductUiState.Success(finalResults, "سجل آخر العمليات (${finalResults.size})")
            }
        }
    }

    fun updateDatabaseFromServer() {
        viewModelScope.launch {
            _uiState.value = ProductUiState.Loading
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

                // ✅ تم إصلاح الخطأ بتحديد نوع القائمة <ProductEntity>
                _uiState.value = ProductUiState.Success(emptyList<ProductEntity>(), "تم التحديث بنجاح! العدد الكلي: ${allProducts.size} صنف\nيرجى البحث أو اختيار قسم")
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
}