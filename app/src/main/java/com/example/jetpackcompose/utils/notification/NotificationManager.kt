package com.example.jetpackcompose.utils.notification

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationManager @Inject constructor() {

    private val _notifications = MutableSharedFlow<NotificationData>()
    val notifications = _notifications.asSharedFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    suspend fun showSuccess(message: String, title: String? = "Thành công") {
        _notifications.emit(
            NotificationData(
                title = title,
                message = message,
                type = NotificationType.SUCCESS
            )
        )
    }

    suspend fun showError(message: String, title: String? = "Lỗi") {
        _notifications.emit(
            NotificationData(
                title = title,
                message = message,
                type = NotificationType.ERROR
            )
        )
    }

    suspend fun showWarning(message: String, title: String? = "Cảnh báo") {
        _notifications.emit(
            NotificationData(
                title = title,
                message = message,
                type = NotificationType.WARNING
            )
        )
    }

    suspend fun showInfo(message: String, title: String? = "Thông báo") {
        _notifications.emit(
            NotificationData(
                title = title,
                message = message,
                type = NotificationType.INFO
            )
        )
    }

    fun showLoading() {
        _isLoading.value = true
    }

    fun hideLoading() {
        _isLoading.value = false
    }
}
