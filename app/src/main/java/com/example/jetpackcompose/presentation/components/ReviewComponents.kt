package com.example.jetpackcompose.presentation.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.jetpackcompose.R
import com.example.jetpackcompose.data.remote.dto.ReviewDto
import com.example.jetpackcompose.utils.OrderUtils
import com.example.jetpackcompose.viewmodel.SortOrder

import java.util.Locale

@Composable
fun RatingSummarySection(
    averageRating: Double,
    totalReviews: Int,
    starCounts: Map<Int, Int>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = String.format(Locale.getDefault(), "%.1f", averageRating),
                    style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                RatingBarDisplay(rating = averageRating.toInt(), size = 16.dp)
                Text(
                    text = "$totalReviews đánh giá",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Column(modifier = Modifier.weight(2f)) {
                for (i in 5 downTo 1) {
                    val count = starCounts[i] ?: 0
                    val progress = if (totalReviews > 0) count.toFloat() / totalReviews else 0f
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 1.dp)
                    ) {
                        Text(text = "$i", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(12.dp))
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFB400),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .weight(1f)
                                .height(8.dp)
                                .clip(CircleShape),
                            color = Color(0xFFFFB400),
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "$count", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun RatingBarDisplay(
    rating: Int,
    maxRating: Int = 5,
    size: androidx.compose.ui.unit.Dp = 24.dp
) {
    Row {
        for (i in 1..maxRating) {
            Icon(
                imageVector = if (i <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = null,
                tint = if (i <= rating) Color(0xFFFFB400) else Color.Gray,
                modifier = Modifier.size(size)
            )
        }
    }
}

@Composable
fun InteractiveRatingBar(
    rating: Int,
    onRatingChange: (Int) -> Unit,
    size: androidx.compose.ui.unit.Dp = 32.dp
) {
    Row {
        for (i in 1..5) {
            IconButton(
                onClick = { onRatingChange(i) },
                modifier = Modifier.size(size + 8.dp)
            ) {
                Icon(
                    imageVector = if (i <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = null,
                    tint = if (i <= rating) Color(0xFFFFB400) else Color.Gray,
                    modifier = Modifier.size(size)
                )
            }
        }
    }
}

@Composable
fun ReviewCard(
    review: ReviewDto,
    isCurrentUser: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val isEditable = OrderUtils.isEditable(review.createdAt)

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = if (review.avatar.isNullOrEmpty()) "https://www.w3schools.com/howto/img_avatar.png" else review.avatar,
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = review.user ?: "Anonymous",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        if (isCurrentUser) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = CircleShape
                            ) {
                                Text(
                                    text = "Bạn",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                    RatingBarDisplay(rating = review.rating ?: 0, size = 14.dp)
                }
                
                if (isCurrentUser) {
                    Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Review options",
                                tint = Color.Gray
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Chỉnh sửa") },
                                onClick = {
                                    showMenu = false
                                    onEdit()
                                },
                                enabled = isEditable,
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Xóa", color = Color.Red) },
                                onClick = {
                                    showMenu = false
                                    onDelete()
                                },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) }
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = review.comment ?: "", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = OrderUtils.formatTimeAgo(review.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                if (isCurrentUser && !isEditable) {
                   Text(
                       text = "Chỉ được sửa trong 5 phút",
                       style = MaterialTheme.typography.labelSmall,
                       color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                   )
                }
            }
        }
    }
}

@Composable
fun CreateReviewSection(
    onSend: (Int, String) -> Unit
) {
    var rating by remember { mutableIntStateOf(5) }
    var comment by remember { mutableStateOf("") }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Bạn nghĩ món này thế nào?", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            InteractiveRatingBar(rating = rating, onRatingChange = { rating = it })
            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Nhập đánh giá của bạn...") },
                minLines = 2
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { 
                    if (comment.isNotBlank()) {
                        onSend(rating, comment)
                        comment = ""
                        rating = 5
                    }
                },
                modifier = Modifier.align(Alignment.End),
                enabled = comment.isNotBlank()
            ) {
                Text("Gửi đánh giá")
            }
        }
    }
}

@Composable
fun ReviewFilterChips(
    selectedRating: Int?,
    onRatingSelected: (Int?) -> Unit,
    selectedSort: SortOrder,
    onSortSelected: (SortOrder) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = selectedRating == null,
                    onClick = { onRatingSelected(null) },
                    label = { Text("Tất cả") }
                )
            }
            items(5) { index ->
                val star = 5 - index
                FilterChip(
                    selected = selectedRating == star,
                    onClick = { onRatingSelected(star) },
                    label = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("$star")
                            Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                    }
                )
            }
        }
        
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(SortOrder.entries) { order ->
                FilterChip(
                    selected = selectedSort == order,
                    onClick = { onSortSelected(order) },
                    label = { 
                        Text(when(order) {
                            SortOrder.NEWEST -> "Mới nhất"
                            SortOrder.OLDEST -> "Cũ nhất"
                            SortOrder.HIGHEST_RATING -> "Điểm cao"
                            SortOrder.LOWEST_RATING -> "Điểm thấp"
                        })
                    }
                )
            }
        }
    }
}

@Composable
fun ReviewSkeleton() {
    Column {
        repeat(3) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .padding(vertical = 8.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(Color.LightGray.copy(alpha = 0.3f))
            )
        }
    }
}

@Composable
fun ReviewEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.RateReview,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Chưa có đánh giá nào",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "Hãy là người đầu tiên đánh giá món ăn này.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
    }
}
