package com.example.jetpackcompose.utils.notification

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class NotificationType {
    SUCCESS,
    ERROR,
    WARNING,
    INFO,
    LOADING
}

data class NotificationData(
    val id: Long = System.currentTimeMillis(),
    val title: String? = null,
    val message: String,
    val type: NotificationType,
    val duration: Long = 3500L,
    val dismissible: Boolean = true
)

fun NotificationType.getIcon(): ImageVector? {
    return when (this) {
        NotificationType.SUCCESS -> Icons.Default.CheckCircle
        NotificationType.ERROR -> Icons.Default.Error
        NotificationType.WARNING -> Icons.Default.Warning
        NotificationType.INFO -> Icons.Default.Info
        NotificationType.LOADING -> null
    }
}

fun NotificationType.getColor(): Color {
    return when (this) {
        NotificationType.SUCCESS -> Color(0xFF4CAF50)
        NotificationType.ERROR -> Color(0xFFF44336)
        NotificationType.WARNING -> Color(0xFFFFC107)
        NotificationType.INFO -> Color(0xFF2196F3)
        NotificationType.LOADING -> Color(0xFF673AB7)
    }
}
