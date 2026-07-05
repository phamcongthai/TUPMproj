package com.example.jetpackcompose.presentation.screen.orders

import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jetpackcompose.data.remote.dto.OrderItemDto
import com.example.jetpackcompose.data.remote.dto.OrderResponse
import com.example.jetpackcompose.utils.OrderUtils
import com.example.jetpackcompose.utils.UiState
import com.example.jetpackcompose.viewmodel.OrdersViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(viewModel: OrdersViewModel) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Đang xử lý", "Hoàn thành", "Đã hủy")
    val context = LocalContext.current

    val uncompleted by viewModel.uncompletedOrders.collectAsState()
    val completed by viewModel.completedOrders.collectAsState()
    val cancelled by viewModel.cancelledOrders.collectAsState()

    var selectedOrderForTracking by remember { mutableStateOf<OrderResponse?>(null) }

    val isRefreshing = (uncompleted is UiState.Loading && selectedTabIndex == 0) ||
                      (completed is UiState.Loading && selectedTabIndex == 1) ||
                      (cancelled is UiState.Loading && selectedTabIndex == 2)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Đơn hàng của tôi", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize().background(Color(0xFFF8F9FA))) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.White,
                contentColor = Color(0xFFFF5722),
                indicator = { tabPositions ->
                    if (selectedTabIndex < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = Color(0xFFFF5722)
                        )
                    }
                },
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { 
                            Text(
                                title, 
                                fontSize = 14.sp,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                            ) 
                        },
                        selectedContentColor = Color(0xFFFF5722),
                        unselectedContentColor = Color.Gray
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = { viewModel.fetchAllOrders() },
                    modifier = Modifier.fillMaxSize()
                ) {
                    val currentState = when (selectedTabIndex) {
                        0 -> uncompleted
                        1 -> completed
                        else -> cancelled
                    }

                    Crossfade(targetState = currentState, label = "OrderListFade") { state ->
                        when (state) {
                            is UiState.Loading -> if (!isRefreshing) ShimmerOrderList()
                            is UiState.Success -> {
                                val orders = state.data
                                if (orders.isEmpty()) {
                                    EmptyOrdersState()
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(orders, key = { it.id ?: "" }) { order ->
                                            OrderCard(
                                                order = order,
                                                onTrackClick = { selectedOrderForTracking = order },
                                                onConfirmClick = { viewModel.completeOrder(order.id!!) }
                                            )
                                        }
                                    }
                                }
                            }
                            is UiState.Error -> ErrorState(state.message) { viewModel.fetchAllOrders() }
                            else -> {}
                        }
                    }
                }
            }
        }
    }

    if (selectedOrderForTracking != null) {
        OrderDetailModal(
            order = selectedOrderForTracking!!,
            onDismiss = { selectedOrderForTracking = null }
        )
    }
}

