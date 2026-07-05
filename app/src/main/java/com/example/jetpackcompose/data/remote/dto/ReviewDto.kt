package com.example.jetpackcompose.data.remote.dto

import com.google.gson.annotations.SerializedName

data class CreateReviewRequest(
    val type: String = "item",
    val userId: String? = null,
    val user: String,
    val menuItemId: String,
    val rating: Int,
    val comment: String
)

data class UpdateReviewRequest(
    val rating: Int,
    val comment: String
)

data class ReviewListRequest(
    val id: String
)

data class ReviewDto(
    @SerializedName("_id") val idUnderScore: String?,
    @SerializedName("id") val idDirect: String?,
    val type: String?,
    val userId: String?,
    val user: String?,
    val avatar: String?,
    val menuItemId: String?,
    val rating: Int?,
    val comment: String?,
    val createdAt: String?,
    val updatedAt: String?,
    val createdBy: CreatedByDto?
) {
    val realId: String? get() = idUnderScore ?: idDirect
    val realUserId: String? get() = userId ?: createdBy?.realId
}

data class CreatedByDto(
    @SerializedName("_id") val idUnderScore: String?,
    @SerializedName("id") val idDirect: String?,
    val email: String?,
    val name: String? = null,
    val avatar: String? = null
) {
    val realId: String? get() = idUnderScore ?: idDirect
}
