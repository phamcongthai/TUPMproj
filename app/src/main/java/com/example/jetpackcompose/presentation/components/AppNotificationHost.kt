package com.example.jetpackcompose.presentation.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.jetpackcompose.utils.notification.NotificationData
import com.example.jetpackcompose.utils.notification.NotificationManager
import com.example.jetpackcompose.utils.notification.NotificationType
import com.example.jetpackcompose.utils.notification.getColor
import com.example.jetpackcompose.utils.notification.getIcon
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun AppNotificationHost(
    notificationManager: NotificationManager,
    modifier: Modifier = Modifier
) {
    val currentNotification = remember { mutableStateOf<NotificationData?>(null) }
    val isLoading by notificationManager.isLoading.collectAsState()

    LaunchedEffect(Unit) {
        notificationManager.notifications.collect { data ->
            currentNotification.value = data
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(100f)
            .padding(16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        // Loading Indicator Overlay
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .zIndex(200f),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = Color(0xFFFF5722))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Đang xử lý...", fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        // Notification Banner
        AnimatedVisibility(
            visible = currentNotification.value != null,
            enter = slideInVertically(
                initialOffsetY = { -it },
                animationSpec = tween(durationMillis = 500)
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = tween(durationMillis = 500)
            ) + fadeOut()
        ) {
            currentNotification.value?.let { data ->
                AppNotificationItem(
                    data = data,
                    onDismiss = { currentNotification.value = null }
                )

                LaunchedEffect(data.id) {
                    if (data.duration > 0) {
                        delay(data.duration)
                        currentNotification.value = null
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNotificationItem(
    data: NotificationData,
    onDismiss: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            data.type.getIcon()?.let { icon ->
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(data.type.getColor().copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = data.type.getColor(),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            }

            // Content
            Column(modifier = Modifier.weight(1f)) {
                if (!data.title.isNullOrEmpty()) {
                    Text(
                        text = data.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.Black
                    )
                }
                Text(
                    text = data.message,
                    fontSize = 14.sp,
                    color = Color.DarkGray
                )
            }

            // Dismiss Button
            if (data.dismissible) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