@Composable
fun OrderCard(order: OrderResponse, onTrackClick: () -> Unit, onConfirmClick: () -> Unit) {
    val shortId = order.id?.takeLast(6)?.uppercase() ?: "N/A"
    val progress = OrderUtils.getProgress(order)
    val currentStatus = order.tracking?.lastOrNull()?.status ?: "pending"
    val methodLabel = if (order.method == "pickup") "Đến lấy" else "Giao hàng"
    val paymentLabel = if (order.payment == "cod") "COD" else "Bank Transfer"
    
    val canConfirm = if (order.method == "ship") {
        currentStatus == "delivering" || currentStatus == "completed"
    } else {
        currentStatus == "ready" || currentStatus == "completed"
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth().clickable { onTrackClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Color(0xFFFFF3E0),
                        shape = CircleShape,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (order.method == "pickup") Icons.Default.Storefront else Icons.Default.DeliveryDining,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = Color(0xFFFF5722)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Mã đơn: #$shortId", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Text(OrderUtils.formatDateTime(order.createdAt), fontSize = 12.sp, color = Color.Gray)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoChip(text = methodLabel, icon = Icons.Default.LocationOn)
                InfoChip(text = paymentLabel, icon = Icons.Default.Payments)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Text(
                    text = getStatusText(currentStatus, order.method ?: "ship"),
                    fontSize = 14.sp,
                    color = if (currentStatus == "cancelled") Color.Red else Color(0xFFFF5722),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                color = if (currentStatus == "cancelled") Color.Red else Color(0xFFFF5722),
                trackColor = Color(0xFFF5F5F5),
                strokeCap = StrokeCap.Round
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFEEEEEE))
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Tổng thanh toán", fontSize = 11.sp, color = Color.Gray)
                    Text(OrderUtils.formatPrice(order.totalPayment), fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = Color.Black)
                }
                
                Row {
                    OutlinedButton(
                        onClick = onTrackClick,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5722)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF5722))
                    ) {
                        Text("Theo dõi", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    if (currentStatus != "cancelled" && currentStatus != "completed") {
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onConfirmClick,
                            enabled = canConfirm,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF5722),
                                disabledContainerColor = Color(0xFFF5F5F5),
                                disabledContentColor = Color.LightGray
                            )
                        ) {
                            Text("Đã nhận", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InfoChip(text: String, icon: ImageVector) {
    Surface(
        color = Color(0xFFF5F5F5),
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, modifier = Modifier.size(14.dp), tint = Color.Gray)
            Spacer(modifier = Modifier.width(4.dp))
            Text(text, fontSize = 11.sp, color = Color.DarkGray, fontWeight = FontWeight.Medium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailModal(order: OrderResponse, onDismiss: () -> Unit) {
    val shortId = order.id?.takeLast(6)?.uppercase() ?: "N/A"
    val steps = OrderUtils.getTimelineSteps(order)
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 40.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Chi tiết đơn #$shortId", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Order Info Section
            SectionTitle("THÔNG TIN ĐƠN HÀNG")
            InfoRow("Mã đơn", "#$shortId")
            InfoRow("Ngày đặt", OrderUtils.formatDateTime(order.createdAt))
            InfoRow("Hình thức", if (order.method == "pickup") "Đến lấy tại quán" else "Giao hàng tận nơi")
            if (order.method == "pickup") {
                InfoRow("Giờ đến lấy", order.pickupTime ?: "N/A")
            } else {
                InfoRow("Địa chỉ giao", order.deliveryAddress ?: "N/A")
            }
            InfoRow("Thanh toán", if (order.payment == "cod") "COD (Tiền mặt)" else "Chuyển khoản")
            
            Spacer(modifier = Modifier.height(28.dp))
            
            // Timeline Section
            SectionTitle("TRẠNG THÁI ĐƠN HÀNG")
            Column(modifier = Modifier.padding(top = 12.dp, start = 4.dp)) {
                steps.forEachIndexed { index, step ->
                    TimelineItemModern(
                        step = step,
                        isLast = index == steps.size - 1
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(28.dp))
            
            // Order Items Section
            SectionTitle("DANH SÁCH MÓN")
            order.orderItems?.forEach { item ->
                OrderItemRow(item)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(thickness = 1.dp, color = Color(0xFFF1F1F1))
            Spacer(modifier = Modifier.height(16.dp))
            
            PriceSummaryRow("Tổng cộng món", OrderUtils.formatPrice(order.totalItemPrice))
            PriceSummaryRow("Phí giao hàng & Phí khác", OrderUtils.formatPrice(order.totalPayment - order.totalItemPrice))
            
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("TỔNG THANH TOÁN", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color.Black)
                Text(OrderUtils.formatPrice(order.totalPayment), fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = Color(0xFFFF5722))
            }
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 14.sp, color = Color.Gray)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(1f).padding(start = 16.dp), color = Color.DarkGray)
    }
}

@Composable
fun OrderItemRow(item: OrderItemDto) {
    val totalPrice = OrderUtils.calculateItemPrice(item)
    
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(item.name ?: "Unknown Item", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("x${item.quantity}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Text(OrderUtils.formatPrice(totalPrice), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black)
        }
        if (item.variant != null) {
            Text("Size: ${item.variant.size ?: "N/A"}", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 2.dp))
        }
        if (item.toppings != null && item.toppings.isNotEmpty()) {
            Text("Toppings: ${item.toppings.joinToString { it.name ?: "" }}", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
fun PriceSummaryRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 14.sp, color = Color.Gray)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.DarkGray)
    }
}

@Composable
fun TimelineItemModern(step: com.example.jetpackcompose.utils.TimelineStep, isLast: Boolean) {
    val color = when {
        step.status == "cancelled" -> Color.Red
        step.isCurrent -> Color(0xFFFF5722)
        step.isCompleted -> Color(0xFFFF9800)
        else -> Color.LightGray
    }
    
    Row(modifier = Modifier.height(IntrinsicSize.Min)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(if (step.isCurrent) 22.dp else 18.dp)
                    .clip(CircleShape)
                    .background(if (step.isCurrent || step.isCompleted || step.status == "cancelled") color else Color.White)
                    .then(if (!step.isCurrent && !step.isCompleted && step.status != "cancelled") Modifier.border(2.dp, Color.LightGray, CircleShape) else Modifier),
                contentAlignment = Alignment.Center
            ) {
                if (step.status == "cancelled") {
                    Icon(Icons.Default.Close, null, modifier = Modifier.size(12.dp), tint = Color.White)
                } else if (step.isCompleted && !step.isCurrent) {
                    Icon(Icons.Default.Check, null, modifier = Modifier.size(12.dp), tint = Color.White)
                } else if (step.isCurrent) {
                    Box(modifier = Modifier.size(8.dp).background(Color.White, CircleShape))
                }
            }
            if (!isLast) {
                Box(modifier = Modifier.width(2.5.dp).weight(1f).background(if (step.isCompleted) color else Color(0xFFF1F1F1)))
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.padding(bottom = 28.dp).fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    step.title, 
                    color = if (step.isCurrent || step.status == "cancelled") color else if (step.isCompleted) Color.Black else Color.LightGray,
                    fontWeight = if (step.isCurrent || step.isCompleted) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 15.sp
                )
                Text(
                    step.timestamp ?: "--:--",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun EmptyOrdersState() {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.AutoMirrored.Filled.ReceiptLong, null, modifier = Modifier.size(100.dp), tint = Color(0xFFEEEEEE))
        Spacer(modifier = Modifier.height(16.dp))
        Text("Chưa có đơn hàng nào", color = Color.Gray, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.ErrorOutline, null, modifier = Modifier.size(60.dp), tint = Color.Red.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(16.dp))
        Text("Rất tiếc, đã có lỗi xảy ra", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(message, textAlign = TextAlign.Center, color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRetry, 
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Thử lại ngay", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ShimmerOrderList() {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        repeat(4) { ShimmerOrderItem() }
    }
}

@Composable
fun ShimmerOrderItem() {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    val shimmerColors = listOf(
        Color(0xFFF1F1F1),
        Color(0xFFE0E0E0),
        Color(0xFFF1F1F1),
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim, y = translateAnim)
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth().height(180.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(brush))
    }
}

fun getStatusText(status: String, method: String): String {
    return when (status.lowercase()) {
        "pending", "confirmed" -> "Nhà hàng đã nhận đơn"
        "preparing" -> "Đang chuẩn bị món"
        "ready" -> if (method == "pickup") "Món đã sẵn sàng - Hãy đến lấy" else "Món đã sẵn sàng - Chờ giao"
        "delivering" -> "Đang giao hàng"
        "completed" -> "Đã hoàn thành"
        "cancelled" -> "Đã hủy"
        else -> status.replaceFirstChar { it.uppercase() }
    }
}
