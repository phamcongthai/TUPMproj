package com.example.jetpackcompose.data.repository

import com.example.jetpackcompose.data.remote.api.ApiService
import com.example.jetpackcompose.data.remote.dto.*
import com.example.jetpackcompose.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface ReviewRepository {
    fun createReview(request: CreateReviewRequest): Flow<Resource<ReviewDto>>
    fun updateReview(id: String, request: UpdateReviewRequest): Flow<Resource<ReviewDto>>
    fun deleteReview(id: String): Flow<Resource<Unit>>
    fun getReviewList(menuItemId: String): Flow<Resource<List<ReviewDto>>>
}

class ReviewRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : BaseRepository(), ReviewRepository {

    override fun createReview(request: CreateReviewRequest) = flow {
        emit(Resource.Loading())
        emit(safeApiCall { apiService.createReview(request) })
    }

    override fun updateReview(id: String, request: UpdateReviewRequest) = flow {
        emit(Resource.Loading())
        emit(safeApiCall { apiService.updateReview(id, request) })
    }

    override fun deleteReview(id: String) = flow {
        emit(Resource.Loading())
        emit(safeApiCall { apiService.deleteReview(id) })
    }

    override fun getReviewList(menuItemId: String) = flow {
        emit(Resource.Loading())
        emit(safeApiCall { apiService.getReviewList(ReviewListRequest(menuItemId)) })
    }
}
