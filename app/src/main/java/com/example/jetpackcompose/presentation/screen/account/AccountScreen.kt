package com.example.jetpackcompose.presentation.screen.account

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.jetpackcompose.utils.Constants
import com.example.jetpackcompose.utils.UiState
import com.example.jetpackcompose.viewmodel.AccountViewModel

@Composable
fun AccountScreen(viewModel: AccountViewModel, onLogout: () -> Unit) {
    val user by viewModel.user.collectAsState()
    
    var showEditProfile by remember { mutableStateOf(false) }
    var showChangePassword by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(Color(0xFFF5F5F5))
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(vertical = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AsyncImage(
                    model = user.avatar.takeIf { !it.isNullOrEmpty() } ?: "https://cdn-icons-png.flaticon.com/512/149/149071.png",
                    contentDescription = "Avatar",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = user.name ?: "Chưa cập nhật", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(text = user.email ?: "", color = Color.Gray, fontSize = 14.sp)
                
                Surface(
                    color = Color(0xFFFF9800).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(
                        text = user.role?.name ?: "CUSTOMER",
                        color = Color(0xFFFF9800),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Info Card
        Card(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                InfoRow(icon = Icons.Default.Phone, label = "Số điện thoại", value = user.phone ?: "Chưa có")
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp)
                InfoRow(icon = Icons.Default.Person, label = "Giới tính", value = user.gender ?: "Chưa có")
            }
        }

        // Actions
        Card(
            modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column {
                ActionItem(icon = Icons.Default.Edit, title = "Chỉnh sửa thông tin") { showEditProfile = true }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                ActionItem(icon = Icons.Default.MyLocation, title = "Làm mới vị trí") { viewModel.refreshLocation() }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                ActionItem(icon = Icons.Default.Lock, title = "Đổi mật khẩu") { showChangePassword = true }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { viewModel.logout(); onLogout() },
            modifier = Modifier.fillMaxWidth().padding(16.dp).height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Red),
            shape = RoundedCornerShape(12.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
        ) {
            Text("Đăng xuất", fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }

    if (showEditProfile) {
        EditProfileModal(
            viewModel = viewModel,
            onDismiss = { showEditProfile = false }
        )
    }

    if (showChangePassword) {
        ChangePasswordModal(
            viewModel = viewModel,
            onDismiss = { showChangePassword = false }
        )
    }
}

@Composable
fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Color(0xFFFF9800), modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(label, fontSize = 12.sp, color = Color.Gray)
            Text(value, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun ActionItem(icon: ImageVector, title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, modifier = Modifier.weight(1f), fontSize = 15.sp)
        Icon(Icons.Default.KeyboardArrowRight, null, tint = Color.LightGray)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileModal(viewModel: AccountViewModel, onDismiss: () -> Unit) {
    val user by viewModel.user.collectAsState()
    var name by remember { mutableStateOf(user.name ?: "") }
    var phone by remember { mutableStateOf(user.phone ?: "") }
    var gender by remember { mutableStateOf(user.gender ?: "Nam") }
    var avatar by remember { mutableStateOf(user.avatar ?: "") }
    
    val updateState by viewModel.updateProfileState.collectAsState()

    LaunchedEffect(updateState) {
        if (updateState is UiState.Success) {
            viewModel.resetStates()
            onDismiss()
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp).navigationBarsPadding()) {
            Text("Chỉnh sửa thông tin", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Họ và tên") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Số điện thoại") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("Giới tính", fontWeight = FontWeight.Medium)
            Row {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = gender == "Nam", onClick = { gender = "Nam" })
                    Text("Nam")
                }
                Spacer(modifier = Modifier.width(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = gender == "Nữ", onClick = { gender = "Nữ" })
                    Text("Nữ")
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { viewModel.updateProfile(name, phone, gender, avatar) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                enabled = updateState !is UiState.Loading
            ) {
                if (updateState is UiState.Loading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                else Text("Cập nhật")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordModal(viewModel: AccountViewModel, onDismiss: () -> Unit) {
    var oldPass by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var confirmPass by remember { mutableStateOf("") }
    
    val updateState by viewModel.updatePasswordState.collectAsState()

    LaunchedEffect(updateState) {
        if (updateState is UiState.Success) {
            viewModel.resetStates()
            onDismiss()
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp).navigationBarsPadding()) {
            Text("Đổi mật khẩu", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(value = oldPass, onValueChange = { oldPass = it }, label = { Text("Mật khẩu cũ") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = newPass, onValueChange = { newPass = it }, label = { Text("Mật khẩu mới") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = confirmPass, onValueChange = { confirmPass = it }, label = { Text("Nhập lại mật khẩu mới") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { 
                    viewModel.updatePassword(oldPass, newPass, confirmPass)
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                enabled = updateState !is UiState.Loading
            ) {
                if (updateState is UiState.Loading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                else Text("Xác nhận đổi mật khẩu")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
