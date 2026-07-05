package com.example.jetpackcompose.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jetpackcompose.data.datastore.AuthPreference
import com.example.jetpackcompose.data.remote.dto.*
import com.example.jetpackcompose.data.repository.ReviewRepository
import com.example.jetpackcompose.utils.Resource
import com.example.jetpackcompose.utils.UiState
import com.example.jetpackcompose.utils.notification.NotificationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val reviewRepository: ReviewRepository,
    private val authPreference: AuthPreference,
    private val notificationManager: NotificationManager
) : ViewModel() {

    private val _reviewsState = MutableStateFlow<UiState<List<ReviewDto>>>(UiState.Idle)
    val reviewsState = _reviewsState.asStateFlow()

    private val _actionState = MutableSharedFlow<UiState<Unit>>()
    val actionState = _actionState.asSharedFlow()

    val currentUser = authPreference.userData.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), null
    )

    private val _filterRating = MutableStateFlow<Int?>(null) // null means "All"
    val filterRating = _filterRating.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.NEWEST)
    val sortOrder = _sortOrder.asStateFlow()

    val filteredReviews = combine(reviewsState, _filterRating, _sortOrder) { state, filter, sort ->
        if (state is UiState.Success) {
            var list = state.data
            if (filter != null) {
                list = list.filter { it.rating == filter }
            }
            list = when (sort) {
                SortOrder.NEWEST -> list.sortedByDescending { it.createdAt }
                SortOrder.OLDEST -> list.sortedBy { it.createdAt }
                SortOrder.HIGHEST_RATING -> list.sortedByDescending { it.rating }
                SortOrder.LOWEST_RATING -> list.sortedBy { it.rating }
            }
            list
        } else emptyList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val starCounts = reviewsState.map { state ->
        if (state is UiState.Success) {
            val list = state.data
            val counts = mutableMapOf<Int, Int>()
            for (i in 1..5) counts[i] = 0
            list.forEach {
                val r = it.rating ?: 0
                if (r in 1..5) {
                    counts[r] = (counts[r] ?: 0) + 1
                }
            }
            counts
        } else emptyMap()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val averageRating = reviewsState.map { state ->
        if (state is UiState.Success) {
            val list = state.data
            if (list.isEmpty()) 0.0 else list.map { it.rating ?: 0 }.average()
        } else 0.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun getReviews(menuItemId: String) {
        viewModelScope.launch {
            reviewRepository.getReviewList(menuItemId).collect { resource ->
                when (resource) {
                    is Resource.Loading -> _reviewsState.value = UiState.Loading
                    is Resource.Success -> _reviewsState.value = UiState.Success(resource.data ?: emptyList())
                    is Resource.Error -> _reviewsState.value = UiState.Error(resource.message ?: "Unknown error")
                }
            }
        }
    }

    fun setFilter(rating: Int?) {
        _filterRating.value = rating
    }

    fun setSort(order: SortOrder) {
        _sortOrder.value = order
    }

    fun createReview(menuItemId: String, rating: Int, comment: String) {
        viewModelScope.launch {
            val user = currentUser.value
            val request = CreateReviewRequest(
                userId = user?.id,
                user = user?.name ?: "Anonymous",
                menuItemId = menuItemId,
                rating = rating,
                comment = comment
            )
            reviewRepository.createReview(request).collect { resource ->
                handleActionResult(resource, menuItemId)
            }
        }
    }

    fun updateReview(reviewId: String, menuItemId: String, rating: Int, comment: String) {
        viewModelScope.launch {
            val request = UpdateReviewRequest(rating, comment)
            reviewRepository.updateReview(reviewId, request).collect { resource ->
                handleActionResult(resource, menuItemId)
            }
        }
    }

    fun deleteReview(reviewId: String, menuItemId: String) {
        viewModelScope.launch {
            reviewRepository.deleteReview(reviewId).collect { resource ->
                handleActionResult(resource, menuItemId)
            }
        }
    }

    private suspend fun <T> handleActionResult(resource: Resource<T>, menuItemId: String) {
        when (resource) {
            is Resource.Loading -> {
                notificationManager.showLoading()
                _actionState.emit(UiState.Loading)
            }
            is Resource.Success -> {
                notificationManager.hideLoading()
                notificationManager.showSuccess("Thực hiện thành công!")
                _actionState.emit(UiState.Success(Unit))
                getReviews(menuItemId) // Refresh list
            }
            is Resource.Error -> {
                notificationManager.hideLoading()
                val errorMsg = resource.message ?: "Thao tác thất bại"
                notificationManager.showError(errorMsg)
                _actionState.emit(UiState.Error(errorMsg))
            }
        }
    }
}

enum class SortOrder {
    NEWEST, OLDEST, HIGHEST_RATING, LOWEST_RATING
}
