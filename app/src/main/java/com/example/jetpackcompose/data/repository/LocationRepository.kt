package com.example.jetpackcompose.data.repository

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.IntentSender
import android.location.Location
import android.os.Looper
import android.util.Log
import com.example.jetpackcompose.data.datastore.AuthPreference
import com.example.jetpackcompose.data.model.LocationData
import com.example.jetpackcompose.data.model.LocationUiState
import com.example.jetpackcompose.data.remote.api.GeocodingApiService
import com.example.jetpackcompose.utils.Resource
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

interface LocationRepository {
    fun getLocationState(): StateFlow<LocationUiState>
    fun shouldShowPermissionPopup(): Flow<Boolean>
    suspend fun markPopupShown()
    suspend fun startLocationUpdate()
    suspend fun refreshLocation()
    suspend fun geocodeAddress(address: String): Resource<LocationData>
    suspend fun updateAddressManually(newAddress: String)
    fun clearCache()
    fun checkGpsSettings(activity: Activity, onGpsEnabled: () -> Unit)
    fun onPermissionDenied()
}

@Singleton
class LocationRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authPreference: AuthPreference,
    private val geocodingApiService: GeocodingApiService
) : LocationRepository {

    private val TAG = "LocationRepository"
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    
    private val _locationState = MutableStateFlow<LocationUiState>(LocationUiState.Idle)
    override fun getLocationState(): StateFlow<LocationUiState> = _locationState.asStateFlow()

    override fun shouldShowPermissionPopup(): Flow<Boolean> = authPreference.isLocationPopupShown().map { shown ->
        !shown
    }

    override suspend fun markPopupShown() {
        authPreference.setLocationPopupShown(true)
    }

    override suspend fun startLocationUpdate() {
        val cachedAddress = authPreference.getAddress().first()
        val cachedLat = authPreference.getLat().first()
        val cachedLng = authPreference.getLng().first()

        if (!cachedAddress.isNullOrEmpty() && cachedLat != null && cachedLng != null) {
            Log.d(TAG, "Using cached location: $cachedAddress")
            _locationState.value = LocationUiState.Success(
                LocationData(cachedAddress, cachedLat, cachedLng)
            )
            return
        }

        fetchFreshLocation()
    }

    override suspend fun refreshLocation() {
        clearCache()
        fetchFreshLocation()
    }

    override suspend fun geocodeAddress(address: String): Resource<LocationData> {
        return withContext(Dispatchers.IO) {
            val query = if (address.lowercase().contains("vietnam") || address.lowercase().contains("việt nam")) {
                address
            } else {
                "$address, Vietnam"
            }
            
            var retryCount = 0
            val maxRetries = 1
            
            while (retryCount <= maxRetries) {
                try {
                    // Nominatim limit is 1 req/sec.
                    if (retryCount > 0) kotlinx.coroutines.delay(2000) else kotlinx.coroutines.delay(1000)
                    
                    Log.d(TAG, "Geocoding attempt ${retryCount + 1}: $query")
                    val response = geocodingApiService.searchAddress(query)
                    
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (!body.isNullOrEmpty()) {
                            val result = body[0]
                            val lat = result.lat?.toDoubleOrNull() ?: 0.0
                            val lon = result.lon?.toDoubleOrNull() ?: 0.0
                            val displayName = result.displayName ?: address
                            
                            Log.d(TAG, "Found coordinates: $lat, $lon for $displayName")
                            return@withContext Resource.Success(LocationData(displayName, lat, lon))
                        } else {
                            // If first attempt with ", Vietnam" failed, try original
                            if (retryCount == 0 && query != address) {
                                // Continue loop but query address directly
                                retryCount++
                                continue 
                            }
                            return@withContext Resource.Error("Không tìm thấy địa chỉ giao hàng.")
                        }
                    } else {
                        Log.e(TAG, "Geocoding API error: ${response.code()}")
                        if (response.code() == 403) {
                            return@withContext Resource.Error("Dịch vụ bản đồ đang bận (403). Thử nhập địa chỉ chi tiết hơn.")
                        }
                        // For other errors, maybe retry
                        retryCount++
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Geocoding exception: ${e.message}")
                    if (retryCount >= maxRetries) {
                        return@withContext Resource.Error("Lỗi kết nối mạng. Vui lòng kiểm tra lại.")
                    }
                    retryCount++
                }
            }
            Resource.Error("Không thể xác định vị trí sau nhiều lần thử.")
        }
    }

    override suspend fun updateAddressManually(newAddress: String) {
        if (newAddress.isBlank()) return
        Log.d(TAG, "Updating address manually: $newAddress")
        
        val result = geocodeAddress(newAddress)
        if (result is Resource.Success) {
            val data = result.data!!
            Log.d(TAG, "Manual geocoding success: ${data.latitude}, ${data.longitude}")
            authPreference.saveLocation(newAddress, data.latitude, data.longitude)
            _locationState.value = LocationUiState.Success(
                LocationData(newAddress, data.latitude, data.longitude)
            )
        } else {
            Log.w(TAG, "Manual geocoding failed: ${result.message}")
            authPreference.saveLocation(newAddress, 0.0, 0.0)
            _locationState.value = LocationUiState.Error(result.message ?: "Lỗi xác định vị trí")
        }
    }

    override fun clearCache() {
        _locationState.value = LocationUiState.Idle
    }

    override fun checkGpsSettings(activity: Activity, onGpsEnabled: () -> Unit) {
        _locationState.value = LocationUiState.CheckingGPS
        
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000).build()
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val client: SettingsClient = LocationServices.getSettingsClient(activity)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            Log.d(TAG, "GPS is already enabled")
            onGpsEnabled()
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    Log.d(TAG, "Requesting GPS to be enabled")
                    exception.startResolutionForResult(activity, 1001)
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.e(TAG, "Error starting resolution: ${sendEx.message}")
                    _locationState.value = LocationUiState.Error("Không thể bật GPS")
                }
            } else {
                Log.e(TAG, "GPS settings check failed")
                _locationState.value = LocationUiState.Error("Vui lòng bật GPS để sử dụng tính năng này", LocationUiState.ErrorType.GPS_DISABLED)
            }
        }
    }

    override fun onPermissionDenied() {
        _locationState.value = LocationUiState.Error(
            "Quyền truy cập vị trí bị từ chối. Vui lòng cấp quyền trong cài đặt.",
            LocationUiState.ErrorType.PERMISSION_DENIED
        )
    }

    @SuppressLint("MissingPermission")
    private suspend fun fetchFreshLocation() {
        Log.d(TAG, "Fetching fresh location...")
        _locationState.value = LocationUiState.GettingLocation

        try {
            val location = withTimeoutOrNull(15000) {
                var loc = fusedLocationClient.lastLocation.await()
                if (loc == null) {
                    Log.d(TAG, "lastLocation is null, trying getCurrentLocation")
                    loc = try {
                        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
                    } catch (e: Exception) {
                        null
                    }
                }
                
                if (loc == null) {
                    Log.d(TAG, "getCurrentLocation is null, trying requestLocationUpdates")
                    loc = requestSingleUpdate().firstOrNull()
                }
                loc
            }

            if (location != null) {
                Log.d(TAG, "Location obtained: ${location.latitude}, ${location.longitude}")
                reverseGeocode(location)
            } else {
                Log.e(TAG, "Location timeout or failed")
                _locationState.value = LocationUiState.Error(
                    "Không thể lấy vị trí hiện tại. Vui lòng kiểm tra GPS.",
                    LocationUiState.ErrorType.TIMEOUT
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in fetchFreshLocation: ${e.message}")
            _locationState.value = LocationUiState.Error("Lỗi: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestSingleUpdate(): Flow<Location?> = callbackFlow {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
            .setMaxUpdates(1)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                trySend(result.lastLocation)
                channel.close()
            }
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, callback, Looper.getMainLooper())
        awaitClose { fusedLocationClient.removeLocationUpdates(callback) }
    }

    private suspend fun reverseGeocode(location: Location) {
        _locationState.value = LocationUiState.ConvertingAddress
        
        withContext(Dispatchers.IO) {
            try {
                val response = geocodingApiService.reverseGeocode(location.latitude, location.longitude)
                if (response.isSuccessful) {
                    val result = response.body()
                    if (result != null) {
                        val fullAddress = result.displayName ?: "${location.latitude}, ${location.longitude}"
                        Log.d(TAG, "Reverse geocoding success: $fullAddress")
                        val locationData = LocationData(fullAddress, location.latitude, location.longitude)
                        authPreference.saveLocation(fullAddress, location.latitude, location.longitude)
                        _locationState.value = LocationUiState.Success(locationData)
                    } else {
                        Log.w(TAG, "No reverse geocode result")
                        _locationState.value = LocationUiState.Error("Không tìm thấy địa chỉ cho tọa độ này.")
                    }
                } else {
                    Log.e(TAG, "Reverse geocode API error: ${response.code()}")
                    _locationState.value = LocationUiState.Error("Lỗi máy chủ địa chỉ")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Reverse geocoding error: ${e.message}")
                _locationState.value = LocationUiState.Error("Không thể xác định địa chỉ. Kiểm tra internet.")
            }
        }
    }
}
