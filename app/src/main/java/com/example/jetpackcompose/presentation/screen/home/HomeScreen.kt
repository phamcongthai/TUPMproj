package com.example.jetpackcompose.presentation.screen.home

import android.app.Activity
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.jetpackcompose.data.model.*
import com.example.jetpackcompose.data.remote.dto.UserDto
import com.example.jetpackcompose.utils.Constants
import com.example.jetpackcompose.utils.UiState
import com.example.jetpackcompose.viewmodel.HomeViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToDetail: (String) -> Unit
) {
    val user by viewModel.user.collectAsStateWithLifecycle()
    val promotionsState by viewModel.promotionsState.collectAsStateWithLifecycle()
    val categoriesState by viewModel.categoriesState.collectAsStateWithLifecycle()
    val menuItemsState by viewModel.filteredMenuItems.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val locationState by viewModel.locationState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val activity = context as? Activity
    var selectedItemForDialog by remember { mutableStateOf<MenuItem?>(null) }

    LaunchedEffect(Unit) {
        viewModel.startLocationUpdate()
    }

    Scaffold { innerPadding ->
        LocationPermissionHandler(viewModel = viewModel) {
            activity?.let {
                viewModel.checkGpsAndStartLocation(it)
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding())
                    .verticalScroll(rememberScrollState())
                    .background(Color(0xFFF8F9FA))
            ) {
                // Header
                val addressText = when (val s = locationState) {
                    is LocationUiState.Success -> s.data.address
                    is LocationUiState.Error -> "Lỗi lấy vị trí"
                    else -> "Đang xác định..."
                }

                HomeHeader(
                    user = user,
                    address = addressText,
                    isLoading = locationState is LocationUiState.GettingLocation || locationState is LocationUiState.ConvertingAddress
                )

                if (locationState is LocationUiState.Error) {
                    LocationErrorSection(locationState as LocationUiState.Error) {
                        viewModel.refreshLocation()
                    }
                }

                // Banner Slider
                PromotionSlider(promotionsState)

                // Features
                FeatureSection()

                // Categories
                CategorySection(categoriesState, selectedCategory) {
                    viewModel.onCategorySelected(it)
                }

                // Menu Items
                MenuItemSection(
                    state = menuItemsState,
                    onNavigate = onNavigateToDetail,
                    onAddToCart = { selectedItemForDialog = it }
                )

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Global Loading Overlay for Location
            if (locationState is LocationUiState.GettingLocation || 
                locationState is LocationUiState.ConvertingAddress ||
                locationState is LocationUiState.CheckingGPS) {
                
                LocationLoadingOverlay(locationState)
            }
        }
    }

    selectedItemForDialog?.let { item ->
        AddToCartDialog(
            menuItem = item,
            onDismiss = { selectedItemForDialog = null },
            onConfirm = { quantity, variant, toppings ->
                viewModel.addToCart(item, quantity, variant, toppings)
                selectedItemForDialog = null
            }
        )
    }
}

@Composable
fun LocationErrorSection(error: LocationUiState.Error, onRetry: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = error.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onRetry) {
                Text("Thử lại", fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun LocationLoadingOverlay(state: LocationUiState) {
    val message = when (state) {
        is LocationUiState.CheckingGPS -> "Đang kiểm tra GPS..."
        is LocationUiState.GettingLocation -> "Đang lấy vị trí của bạn..."
        is LocationUiState.ConvertingAddress -> "Đang xác định địa chỉ..."
        else -> "Vui lòng chờ..."
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    color = Color(0xFFFF5722),
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = message,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.Black
                )
                Text(
                    text = "Quá trình này có thể mất vài giây",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
fun HomeHeader(user: UserDto, address: String, isLoading: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFFF5722), Color(0xFFFF9800))
                )
            )
            .padding(top = 48.dp, bottom = 20.dp, start = 16.dp, end = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            AsyncImage(
                model = if (user.avatar.isNullOrEmpty()) "https://cdn-icons-png.flaticon.com/512/149/149071.png" else user.avatar,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Xin chào, ${user.name ?: "Bạn"} 👋",
                    color = Color.White, 
                    fontSize = 16.sp, 
                    fontWeight = FontWeight.ExtraBold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    if (isLoading) {
                        Box(modifier = Modifier
                            .width(120.dp)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = 0.3f))
                        )
                    } else {
                        Text(
                            text = address,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            IconButton(
                onClick = { /* Notification */ },
                modifier = Modifier.background(Color.White.copy(alpha = 0.2f), CircleShape)
            ) {
                Icon(Icons.Default.Notifications, null, tint = Color.White)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PromotionSlider(state: UiState<List<Promotion>>) {
    when (state) {
        is UiState.Success -> {
            val promotions = state.data
            val pagerState = rememberPagerState(pageCount = { promotions.size })

            LaunchedEffect(Unit) {
                while (true) {
                    delay(4000)
                    if (promotions.isNotEmpty()) {
                        val nextPager = (pagerState.currentPage + 1) % promotions.size
                        pagerState.animateScrollToPage(nextPager)
                    }
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(20.dp))
                ) { page ->
                    val promo = promotions[page]
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = Constants.getImageUrl(promo.image, "promotion"),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(16.dp)
                        ) {
                            Surface(
                                color = Color(0xFFFF5722),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    "Khuyến mãi", 
                                    color = Color.White, 
                                    fontSize = 10.sp, 
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(promo.title, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                            Text(promo.description, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                        }
                    }
                }
            }
        }
        else -> { /* Loading or Error shimmer */ }
    }
}

@Composable
fun FeatureSection() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FeatureCard("🚀", "Siêu tốc", Modifier.weight(1f))
        FeatureCard("🏷️", "Giá tốt", Modifier.weight(1f))
        FeatureCard("💎", "Uy tín", Modifier.weight(1f))
    }
}

@Composable
fun FeatureCard(icon: String, title: String, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFFF8F9FA), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = Color.DarkGray)
        }
    }
}

