package com.example.jetpackcompose.data.remote.api

import com.example.jetpackcompose.data.remote.dto.GeocodingResponseDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

interface GeocodingApiService {
    @GET("search")
    suspend fun searchAddress(
        @Query("q") query: String,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 1,
        @Query("countrycodes") countrycodes: String = "vn"
    ): Response<List<GeocodingResponseDto>>

    @GET("reverse")
    suspend fun reverseGeocode(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("format") format: String = "json"
    ): Response<GeocodingResponseDto>
}
