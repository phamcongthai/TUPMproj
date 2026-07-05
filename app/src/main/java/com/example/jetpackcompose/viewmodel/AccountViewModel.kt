package com.example.jetpackcompose.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jetpackcompose.data.datastore.AuthPreference
import com.example.jetpackcompose.data.remote.dto.UpdatePasswordRequest
import com.example.jetpackcompose.data.remote.dto.UpdateUserRequest
import com.example.jetpackcompose.data.remote.dto.UserDto
import com.example.jetpackcompose.data.repository.AuthRepository
import com.example.jetpackcompose.data.repository.LocationRepository
import com.example.jetpackcompose.utils.Resource
import com.example.jetpackcompose.utils.UiState
import com.example.jetpackcompose.utils.notification.NotificationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val authPreference: AuthPreference,
    private val authRepository: AuthRepository,
    private val locationRepository: LocationRepository,
    private val notificationManager: NotificationManager
) : ViewModel() {

    val user = authPreference.userData.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        UserDto(null, null, null, null, null, null, null)
    )

    val locationState = locationRepository.getLocationState()

    fun refreshLocation() {
        viewModelScope.launch {
            locationRepository.refreshLocation()
        }
    }

    private val _updateProfileState = MutableStateFlow<UiState<UserDto>>(UiState.Idle)
    val updateProfileState = _updateProfileState.asStateFlow()

    private val _updatePasswordState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val updatePasswordState = _updatePasswordState.asStateFlow()

    fun logout() {
        viewModelScope.launch {
            authPreference.clearAuth()
        }
    }

    fun updateProfile(name: String, phone: String, gender: String, avatar: String) {
        val currentUser = user.value
        val userId = currentUser.id ?: return
        val email = currentUser.email ?: return

        viewModelScope.launch {
            val request = UpdateUserRequest(email, name, phone, gender, avatar)
            authRepository.updateUser(userId, request).collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        notificationManager.hideLoading()
                        val updatedUser = resource.data!!
                        authPreference.saveUser(updatedUser)
                        notificationManager.showSuccess("Cập nhật thông tin thành công!")
                        _updateProfileState.value = UiState.Success(updatedUser)
                    }
                    is Resource.Error -> {
                        notificationManager.hideLoading()
                        val errorMsg = resource.message ?: "Cập nhật thất bại"
                        notificationManager.showError(errorMsg)
                        _updateProfileState.value = UiState.Error(errorMsg)
                    }
                    is Resource.Loading -> {
                        notificationManager.showLoading()
                        _updateProfileState.value = UiState.Loading
                    }
                }
            }
        }
    }

    fun updatePassword(oldPass: String, newPass: String, confirmPass: String) {
        if (newPass.length < 8) {
            viewModelScope.launch { notificationManager.showError("Mật khẩu mới phải có ít nhất 8 ký tự") }
            return
        }
        if (newPass != confirmPass) {
            viewModelScope.launch { notificationManager.showError("Mật khẩu xác nhận không khớp") }
            return
        }

        val email = user.value.email ?: return
        viewModelScope.launch {
            val request = UpdatePasswordRequest(email, oldPass, newPass)
            authRepository.updatePassword(request).collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        notificationManager.hideLoading()
                        notificationManager.showSuccess("Đổi mật khẩu thành công!")
                        _updatePasswordState.value = UiState.Success(Unit)
                    }
                    is Resource.Error -> {
                        notificationManager.hideLoading()
                        val errorMsg = resource.message ?: "Đổi mật khẩu thất bại"
                        notificationManager.showError(errorMsg)
                        _updatePasswordState.value = UiState.Error(errorMsg)
                    }
                    is Resource.Loading -> {
                        notificationManager.showLoading()
                        _updatePasswordState.value = UiState.Loading
                    }
                }
            }
        }
    }

    fun resetStates() {
        _updateProfileState.value = UiState.Idle
        _updatePasswordState.value = UiState.Idle
    }
}
