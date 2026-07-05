package com.example.jetpackcompose.presentation.screen.checkout

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jetpackcompose.utils.UiState
import com.example.jetpackcompose.viewmodel.CheckoutViewModel
import com.example.jetpackcompose.viewmodel.DeliveryMethod
import com.example.jetpackcompose.viewmodel.PaymentMethod
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    viewModel: CheckoutViewModel,
    onBack: () -> Unit,
    onNavigateHome: () -> Unit
) {
    val method by viewModel.method.collectAsState()
    val payment by viewModel.payment.collectAsState()
    val address by viewModel.address.collectAsState()
    val pickupTime by viewModel.pickupTime.collectAsState()
    val note by viewModel.note.collectAsState()
    val distanceKm by viewModel.distanceKm.collectAsState()
    val shipFee by viewModel.shipFee.collectAsState()
    val totalItemPrice by viewModel.totalItemPrice.collectAsState()
    val totalPayment by viewModel.totalPayment.collectAsState()
    val checkoutState by viewModel.checkoutState.collectAsState()

    LaunchedEffect(checkoutState) {
        if (checkoutState is UiState.Success) {
            viewModel.resetState()
            onNavigateHome()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Thanh toán", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .background(Color(0xFFF8F8F8))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Delivery Method Section
            CheckoutCard(title = "Phương thức nhận hàng") {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = method == DeliveryMethod.PICKUP,
                        onClick = { viewModel.setMethod(DeliveryMethod.PICKUP) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text("Đến lấy")
                    }
                    SegmentedButton(
                        selected = method == DeliveryMethod.SHIP,
                        onClick = { viewModel.setMethod(DeliveryMethod.SHIP) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text("Giao hàng")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (method == DeliveryMethod.PICKUP) {
                    PickupSection(pickupTime) { viewModel.setPickupTime(it) }
                } else {
                    ShipSection(address, distanceKm, shipFee) { viewModel.setAddress(it) }
                }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { viewModel.setNote(it) },
                    label = { Text("Ghi chú cho quán") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    )
                )
            }

            // Payment Method Section
            CheckoutCard(title = "Phương thức thanh toán") {
                PaymentOption(
                    title = "Tiền mặt (COD)",
                    selected = payment == PaymentMethod.COD,
                    icon = Icons.Default.Payments,
                    onClick = { viewModel.setPayment(PaymentMethod.COD) }
                )
                PaymentOption(
                    title = "Chuyển khoản ngân hàng",
                    selected = payment == PaymentMethod.BANK,
                    icon = Icons.Default.AccountBalance,
                    onClick = { viewModel.setPayment(PaymentMethod.BANK) }
                )
            }

            // Order Summary
            CheckoutCard(title = "Tổng cộng") {
                SummaryRow("Tổng tiền món", "${totalItemPrice}đ")
                if (method == DeliveryMethod.SHIP) {
                    SummaryRow("Phí giao hàng (${distanceKm} km)", "${shipFee}đ")
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Tổng thanh toán", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(
                        "${totalPayment}đ",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color(0xFFFF9800)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { viewModel.placeOrder() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(),
                enabled = checkoutState !is UiState.Loading
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color(0xFFFF9800), Color(0xFFFF5722))
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (checkoutState is UiState.Loading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("XÁC NHẬN ĐẶT MÓN", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun CheckoutCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
fun PickupSection(pickupTime: String, onTimeSelected: (String) -> Unit) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    
    val timePicker = TimePickerDialog(
        context,
        { _, hour, minute ->
            onTimeSelected(String.format("%02d:%02d", hour, minute))
        },
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        true
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { timePicker.show() }
            .background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.AccessTime, null, tint = Color(0xFFFF9800))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text("Thời gian đến lấy", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Text(
                if (pickupTime.isEmpty()) "Chọn thời gian" else pickupTime,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ShipSection(address: String, distance: Double, shipFee: Long, onAddressChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = address,
            onValueChange = onAddressChange,
            label = { Text("Địa chỉ giao hàng") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.LocationOn, null, tint = Color(0xFFFF9800)) },
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
            singleLine = true
        )

        if (distance > 0) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Route, null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Khoảng cách: ${distance} km", fontSize = 13.sp, color = Color.Gray)
                }
                Text("Phí ship: ${shipFee}đ", fontSize = 13.sp, color = Color(0xFFFF9800), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PaymentOption(title: String, selected: Boolean, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = if (selected) Color(0xFFFF9800) else Color.Gray)
        Spacer(modifier = Modifier.width(12.dp))
        Text(title, modifier = Modifier.weight(1f))
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFFF9800))
        )
    }
}

@Composable
fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.Gray)
        Text(value, fontWeight = FontWeight.Medium)
    }
}
