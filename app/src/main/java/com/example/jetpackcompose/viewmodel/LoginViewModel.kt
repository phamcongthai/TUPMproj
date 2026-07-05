package com.example.jetpackcompose.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jetpackcompose.data.datastore.AuthPreference
import com.example.jetpackcompose.data.remote.dto.GoogleLoginRequest
import com.example.jetpackcompose.data.remote.dto.LoginRequest
import com.example.jetpackcompose.data.repository.AuthRepository
import com.example.jetpackcompose.utils.Resource
import com.example.jetpackcompose.utils.UiState
import com.example.jetpackcompose.utils.ValidationUtils
import com.example.jetpackcompose.utils.notification.NotificationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val authPreference: AuthPreference,
    private val notificationManager: NotificationManager
) : ViewModel() {

    private val _email = MutableStateFlow("")
    val email = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password = _password.asStateFlow()

    private val _loginState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val loginState = _loginState.asStateFlow()

    val isEmailValid = _email.map { ValidationUtils.isValidEmail(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isPasswordValid = _password.map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun onEmailChange(newValue: String) {
        _email.value = newValue
    }

    fun onPasswordChange(newValue: String) {
        _password.value = newValue
    }

    fun login() {
        if (!ValidationUtils.isValidEmail(_email.value) || _password.value.isEmpty()) return

        viewModelScope.launch {
            repository.login(LoginRequest(_email.value, _password.value)).collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        notificationManager.hideLoading()
                        val authData = resource.data
                        if (authData?.accessToken != null && authData.user != null) {
                            authPreference.saveTokens(
                                authData.accessToken,
                                authData.refreshToken ?: ""
                            )
                            authPreference.saveUser(authData.user)
                            notificationManager.showSuccess("Đăng nhập thành công! Chào mừng trở lại.")
                            _loginState.value = UiState.Success(Unit)
                        } else {
                            notificationManager.showError("Dữ liệu phản hồi không hợp lệ")
                            _loginState.value = UiState.Error("Dữ liệu phản hồi không hợp lệ")
                        }
                    }
                    is Resource.Error -> {
                        notificationManager.hideLoading()
                        val errorMsg = resource.message ?: "Đăng nhập thất bại"
                        notificationManager.showError(errorMsg)
                        _loginState.value = UiState.Error(errorMsg)
                    }
                    is Resource.Loading -> {
                        notificationManager.showLoading()
                        _loginState.value = UiState.Loading
                    }
                }
            }
        }
    }

    fun googleLogin(email: String, name: String, picture: String) {
        viewModelScope.launch {
            repository.googleLogin(GoogleLoginRequest(email, name, picture)).collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        notificationManager.hideLoading()
                        val authData = resource.data
                        if (authData?.accessToken != null && authData.user != null) {
                            authPreference.saveTokens(
                                authData.accessToken,
                                authData.refreshToken ?: ""
                            )
                            authPreference.saveUser(authData.user)
                            notificationManager.showSuccess("Đăng nhập Google thành công!")
                            _loginState.value = UiState.Success(Unit)
                        } else {
                            notificationManager.showError("Dữ liệu Google không hợp lệ")
                            _loginState.value = UiState.Error("Dữ liệu phản hồi không hợp lệ")
                        }
                    }
                    is Resource.Error -> {
                        notificationManager.hideLoading()
                        val errorMsg = resource.message ?: "Google Login Failed"
                        notificationManager.showError(errorMsg)
                        _loginState.value = UiState.Error(errorMsg)
                    }
                    is Resource.Loading -> {
                        notificationManager.showLoading()
                        _loginState.value = UiState.Loading
                    }
                }
            }
        }
    }

    fun resetState() {
        _loginState.value = UiState.Idle
    }
}
