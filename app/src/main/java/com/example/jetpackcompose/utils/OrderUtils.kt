package com.example.jetpackcompose.utils

import com.example.jetpackcompose.data.remote.dto.OrderItemDto
import com.example.jetpackcompose.data.remote.dto.OrderResponse
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

data class TimelineStep(
    val title: String,
    val status: String,
    val isCompleted: Boolean,
    val isCurrent: Boolean,
    val timestamp: String? = null
)

object OrderUtils {
    fun getProgress(order: OrderResponse): Float {
        val currentStatus = order.tracking?.lastOrNull()?.status ?: "pending"
        if (currentStatus == "cancelled") return 0f
        
        return when (order.method) {
            "pickup" -> when (currentStatus) {
                "pending" -> 0.2f
                "confirmed" -> 0.4f
                "preparing" -> 0.6f
                "ready" -> 0.8f
                "completed" -> 1.0f
                else -> 0f
            }
            "ship" -> when (currentStatus) {
                "pending" -> 0.15f
                "confirmed" -> 0.3f
                "preparing" -> 0.5f
                "ready" -> 0.7f
                "delivering" -> 0.9f
                "completed" -> 1.0f
                else -> 0f
            }
            else -> 0f
        }
    }

    fun getTimelineSteps(order: OrderResponse): List<TimelineStep> {
        val method = order.method ?: "ship"
        val currentStatus = order.tracking?.lastOrNull()?.status ?: "pending"
        
        val steps = mutableListOf<Pair<String, String>>()
        steps.add("pending" to "Nhà hàng nhận đơn")
        steps.add("preparing" to "Chuẩn bị món")
        steps.add("ready" to "Món đã sẵn sàng")
        if (method == "ship") {
            steps.add("delivering" to "Đang giao")
        }
        steps.add("completed" to "Hoàn thành")

        if (currentStatus == "cancelled") {
            // Find when it was cancelled if possible, but the requirement says if tracking has cancelled, timeline stops.
            // Usually, we show the steps up to when it was cancelled, then the cancelled step.
            val result = mutableListOf<TimelineStep>()
            
            // Add steps that were completed before cancellation
            for (step in steps) {
                val trackingItem = order.tracking?.find { it.status == step.first }
                if (trackingItem != null) {
                    result.add(TimelineStep(step.second, step.first, true, false, formatTimeOnly(trackingItem.timestamp ?: trackingItem.updatedAt)))
                }
            }
            
            // Add cancelled step
            val cancelledItem = order.tracking?.find { it.status == "cancelled" }
            result.add(TimelineStep("Đơn hàng đã bị huỷ", "cancelled", true, true, formatTimeOnly(cancelledItem?.timestamp ?: cancelledItem?.updatedAt)))
            
            return result
        }

        return steps.map { step ->
            val trackingItem = order.tracking?.find { it.status == step.first }
            // Specially handle "pending" step which can be satisfied by "confirmed" status too
            val isStepCompleted = if (step.first == "pending") {
                order.tracking?.any { it.status == "pending" || it.status == "confirmed" } == true
            } else {
                order.tracking?.any { it.status == step.first } == true
            }
            
            val isCurrent = step.first == currentStatus || (step.first == "pending" && currentStatus == "confirmed")
            
            val displayTimestamp = if (isStepCompleted) {
                val item = trackingItem ?: order.tracking?.find { it.status == "confirmed" && step.first == "pending" }
                formatTimeOnly(item?.timestamp ?: item?.updatedAt)
            } else "--:--"

            TimelineStep(
                title = step.second,
                status = step.first,
                isCompleted = isStepCompleted,
                isCurrent = isCurrent,
                timestamp = displayTimestamp
            )
        }
    }

    fun formatPrice(price: Long): String {
        val formatter = NumberFormat.getInstance(Locale("vi", "VN"))
        return "${formatter.format(price)}đ"
    }

    fun formatDateTime(dateTimeString: String?): String {
        if (dateTimeString == null) return "N/A"
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val date = inputFormat.parse(dateTimeString)
            val outputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            dateTimeString
        }
    }

    fun formatTimeAgo(dateTimeString: String?): String {
        if (dateTimeString == null) return ""
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val date = inputFormat.parse(dateTimeString) ?: return ""
            val now = Date()
            val diff = now.time - date.time

            when {
                diff < 60 * 1000 -> "Vừa xong"
                diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)} phút trước"
                diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)} giờ trước"
                diff < 2 * 24 * 60 * 60 * 1000 -> "Hôm qua"
                else -> {
                    val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    outputFormat.format(date)
                }
            }
        } catch (e: Exception) {
            ""
        }
    }

    fun isEditable(createdAt: String?): Boolean {
        if (createdAt == null) return false
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val date = inputFormat.parse(createdAt) ?: return false
            val now = Date()
            val diff = now.time - date.time
            diff <= 5 * 60 * 1000 // 5 minutes
        } catch (e: Exception) {
            false
        }
    }

    fun formatTimeOnly(dateTimeString: String?): String {
        if (dateTimeString == null) return "--:--"
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val date = inputFormat.parse(dateTimeString)
            val outputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            "--:--"
        }
    }

    fun calculateItemPrice(item: OrderItemDto): Long {
        val variantPrice = item.variant?.price ?: item.price
        val toppingsPrice = item.toppings?.sumOf { it.price } ?: 0L
        return (variantPrice + toppingsPrice) * item.quantity
    }
}
