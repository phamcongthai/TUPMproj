package com.example.jetpackcompose.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jetpackcompose.data.model.MenuItem
import com.example.jetpackcompose.data.repository.FoodRepository
import com.example.jetpackcompose.utils.Resource
import com.example.jetpackcompose.utils.UiState
import com.example.jetpackcompose.utils.notification.NotificationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MenuViewModel @Inject constructor(
    private val foodRepository: FoodRepository,
    private val notificationManager: NotificationManager
) : ViewModel() {

    private val _categoriesState = MutableStateFlow<UiState<List<String>>>(UiState.Loading)
    val categoriesState = _categoriesState.asStateFlow()

    private val _allMenuItems = MutableStateFlow<UiState<List<MenuItem>>>(UiState.Loading)

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val filteredMenuItems: StateFlow<UiState<List<MenuItem>>> = combine(
        _allMenuItems, 
        _selectedCategory,
        _searchQuery
    ) { state, category, query ->
        when (state) {
            is UiState.Success -> {
                var filtered = state.data
                if (category != "All") {
                    filtered = filtered.filter { it.category.equals(category, ignoreCase = true) }
                }
                if (query.isNotEmpty()) {
                    filtered = filtered.filter { it.name.contains(query, ignoreCase = true) }
                }
                UiState.Success(filtered)
            }
            else -> state
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    init {
        loadData()
    }

    private fun loadData() {
        fetchCategories()
        fetchMenuItems()
    }

    private fun fetchCategories() {
        viewModelScope.launch {
            foodRepository.getCategories().collect { resource ->
                _categoriesState.value = when (resource) {
                    is Resource.Success -> {
                        val list = mutableListOf("All")
                        list.addAll(resource.data ?: emptyList())
                        UiState.Success(list)
                    }
                    is Resource.Error -> {
                        val errorMsg = resource.message ?: "Không thể tải danh mục"
                        notificationManager.showError(errorMsg)
                        UiState.Error(errorMsg)
                    }
                    is Resource.Loading -> UiState.Loading
                }
            }
        }
    }

    private fun fetchMenuItems() {
        viewModelScope.launch {
            foodRepository.getMenuItems(null).collect { resource ->
                _allMenuItems.value = when (resource) {
                    is Resource.Success -> UiState.Success(resource.data ?: emptyList())
                    is Resource.Error -> {
                        val errorMsg = resource.message ?: "Không thể tải thực đơn"
                        notificationManager.showError(errorMsg)
                        UiState.Error(errorMsg)
                    }
                    is Resource.Loading -> UiState.Loading
                }
            }
        }
    }

    fun onCategorySelected(category: String) {
        _selectedCategory.value = category
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }
}
