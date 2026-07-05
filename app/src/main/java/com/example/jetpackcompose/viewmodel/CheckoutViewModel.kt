package com.example.jetpackcompose.viewmodel

import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jetpackcompose.data.datastore.AuthPreference
import com.example.jetpackcompose.data.remote.dto.*
import com.example.jetpackcompose.data.repository.CartRepository
import com.example.jetpackcompose.data.repository.FoodRepository
import com.example.jetpackcompose.utils.Constants
import com.example.jetpackcompose.utils.Resource
import com.example.jetpackcompose.utils.UiState
import com.example.jetpackcompose.utils.notification.NotificationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class CheckoutViewModel @Inject constructor(
    private val cartRepository: CartRepository,
    private val foodRepository: FoodRepository,
    private val authPreference: AuthPreference,
    private val notificationManager: NotificationManager,
    @ApplicationContext private val context: Context
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

    private val _checkoutState = MutableStateFlow<UiState<Any>>(UiState.Idle)
    val checkoutState = _checkoutState.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage = _toastMessage.asSharedFlow()

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
                    if (_method.value == DeliveryMethod.SHIP) {
                        calculateDistance(savedAddress)
                    }
                }
            }
        }

        // Use debounce to avoid triggering Geocoder on every keystroke
        viewModelScope.launch {
            _address
                .debounce(800) // Adjust as needed
                .distinctUntilChanged()
                .collect { addr ->
                    if (addr.isNotBlank() && _method.value == DeliveryMethod.SHIP) {
                        calculateDistance(addr)
                    } else {
                        _distanceKm.value = 0.0
                        _shipFee.value = 0
                    }
                }
        }
    }

    fun setMethod(m: DeliveryMethod) { 
        _method.value = m 
        if (m == DeliveryMethod.PICKUP) {
            _distanceKm.value = 0.0
            _shipFee.value = 0
        } else if (_address.value.isNotBlank()) {
            calculateDistance(_address.value)
        }
    }
    
    fun setPayment(p: PaymentMethod) { _payment.value = p }
    
    fun setAddress(a: String) {
        _address.value = a
        viewModelScope.launch {
            // We save just the string here. The lat/lng will be updated via geocoding if it's a ship order.
            authPreference.saveLocation(a, 0.0, 0.0)
        }
    }
    
    fun setPickupTime(t: String) { _pickupTime.value = t }
    fun setNote(n: String) { _note.value = n }

    private fun calculateDistance(addressStr: String) {
        geocodeJob?.cancel() // Cancel previous calculation if still running
        geocodeJob = viewModelScope.launch {
            Log.d(TAG, "Starting distance calculation for: $addressStr")

            try {
                val distResult = withContext(Dispatchers.IO) {
                    if (!Geocoder.isPresent()) {
                        Log.e(TAG, "Geocoder is not present on this device")
                        return@withContext null
                    }
                    val geocoder = Geocoder(context, Locale.getDefault())
                    @Suppress("DEPRECATION")
                    val addresses = try {
                        geocoder.getFromLocationName(addressStr, 1)
                    } catch (e: Exception) {
                        Log.e(TAG, "Geocoder error: ${e.message}")
                        null
                    }

                    if (addresses != null && addresses.isNotEmpty()) {
                        val location = addresses[0]
                        val destLat = location.latitude
                        val destLng = location.longitude
                        Log.d(TAG, "Found location: $destLat, $destLng")

                        val results = FloatArray(1)
                        Location.distanceBetween(
                            Constants.STORE_LAT, Constants.STORE_LNG,
                            destLat, destLng,
                            results
                        )

                        val distKm = results[0] / 1000.0
                        val roundedDist = Math.round(distKm * 10) / 10.0
                        Log.d(TAG, "Calculated distance: $roundedDist km")
                        roundedDist
                    } else {
                        Log.w(TAG, "No address found for: $addressStr")
                        null
                    }
                }

                if (distResult != null) {
                    _distanceKm.value = distResult
                    _shipFee.value = (distResult * 2000).toLong() // 2000đ/km
                    Log.d(TAG, "Updated ship fee: ${_shipFee.value}")
                } else {
                    _distanceKm.value = 0.0
                    _shipFee.value = 0
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in calculateDistance: ${e.message}")
                _distanceKm.value = 0.0
                _shipFee.value = 0
            }
        }
    }

    fun placeOrder() {
        if (cartItems.value.isEmpty()) return
        
        val outOfStockItems = cartItems.value.filter { it.status == "out_of_stock" }
        if (outOfStockItems.isNotEmpty()) {
            viewModelScope.launch {
                notificationManager.showWarning("Giỏ hàng chứa món đã hết hàng. Vui lòng kiểm tra lại.")
            }
            return
        }

        if (_method.value == DeliveryMethod.SHIP && _address.value.isEmpty()) {
            viewModelScope.launch { notificationManager.showError("Vui lòng nhập địa chỉ giao hàng") }
            return
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
