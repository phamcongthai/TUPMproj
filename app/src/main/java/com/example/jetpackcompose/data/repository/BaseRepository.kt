package com.example.jetpackcompose.data.repository

import com.example.jetpackcompose.data.remote.dto.ApiResponse
import com.example.jetpackcompose.utils.Resource
import com.google.gson.Gson
import retrofit2.Response

abstract class BaseRepository {

    protected suspend fun <T> safeApiCall(apiCall: suspend () -> Response<ApiResponse<T>>): Resource<T> {
        return try {
            val response = apiCall()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    if (body.data != null) {
                        Resource.Success(body.data)
                    } else if (body.statusCode == 200 || body.statusCode == 201) {
                        // For cases where data might be null but status is success (like logout)
                        @Suppress("UNCHECKED_CAST")
                        Resource.Success(Unit as T)
                    } else {
                        Resource.Error(body.message ?: "Data is null")
                    }
                } else {
                    Resource.Error("Response body is null")
                }
            } else {
                // Handle error body if available
                val errorJson = response.errorBody()?.string()
                val message = try {
                    val apiResponse = Gson().fromJson(errorJson, ApiResponse::class.java)
                    apiResponse.message ?: response.message()
                } catch (_: Exception) {
                    response.message()
                }
                Resource.Error(message)
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An unknown error occurred")
        }
    }
}