@Composable
fun CategorySection(
    state: UiState<List<String>>,
    selectedCategory: String,
    onSelect: (String) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(
            "Danh mục món ăn",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        when (state) {
            is UiState.Success -> {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.data) { category ->
                        CategoryChip(category, selectedCategory == category) { onSelect(category) }
                    }
                }
            }
            else -> {}
        }
    }
}

@Composable
fun CategoryChip(name: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) Color(0xFFFF5722) else Color.White,
        shadowElevation = if (isSelected) 4.dp else 1.dp,
        modifier = Modifier.height(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(
                name,
                color = if (isSelected) Color.White else Color(0xFF666666),
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
            )
        }
    }
}

@Composable
fun MenuItemSection(
    state: UiState<List<MenuItem>>,
    onNavigate: (String) -> Unit,
    onAddToCart: (MenuItem) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            "Gợi ý cho bạn",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        
        AnimatedContent(
            targetState = state,
            transitionSpec = {
                fadeIn(animationSpec = tween(400)) togetherWith fadeOut(animationSpec = tween(400))
            },
            label = "MenuAnimation"
        ) { targetState ->
            when (targetState) {
                is UiState.Success -> {
                    val items = targetState.data
                    if (items.isEmpty()) {
                        EmptyMenuState()
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.animateContentSize()
                        ) {
                            for (i in items.indices step 2) {
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    MenuItemCard(
                                        item = items[i], 
                                        modifier = Modifier.weight(1f),
                                        onCardClick = { onNavigate(items[i].id) },
                                        onAddClick = { onAddToCart(items[i]) }
                                    )
                                    if (i + 1 < items.size) {
                                        MenuItemCard(
                                            item = items[i + 1], 
                                            modifier = Modifier.weight(1f),
                                            onCardClick = { onNavigate(items[i+1].id) },
                                            onAddClick = { onAddToCart(items[i+1]) }
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFFFF5722))
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
fun EmptyMenuState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.SearchOff,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Color.LightGray
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Hiện tại không có món ăn nào",
            color = Color.Gray,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun MenuItemCard(item: MenuItem, modifier: Modifier, onCardClick: () -> Unit, onAddClick: () -> Unit) {
    val isAvailable = item.status == "available"
    val cardAlpha by animateFloatAsState(targetValue = if (isAvailable) 1f else 0.6f, label = "alpha")
    
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = isAvailable) { onCardClick() }
            .graphicsLayer { alpha = cardAlpha },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(130.dp)) {
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
                            .background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "HẾT HÀNG",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                } else {
                    Surface(
                        modifier = Modifier.padding(8.dp).align(Alignment.TopStart),
                        color = Color.White.copy(alpha = 0.9f),
                        shape = CircleShape
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Star, null, tint = Color(0xFFFF9800), modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(item.averageRating.toString(), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    item.name,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isAvailable) Color.Black else Color.Gray
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    item.description,
                    fontSize = 11.sp,
                    color = Color.Gray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 15.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${item.price}đ",
                        color = if (isAvailable) Color(0xFFFF5722) else Color.Gray,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                    
                    if (isAvailable) {
                        IconButton(
                            onClick = onAddClick,
                            modifier = Modifier
                                .size(30.dp)
                                .background(
                                    brush = Brush.linearGradient(listOf(Color(0xFFFF9800), Color(0xFFFF5722))),
                                    shape = CircleShape
                                )
                        ) {
                            Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = null,
                            tint = Color.LightGray,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}
