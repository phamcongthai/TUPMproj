package com.example.jetpackcompose.data.remote.api

import com.example.jetpackcompose.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    @POST("/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponse<AuthData>>

    @POST("/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<ApiResponse<AuthData>>

    @POST("/auth/google")
    suspend fun googleLogin(@Body request: GoogleLoginRequest): Response<ApiResponse<AuthData>>

    @GET("/auth/account")
    suspend fun getAccount(): Response<ApiResponse<AccountData>>

    @GET("/auth/refresh-token")
    suspend fun refreshToken(@Header("Authorization") refreshToken: String): Response<ApiResponse<TokenData>>

    @POST("/auth/logout")
    suspend fun logout(): Response<ApiResponse<Unit>>

    @POST("/promotions/active")
    suspend fun getActivePromotions(): Response<ApiResponse<List<PromotionDto>>>

    @POST("/menus/category")
    suspend fun getCategories(): Response<ApiResponse<List<String>>>

    @POST("/menus/items")
    suspend fun getMenuItems(@Query("category") category: String?): Response<ApiResponse<List<MenuItemDto>>>

    @GET("/menus/{id}")
    suspend fun getMenuItem(@Path("id") id: String): Response<ApiResponse<MenuItemDto>>

    @POST("/pre-order")
    suspend fun createPreOrder(@Body body: CreateOrderRequest): Response<ApiResponse<Any>>

    @PATCH("/users/{id}")
    suspend fun updateUser(@Path("id") id: String, @Body body: UpdateUserRequest): Response<ApiResponse<UserDto>>

    @POST("/users/update-password")
    suspend fun updatePassword(@Body body: UpdatePasswordRequest): Response<ApiResponse<Unit>>

    @POST("/pre-order/uncompleted")
    suspend fun getUncompletedOrders(): Response<ApiResponse<List<OrderResponse>>>

    @POST("/pre-order/completed")
    suspend fun getCompletedOrders(): Response<ApiResponse<List<OrderResponse>>>

    @POST("/pre-order/cancelled")
    suspend fun getCancelledOrders(): Response<ApiResponse<List<OrderResponse>>>

    @POST("/pre-order/completed-pre-order")
    suspend fun completePreOrder(@Body body: CompleteOrderRequest): Response<ApiResponse<Unit>>

    // Review Endpoints
    @POST("/reviews")
    suspend fun createReview(@Body request: CreateReviewRequest): Response<ApiResponse<ReviewDto>>

    @PATCH("/reviews/{id}")
    suspend fun updateReview(@Path("id") id: String, @Body request: UpdateReviewRequest): Response<ApiResponse<ReviewDto>>

    @DELETE("/reviews/{id}")
    suspend fun deleteReview(@Path("id") id: String): Response<ApiResponse<Unit>>

    @POST("/reviews/comment-list")
    suspend fun getReviewList(@Body request: ReviewListRequest): Response<ApiResponse<List<ReviewDto>>>
}
