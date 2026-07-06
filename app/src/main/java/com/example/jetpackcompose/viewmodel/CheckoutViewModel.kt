package com.example.jetpackcompose.viewmodel

import android.location.Location
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jetpackcompose.data.datastore.AuthPreference
import com.example.jetpackcompose.data.remote.dto.*
import com.example.jetpackcompose.data.repository.CartRepository
import com.example.jetpackcompose.data.repository.FoodRepository
import com.example.jetpackcompose.data.repository.LocationRepository
import com.example.jetpackcompose.utils.Constants
import com.example.jetpackcompose.utils.Resource
import com.example.jetpackcompose.utils.UiState
import com.example.jetpackcompose.utils.notification.NotificationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class CheckoutViewModel @Inject constructor(
    private val cartRepository: CartRepository,
    private val foodRepository: FoodRepository,
    private val authPreference: AuthPreference,
    private val locationRepository: LocationRepository,
    private val notificationManager: NotificationManager
) : ViewModel() {

    private val TAG = "CheckoutViewModel"

    val cartItems = cartRepository.getCartItems().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    private val _method = MutableStateFlow(DeliveryMethod.SHIP)
    val method = _method.asStateFlow()

    private val _payment = MutableStateFlow(PaymentMethod.COD)
    val payment = _payment.asStateFlow()

    private val _address = MutableStateFlow("")
    val address = _address.asStateFlow()

    private val _pickupTime = MutableStateFlow("")
    val pickupTime = _pickupTime.asStateFlow()

    private val _note = MutableStateFlow("")
    val note = _note.asStateFlow()

    private val _distanceKm = MutableStateFlow(0.0)
    val distanceKm = _distanceKm.asStateFlow()

    private val _shipFee = MutableStateFlow(0L)
    val shipFee = _shipFee.asStateFlow()

    private val _isAddressValid = MutableStateFlow(true)
    val isAddressValid = _isAddressValid.asStateFlow()

    private val _geocodingLoading = MutableStateFlow(false)
    val geocodingLoading = _geocodingLoading.asStateFlow()

    private val _checkoutState = MutableStateFlow<UiState<Any>>(UiState.Idle)
    val checkoutState = _checkoutState.asStateFlow()

    val totalItemPrice = cartItems.map { items ->
        items.sumOf { it.getTotalPrice() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val totalPayment = combine(totalItemPrice, shipFee, _method) { items, ship, m ->
        if (m == DeliveryMethod.SHIP) items + ship else items
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    private var geocodeJob: Job? = null

    init {
        // Load initial address from storage
        viewModelScope.launch {
            authPreference.getAddress().first()?.let { savedAddress ->
                if (savedAddress.isNotBlank()) {
                    _address.value = savedAddress
                    // We don't necessarily know lat/lng here unless we also load them
                    val lat = authPreference.getLat().first()
                    val lng = authPreference.getLng().first()
                    if (lat != null && lng != null && lat != 0.0 && lng != 0.0) {
                        calculateDistance(lat, lng)
                    } else if (_method.value == DeliveryMethod.SHIP) {
                        startGeocoding(savedAddress)
                    }
                }
            }
        }

        // Debounce address input
        viewModelScope.launch {
            _address
                .debounce(1000)
                .distinctUntilChanged()
                .collect { addr ->
                    if (addr.isNotBlank() && _method.value == DeliveryMethod.SHIP) {
                        startGeocoding(addr)
                    } else {
                        resetDistance()
                    }
                }
        }
    }

    private fun startGeocoding(addr: String) {
        geocodeJob?.cancel()
        geocodeJob = viewModelScope.launch {
            _geocodingLoading.value = true
            _isAddressValid.value = false // Invalid until proven otherwise
            
            val result = locationRepository.geocodeAddress(addr)
            
            _geocodingLoading.value = false
            when (result) {
                is Resource.Success -> {
                    val location = result.data!!
                    calculateDistance(location.latitude, location.longitude)
                    _isAddressValid.value = true
                    // Save valid coordinates to DataStore
                    authPreference.saveLocation(addr, location.latitude, location.longitude)
                }
                is Resource.Error -> {
                    resetDistance()
                    _isAddressValid.value = false
                    notificationManager.showError(result.message ?: "Không tìm thấy địa chỉ.")
                }
                else -> {}
            }
        }
    }

    private fun resetDistance() {
        _distanceKm.value = 0.0
        _shipFee.value = 0
    }

    private fun calculateDistance(destLat: Double, destLng: Double) {
        val results = FloatArray(1)
        Location.distanceBetween(
            Constants.STORE_LAT, Constants.STORE_LNG,
            destLat, destLng,
            results
        )

        val distKm = results[0] / 1000.0
        val roundedDist = Math.round(distKm * 10) / 10.0
        _distanceKm.value = roundedDist
        _shipFee.value = (roundedDist * 2000).toLong() // 2000đ/km
        Log.d(TAG, "Distance calculated: $roundedDist km, Fee: ${_shipFee.value}")
    }

    fun setMethod(m: DeliveryMethod) { 
        _method.value = m 
        if (m == DeliveryMethod.PICKUP) {
            resetDistance()
            _isAddressValid.value = true // Not needed for pickup
        } else if (_address.value.isNotBlank()) {
            startGeocoding(_address.value)
        }
    }
    
    fun setPayment(p: PaymentMethod) { _payment.value = p }
    
    fun setAddress(a: String) {
        _address.value = a
    }
    
    fun setPickupTime(t: String) { _pickupTime.value = t }
    fun setNote(n: String) { _note.value = n }

    fun placeOrder() {
        if (cartItems.value.isEmpty()) return
        
        val outOfStockItems = cartItems.value.filter { it.status == "out_of_stock" }
        if (outOfStockItems.isNotEmpty()) {
            viewModelScope.launch {
                notificationManager.showWarning("Giỏ hàng chứa món đã hết hàng. Vui lòng kiểm tra lại.")
            }
            return
        }

        if (_method.value == DeliveryMethod.SHIP) {
            if (_address.value.isEmpty()) {
                viewModelScope.launch { notificationManager.showError("Vui lòng nhập địa chỉ giao hàng") }
                return
            }
            if (!_isAddressValid.value) {
                viewModelScope.launch { notificationManager.showError("Địa chỉ giao hàng không hợp lệ") }
                return
            }
        }

        if (_method.value == DeliveryMethod.PICKUP && _pickupTime.value.isEmpty()) {
            viewModelScope.launch { notificationManager.showError("Vui lòng chọn thời gian đến lấy") }
            return
        }

        viewModelScope.launch {
            val customerId = authPreference.getUserId().first() ?: ""
            
            val request = CreateOrderRequest(
                customerId = customerId,
                method = _method.value.name.lowercase(),
                payment = _payment.value.name.lowercase(),
                deliveryAddress = if (_method.value == DeliveryMethod.SHIP) _address.value else null,
                pickupTime = if (_method.value == DeliveryMethod.PICKUP) _pickupTime.value else null,
                note = _note.value,
                totalItemPrice = totalItemPrice.value,
                totalPayment = totalPayment.value,
                orderItems = cartItems.value.map { item ->
                    OrderItemDto(
                        kitchenArea = item.kitchenArea,
                        menuItemId = item.id,
                        name = item.name,
                        quantity = item.quantity,
                        price = item.variant?.price ?: item.price,
                        variant = item.variant?.let { v ->
                            OrderVariantDto(v.id, v.label, v.price)
                        },
                        toppings = item.toppings.map { t ->
                            OrderToppingDto(t.id, t.name, t.price)
                        }
                    )
                }
            )

            foodRepository.createPreOrder(request).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        notificationManager.showLoading()
                        _checkoutState.value = UiState.Loading
                    }
                    is Resource.Success -> {
                        notificationManager.hideLoading()
                        notificationManager.showSuccess("Đặt hàng thành công!")
                        _checkoutState.value = UiState.Success(resource.data!!)
                        cartRepository.clearCart()
                    }
                    is Resource.Error -> {
                        notificationManager.hideLoading()
                        val errorMsg = resource.message ?: "Đặt hàng thất bại"
                        notificationManager.showError(errorMsg)
                        _checkoutState.value = UiState.Error(errorMsg)
                    }
                }
            }
        }
    }
    
    fun resetState() {
        _checkoutState.value = UiState.Idle
    }
}
