package com.example.jetpackcompose.data.repository

import com.example.jetpackcompose.data.remote.api.ApiService
import com.example.jetpackcompose.data.remote.dto.*
import com.example.jetpackcompose.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import dagger.Lazy
import com.example.jetpackcompose.BuildConfig

interface AuthRepository {
    fun login(request: LoginRequest): Flow<Resource<AuthData>>
    fun register(request: RegisterRequest): Flow<Resource<AuthData>>
    fun googleLogin(request: GoogleLoginRequest): Flow<Resource<AuthData>>
    fun getAccount(): Flow<Resource<AccountData>>
    fun logout(): Flow<Resource<Unit>>
    fun updateUser(id: String, request: UpdateUserRequest): Flow<Resource<UserDto>>
    fun updatePassword(request: UpdatePasswordRequest): Flow<Resource<Unit>>
}

class AuthRepositoryImpl @Inject constructor(
    private val apiService: Lazy<ApiService>
) : BaseRepository(), AuthRepository {

    init {
        // Experiment switch reproduces the previous eager network initialization.
        if (!BuildConfig.STARTUP_OPTIMIZATIONS) apiService.get()
    }

    override fun login(request: LoginRequest) = flow {
        emit(Resource.Loading())
        emit(safeApiCall { apiService.get().login(request) })
    }

    override fun register(request: RegisterRequest) = flow {
        emit(Resource.Loading())
        emit(safeApiCall { apiService.get().register(request) })
    }

    override fun googleLogin(request: GoogleLoginRequest) = flow {
        emit(Resource.Loading())
        emit(safeApiCall { apiService.get().googleLogin(request) })
    }

    override fun getAccount() = flow {
        emit(Resource.Loading())
        emit(safeApiCall { apiService.get().getAccount() })
    }

    override fun logout() = flow {
        emit(Resource.Loading())
        emit(safeApiCall { apiService.get().logout() })
    }

    override fun updateUser(id: String, request: UpdateUserRequest) = flow {
        emit(Resource.Loading())
        emit(safeApiCall { apiService.get().updateUser(id, request) })
    }

    override fun updatePassword(request: UpdatePasswordRequest) = flow {
        emit(Resource.Loading())
        emit(safeApiCall { apiService.get().updatePassword(request) })
    }
}
