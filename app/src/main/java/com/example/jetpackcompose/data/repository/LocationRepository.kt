package com.example.jetpackcompose.data.repository

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.IntentSender
import android.location.Geocoder
import android.location.Location
import android.os.Looper
import android.util.Log
import com.example.jetpackcompose.data.datastore.AuthPreference
import com.example.jetpackcompose.data.model.LocationData
import com.example.jetpackcompose.data.model.LocationUiState
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
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

interface LocationRepository {
    fun getLocationState(): StateFlow<LocationUiState>
    fun shouldShowPermissionPopup(): Flow<Boolean>
    suspend fun markPopupShown()
    suspend fun startLocationUpdate()
    suspend fun refreshLocation()
    fun clearCache()
    fun checkGpsSettings(activity: Activity, onGpsEnabled: () -> Unit)
    fun onPermissionDenied()
}

@Singleton
class LocationRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authPreference: AuthPreference
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
                val geocoder = Geocoder(context, Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                
                if (!addresses.isNullOrEmpty()) {
                    val addr = addresses[0]
                    val fullAddress = buildString {
                        val feature = addr.featureName
                        val thoroughfare = addr.thoroughfare
                        val subAdmin = addr.subAdminArea
                        val admin = addr.adminArea
                        
                        if (feature != null) append(feature)
                        if (thoroughfare != null && thoroughfare != feature) {
                            if (isNotEmpty()) append(", ")
                            append(thoroughfare)
                        }
                        if (subAdmin != null) {
                            if (isNotEmpty()) append(", ")
                            append(subAdmin)
                        }
                        if (admin != null) {
                            if (isNotEmpty()) append(", ")
                            append(admin)
                        }
                    }
                    
                    Log.d(TAG, "Geocoding success: $fullAddress")
                    val locationData = LocationData(fullAddress, location.latitude, location.longitude)
                    authPreference.saveLocation(fullAddress, location.latitude, location.longitude)
                    _locationState.value = LocationUiState.Success(locationData)
                } else {
                    Log.w(TAG, "No address found")
                    _locationState.value = LocationUiState.Error(
                        "Không tìm thấy địa chỉ cho tọa độ này.",
                        LocationUiState.ErrorType.GEOCODER_FAILED
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Geocoder error: ${e.message}")
                _locationState.value = LocationUiState.Error("Lỗi chuyển đổi địa chỉ: ${e.message}")
            }
        }
    }
}
