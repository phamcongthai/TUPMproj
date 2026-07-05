package com.example.jetpackcompose.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jetpackcompose.data.datastore.AuthPreference
import com.example.jetpackcompose.data.remote.dto.UserDto
import com.example.jetpackcompose.data.repository.AuthRepository
import com.example.jetpackcompose.utils.Resource
import com.example.jetpackcompose.utils.notification.NotificationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authPreference: AuthPreference,
    private val authRepository: AuthRepository,
    private val notificationManager: NotificationManager
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        checkAuth()
    }

    private fun checkAuth() {
        viewModelScope.launch {
            authPreference.getAccessToken().collect { token ->
                if (token.isNullOrEmpty()) {
                    _authState.value = AuthState.Unauthenticated
                } else {
                    fetchAccount()
                }
            }
        }
    }

    private fun fetchAccount() {
        viewModelScope.launch {
            authRepository.getAccount().collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        resource.data?.user?.let {
                            authPreference.saveUser(it)
                            _authState.value = AuthState.Authenticated(it)
                        } ?: run {
                            _authState.value = AuthState.Unauthenticated
                        }
                    }
                    is Resource.Error -> {
                        if (_authState.value !is AuthState.Unauthenticated) {
                            notificationManager.showWarning("Phiên đăng nhập hết hạn, vui lòng đăng nhập lại.")
                        }
                        _authState.value = AuthState.Unauthenticated
                    }
                    is Resource.Loading -> {
                        _authState.value = AuthState.Loading
                    }
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout().collect { resource ->
                if (resource is Resource.Success) {
                    authPreference.clearAuth()
                    _authState.value = AuthState.Unauthenticated
                }
            }
        }
    }
}

sealed class AuthState {
    object Loading : AuthState()
    data class Authenticated(val user: UserDto) : AuthState()
    object Unauthenticated : AuthState()
}
