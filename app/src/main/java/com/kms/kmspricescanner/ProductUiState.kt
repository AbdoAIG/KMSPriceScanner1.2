package com.kms.kmspricescanner

// كلاس مغلق (Sealed Class) يمثل حالات الشاشة بدقة شديدة
sealed class ProductUiState {
    object Loading : ProductUiState()
    data class Empty(val message: String) : ProductUiState()
    data class Success(val products: List<ProductEntity>, val statusMessage: String) : ProductUiState()
    data class Error(val errorMessage: String) : ProductUiState()
}