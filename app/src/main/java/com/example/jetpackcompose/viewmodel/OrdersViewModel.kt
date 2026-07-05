package com.example.jetpackcompose.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jetpackcompose.data.remote.dto.OrderResponse
import com.example.jetpackcompose.data.repository.FoodRepository
import com.example.jetpackcompose.utils.Resource
import com.example.jetpackcompose.utils.UiState
import com.example.jetpackcompose.utils.notification.NotificationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OrdersViewModel @Inject constructor(
    private val foodRepository: FoodRepository,
    private val notificationManager: NotificationManager
) : ViewModel() {

    private val _uncompletedOrders = MutableStateFlow<UiState<List<OrderResponse>>>(UiState.Loading)
    val uncompletedOrders = _uncompletedOrders.asStateFlow()

    private val _completedOrders = MutableStateFlow<UiState<List<OrderResponse>>>(UiState.Loading)
    val completedOrders = _completedOrders.asStateFlow()

    private val _cancelledOrders = MutableStateFlow<UiState<List<OrderResponse>>>(UiState.Loading)
    val cancelledOrders = _cancelledOrders.asStateFlow()

    init {
        fetchAllOrders()
    }

    fun fetchAllOrders() {
        fetchUncompleted()
        fetchCompleted()
        fetchCancelled()
    }

    private fun fetchUncompleted() {
        viewModelScope.launch {
            foodRepository.getUncompletedOrders().collect { resource ->
                _uncompletedOrders.value = when (resource) {
                    is Resource.Success -> UiState.Success(resource.data ?: emptyList())
                    is Resource.Error -> UiState.Error(resource.message ?: "Error")
                    is Resource.Loading -> UiState.Loading
                }
            }
        }
    }

    private fun fetchCompleted() {
        viewModelScope.launch {
            foodRepository.getCompletedOrders().collect { resource ->
                _completedOrders.value = when (resource) {
                    is Resource.Success -> UiState.Success(resource.data ?: emptyList())
                    is Resource.Error -> UiState.Error(resource.message ?: "Error")
                    is Resource.Loading -> UiState.Loading
                }
            }
        }
    }

    private fun fetchCancelled() {
        viewModelScope.launch {
            foodRepository.getCancelledOrders().collect { resource ->
                _cancelledOrders.value = when (resource) {
                    is Resource.Success -> UiState.Success(resource.data ?: emptyList())
                    is Resource.Error -> UiState.Error(resource.message ?: "Error")
                    is Resource.Loading -> UiState.Loading
                }
            }
        }
    }

    fun completeOrder(orderId: String) {
        viewModelScope.launch {
            foodRepository.completePreOrder(orderId).collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        notificationManager.hideLoading()
                        notificationManager.showSuccess("Đã nhận hàng thành công!")
                        fetchAllOrders()
                    }
                    is Resource.Error -> {
                        notificationManager.hideLoading()
                        notificationManager.showError(resource.message ?: "Có lỗi xảy ra khi xác nhận đơn hàng")
                    }
                    is Resource.Loading -> {
                        notificationManager.showLoading()
                    }
                }
            }
        }
    }
}
