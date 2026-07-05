package com.example.jetpackcompose.presentation.screen.orderdetail

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.jetpackcompose.data.remote.dto.ReviewDto
import com.example.jetpackcompose.presentation.components.*
import com.example.jetpackcompose.utils.Constants
import com.example.jetpackcompose.utils.UiState
import com.example.jetpackcompose.viewmodel.FoodDetailViewModel
import com.example.jetpackcompose.viewmodel.ReviewViewModel
import com.example.jetpackcompose.viewmodel.SortOrder

@Composable
fun OrderDetailScreen(
    viewModel: FoodDetailViewModel,
    reviewViewModel: ReviewViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val quantity by viewModel.quantity.collectAsState()
    val selectedVariant by viewModel.selectedVariant.collectAsState()
    val selectedToppings by viewModel.selectedToppings.collectAsState()

    val reviewsState by reviewViewModel.reviewsState.collectAsState()
    val filteredReviews by reviewViewModel.filteredReviews.collectAsState()
    val starCounts by reviewViewModel.starCounts.collectAsState()
    val averageRating by reviewViewModel.averageRating.collectAsState()
    val filterRating by reviewViewModel.filterRating.collectAsState()
    val sortOrder by reviewViewModel.sortOrder.collectAsState()
    val currentUser by reviewViewModel.currentUser.collectAsState()

    var editingReview by remember { mutableStateOf<ReviewDto?>(null) }
    var deletingReview by remember { mutableStateOf<ReviewDto?>(null) }
    
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val menuId = (state as? UiState.Success)?.data?.id ?: ""

    LaunchedEffect(menuId) {
        if (menuId.isNotEmpty()) {
            reviewViewModel.getReviews(menuId)
        }
    }

    LaunchedEffect(Unit) {
        reviewViewModel.actionState.collect { actionState ->
            if (actionState is UiState.Success) {
                editingReview = null
                deletingReview = null
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.addToCartEvent.collect { _ ->
            // Success notification is now handled by the viewModel
        }
    }

    Scaffold(
        bottomBar = {
            if (state is UiState.Success) {
                val item = (state as UiState.Success).data
                val isAvailable = item.status == "available"
                
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 16.dp,
                    tonalElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .navigationBarsPadding()
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Quantity Selector
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(if (isAvailable) Color(0xFFF5F5F5) else Color(0xFFEEEEEE), CircleShape)
                                .padding(horizontal = 4.dp, vertical = 4.dp)
                        ) {
                            IconButton(
                                onClick = { viewModel.decrementQuantity() },
                                modifier = Modifier.size(36.dp),
                                enabled = isAvailable
                            ) {
                                Icon(Icons.Default.Remove, null, tint = if (isAvailable) Color(0xFFFF5722) else Color.Gray)
                            }
                            Text(
                                text = quantity.toString(),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.widthIn(min = 30.dp),
                                textAlign = TextAlign.Center,
                                fontSize = 18.sp,
                                color = if (isAvailable) Color.Black else Color.Gray
                            )
                            IconButton(
                                onClick = { viewModel.incrementQuantity() },
                                modifier = Modifier.size(36.dp),
                                enabled = isAvailable
                            ) {
                                Icon(Icons.Default.Add, null, tint = if (isAvailable) Color(0xFFFF5722) else Color.Gray)
                            }
                        }

                        Button(
                            onClick = { viewModel.addToCart() },
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isAvailable) Color.Transparent else Color.LightGray,
                                disabledContainerColor = Color.LightGray
                            ),
                            contentPadding = PaddingValues(),
                            enabled = isAvailable
                        ) {
                            val basePrice = selectedVariant?.price ?: item.price
                            val totalPrice = (basePrice + selectedToppings.sumOf { it.price }) * quantity
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .then(
                                        if (isAvailable) {
                                            Modifier.background(
                                                brush = Brush.horizontalGradient(
                                                    colors = listOf(Color(0xFFFF9800), Color(0xFFFF5722))
                                                ),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                        } else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    if (isAvailable) "Thêm vào giỏ - ${totalPrice}đ" else "Hết hàng",
                                    color = if (isAvailable) Color.White else Color.Gray,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(bottom = innerPadding.calculateBottomPadding())) {
            
            when (val s = state) {
                is UiState.Success -> {
                    val item = s.data
                    val isAvailable = item.status == "available"
                    
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                    ) {
                        // Image Header
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp)
                        ) {
                            AsyncImage(
                                model = Constants.getImageUrl(item.image, "menu"),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                colorFilter = if (!isAvailable) ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
                                    0.33f, 0.33f, 0.33f, 0f, 0f,
                                    0.33f, 0.33f, 0.33f, 0f, 0f,
                                    0.33f, 0.33f, 0.33f, 0f, 0f,
                                    0f, 0f, 0f, 1f, 0f
                                ))) else null
                            )
                            
                            if (!isAvailable) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.5f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Surface(
                                        color = Color.Red.copy(alpha = 0.8f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            "MÓN ĂN HIỆN ĐÃ HẾT HÀNG",
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                            fontWeight = FontWeight.Black,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White)
                                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                                .offset(y = (-32).dp)
                                .padding(24.dp)
                        ) {
                            if (!isAvailable) {
                                Surface(
                                    color = Color(0xFFFFF1F0),
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, Color(0xFFFFA39E))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Info, null, tint = Color.Red)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            "Rất tiếc, món này đã hết hàng hôm nay. Bạn có thể xem các món khác của quán nhé!",
                                            fontSize = 13.sp,
                                            color = Color.Red
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.weight(1f),
                                    color = if (isAvailable) Color.Black else Color.Gray
                                )
                                Surface(
                                    color = Color(0xFFF8F9FA),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Star, null, tint = Color(0xFFFF9800), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = item.averageRating.toString(),
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "${item.price}đ",
                                color = if (isAvailable) Color(0xFFFF5722) else Color.Gray,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black
                            )
                            
                            Spacer(modifier = Modifier.height(20.dp))
                            HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFEEEEEE))
                            Spacer(modifier = Modifier.height(20.dp))
                            
                            Text(
                                text = "Thông tin món ăn",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = item.description,
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color(0xFF666666),
                                lineHeight = 26.sp
                            )

                            // Variants Section
                            if (item.variants.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(28.dp))
                                Text("Chọn Kích cỡ", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                item.variants.forEach { variant ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable(enabled = isAvailable) { viewModel.selectVariant(variant) }
                                            .padding(vertical = 10.dp)
                                    ) {
                                        RadioButton(
                                            selected = selectedVariant?.id == variant.id,
                                            onClick = { if (isAvailable) viewModel.selectVariant(variant) },
                                            colors = RadioButtonDefaults.colors(
                                                selectedColor = Color(0xFFFF5722),
                                                disabledSelectedColor = Color.Gray
                                            ),
                                            enabled = isAvailable
                                        )
                                        Text(
                                            variant.label, 
                                            modifier = Modifier.weight(1f), 
                                            fontSize = 16.sp,
                                            color = if (isAvailable) Color.Black else Color.Gray,
                                            fontWeight = if (selectedVariant?.id == variant.id) FontWeight.Bold else FontWeight.Normal
                                        )
                                        Text(
                                            "${variant.price}đ", 
                                            fontWeight = FontWeight.Bold,
                                            color = if (isAvailable) Color.Black else Color.Gray
                                        )
                                    }
                                }
                            }

                            // Toppings Section
                            if (item.toppings.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(28.dp))
                                Text("Toppings thêm", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                item.toppings.forEach { topping ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable(enabled = isAvailable) { viewModel.toggleTopping(topping) }
                                            .padding(vertical = 10.dp)
                                    ) {
                                        Checkbox(
                                            checked = selectedToppings.any { it.id == topping.id },
                                            onCheckedChange = { if (isAvailable) viewModel.toggleTopping(topping) },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = Color(0xFFFF5722),
                                                disabledCheckedColor = Color.Gray
                                            ),
                                            enabled = isAvailable
                                        )
                                        Text(
                                            topping.name, 
                                            modifier = Modifier.weight(1f), 
                                            fontSize = 16.sp,
                                            color = if (isAvailable) Color.Black else Color.Gray
                                        )
                                        Text(
                                            "+${topping.price}đ", 
                                            color = Color.Gray,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(40.dp))
                            Text("Đánh giá từ khách hàng", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                            Spacer(modifier = Modifier.height(16.dp))

                            RatingSummarySection(
                                averageRating = averageRating,
                                totalReviews = (reviewsState as? UiState.Success)?.data?.size ?: 0,
                                starCounts = starCounts
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            if (currentUser != null) {
                                CreateReviewSection(
                                    onSend = { rating, comment ->
                                        reviewViewModel.createReview(menuId, rating, comment)
                                    }
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            ReviewFilterChips(
                                selectedRating = filterRating,
                                onRatingSelected = { reviewViewModel.setFilter(it) },
                                selectedSort = sortOrder,
                                onSortSelected = { reviewViewModel.setSort(it) }
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            AnimatedContent(
                                targetState = reviewsState,
                                label = "ReviewsAnimation"
                            ) { targetState ->
                                when (targetState) {
                                    is UiState.Success -> {
                                        if (targetState.data.isEmpty()) {
                                            ReviewEmptyState()
                                        } else {
                                            Column {
                                                filteredReviews.forEach { review ->
                                                    val isCurrentUser = remember(review, currentUser) {
                                                        val rUserId = review.realUserId
                                                        val cUserId = currentUser?.id
                                                        
                                                        (rUserId != null && rUserId != "null" && rUserId != "undefined" && rUserId == cUserId) || 
                                                        ((rUserId == null || rUserId == "null" || rUserId == "undefined") && review.user == currentUser?.name)
                                                    }
                                                    ReviewCard(
                                                        review = review,
                                                        isCurrentUser = isCurrentUser,
                                                        onEdit = { editingReview = review },
                                                        onDelete = { deletingReview = review }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    is UiState.Loading -> {
                                        ReviewSkeleton()
                                    }
                                    is UiState.Error -> {
                                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("Không tải được đánh giá", color = Color.Gray)
                                                Button(onClick = { reviewViewModel.getReviews(menuId) }) {
                                                    Text("Thử lại")
                                                }
                                            }
                                        }
                                    }
                                    else -> {}
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(120.dp))
                        }
                    }
                }
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFFFF5722))
                    }
                }
                is UiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(s.message, color = Color.Red)
                    }
                }
                else -> {}
            }

            // Top Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .statusBarsPadding(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        .size(40.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
                }
                IconButton(
                    onClick = { /* Share */ },
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        .size(40.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = Color.White)
                }
            }

            // Dialogs
            editingReview?.let { review ->
                AlertDialog(
                    onDismissRequest = { editingReview = null },
                    title = { Text("Chỉnh sửa đánh giá") },
                    text = {
                        Column {
                            var localRating by remember { mutableIntStateOf(review.rating ?: 5) }
                            var localComment by remember { mutableStateOf(review.comment ?: "") }
                            
                            InteractiveRatingBar(rating = localRating, onRatingChange = { localRating = it })
                            OutlinedTextField(
                                value = localComment,
                                onValueChange = { localComment = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Đánh giá của bạn") }
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Button(
                                onClick = { 
                                    reviewViewModel.updateReview(review.realId ?: "", menuId, localRating, localComment)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Lưu thay đổi")
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(onClick = { editingReview = null }) {
                            Text("Hủy")
                        }
                    }
                )
            }

            deletingReview?.let { review ->
                AlertDialog(
                    onDismissRequest = { deletingReview = null },
                    title = { Text("Xóa đánh giá") },
                    text = { Text("Bạn có chắc chắn muốn xóa đánh giá này không?") },
                    confirmButton = {
                        TextButton(
                            onClick = { 
                                reviewViewModel.deleteReview(review.realId ?: "", menuId)
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                        ) {
                            Text("Xóa")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { deletingReview = null }) {
                            Text("Hủy")
                        }
                    }
                )
            }
        }
    }
}

