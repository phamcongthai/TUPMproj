package com.example.jetpackcompose.data.remote.dto

import com.google.gson.annotations.SerializedName

data class OrderResponse(
    @SerializedName("_id") val id: String? = null,
    val paymentStatus: String? = null,
    val customerId: String? = null,
    val method: String? = null, // "ship" or "pickup"
    val payment: String? = null, // "cod" or "bank"
    val deliveryAddress: String? = null,
    val pickupTime: String? = null,
    val note: String? = null,
    val totalItemPrice: Long = 0,
    val totalPayment: Long = 0,
    val status: String? = null, // "pending", "confirmed", "preparing", "ready", "delivering", "completed", "cancelled"
    val orderItems: List<OrderItemDto>? = null,
    val tracking: List<TrackingDto>? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

data class TrackingDto(
    val status: String? = null,
    val updatedAt: String? = null,
    val timestamp: String? = null
)
