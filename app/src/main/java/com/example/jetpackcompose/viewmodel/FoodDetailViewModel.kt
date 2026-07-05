package com.example.jetpackcompose.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jetpackcompose.data.model.CartItem
import com.example.jetpackcompose.data.model.MenuItem
import com.example.jetpackcompose.data.model.Topping
import com.example.jetpackcompose.data.model.Variant
import com.example.jetpackcompose.data.repository.CartRepository
import com.example.jetpackcompose.data.repository.FoodRepository
import com.example.jetpackcompose.utils.Resource
import com.example.jetpackcompose.utils.UiState
import com.example.jetpackcompose.utils.notification.NotificationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FoodDetailViewModel @Inject constructor(
    private val foodRepository: FoodRepository,
    private val cartRepository: CartRepository,
    private val notificationManager: NotificationManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val menuId: String = checkNotNull(savedStateHandle["menuId"])

    private val _uiState = MutableStateFlow<UiState<MenuItem>>(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _quantity = MutableStateFlow(1)
    val quantity = _quantity.asStateFlow()

    private val _selectedVariant = MutableStateFlow<Variant?>(null)
    val selectedVariant = _selectedVariant.asStateFlow()

    private val _selectedToppings = MutableStateFlow<List<Topping>>(emptyList())
    val selectedToppings = _selectedToppings.asStateFlow()

    private val _addToCartEvent = MutableSharedFlow<Boolean>()
    val addToCartEvent = _addToCartEvent.asSharedFlow()

    init {
        fetchMenuItem()
    }

    private fun fetchMenuItem() {
        foodRepository.getMenuItem(menuId).onEach { resource ->
            when (resource) {
                is Resource.Loading -> _uiState.value = UiState.Loading
                is Resource.Success -> {
                    val item = resource.data!!
                    _uiState.value = UiState.Success(item)
                    if (item.variants.isNotEmpty()) {
                        _selectedVariant.value = item.variants.first()
                    }
                }
                is Resource.Error -> _uiState.value = UiState.Error(resource.message ?: "Error")
            }
        }.launchIn(viewModelScope)
    }

    fun incrementQuantity() {
        val item = (_uiState.value as? UiState.Success)?.data
        if (item?.status == "out_of_stock") return
        _quantity.value++
    }
    fun decrementQuantity() {
        val item = (_uiState.value as? UiState.Success)?.data
        if (item?.status == "out_of_stock") return
        if (_quantity.value > 1) _quantity.value--
    }

    fun selectVariant(variant: Variant) {
        val item = (_uiState.value as? UiState.Success)?.data
        if (item?.status == "out_of_stock") return
        _selectedVariant.value = variant
    }

    fun toggleTopping(topping: Topping) {
        val item = (_uiState.value as? UiState.Success)?.data
        if (item?.status == "out_of_stock") return
        val current = _selectedToppings.value.toMutableList()
        if (current.any { it.id == topping.id }) {
            current.removeAll { it.id == topping.id }
        } else {
            current.add(topping)
        }
        _selectedToppings.value = current
    }

    private fun resetInputs() {
        _quantity.value = 1
        val state = _uiState.value
        if (state is UiState.Success) {
            if (state.data.variants.isNotEmpty()) {
                _selectedVariant.value = state.data.variants.first()
            } else {
                _selectedVariant.value = null
            }
        }
        _selectedToppings.value = emptyList()
    }

    fun addToCart() {
        val state = _uiState.value
        if (state is UiState.Success) {
            val item = state.data
            if (item.status == "out_of_stock") {
                viewModelScope.launch {
                    notificationManager.showWarning("Món ăn hiện đã hết hàng.")
                }
                return
            }
            viewModelScope.launch {
                cartRepository.addToCart(
                    CartItem(
                        id = item.id,
                        name = item.name,
                        image = item.image,
                        kitchenArea = item.kitchenArea,
                        quantity = _quantity.value,
                        price = item.price,
                        status = item.status,
                        variant = _selectedVariant.value,
                        toppings = _selectedToppings.value
                    )
                )
                notificationManager.showSuccess("Đã thêm ${item.name} vào giỏ hàng")
                _addToCartEvent.emit(true)
                resetInputs()
            }
        }
    }
}
