package com.example.jetpackcompose.presentation.screen.register

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jetpackcompose.utils.UiState
import com.example.jetpackcompose.viewmodel.RegisterViewModel

@Composable
fun RegisterScreen(
    viewModel: RegisterViewModel,
    onNavigateToLogin: () -> Unit
) {
    val name by viewModel.name.collectAsState()
    val email by viewModel.email.collectAsState()
    val phone by viewModel.phone.collectAsState()
    val password by viewModel.password.collectAsState()
    val confirmPassword by viewModel.confirmPassword.collectAsState()
    val registerState by viewModel.registerState.collectAsState()

    val isNameValid by viewModel.isNameValid.collectAsState()
    val isEmailValid by viewModel.isEmailValid.collectAsState()
    val isPhoneValid by viewModel.isPhoneValid.collectAsState()
    val isPasswordValid by viewModel.isPasswordValid.collectAsState()
    val isConfirmPasswordValid by viewModel.isConfirmPasswordValid.collectAsState()

    var passwordVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(registerState) {
        if (registerState is UiState.Success) {
            onNavigateToLogin()
            viewModel.resetState()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Tạo tài khoản mới",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = name,
            onValueChange = viewModel::onNameChange,
            label = { Text("Họ và tên") },
            modifier = Modifier.fillMaxWidth(),
            isError = !isNameValid,
            supportingText = { if (!isNameValid) Text("Tên phải có ít nhất 2 ký tự") },
            leadingIcon = { Icon(Icons.Default.Person, null) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = email,
            onValueChange = viewModel::onEmailChange,
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            isError = !isEmailValid,
            supportingText = { if (!isEmailValid) Text("Email không hợp lệ") },
            leadingIcon = { Icon(Icons.Default.Email, null) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = phone,
            onValueChange = viewModel::onPhoneChange,
            label = { Text("Số điện thoại") },
            modifier = Modifier.fillMaxWidth(),
            isError = !isPhoneValid,
            supportingText = { if (!isPhoneValid) Text("Số điện thoại không hợp lệ (10-11 số)") },
            leadingIcon = { Icon(Icons.Default.Phone, null) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = viewModel::onPasswordChange,
            label = { Text("Mật khẩu") },
            modifier = Modifier.fillMaxWidth(),
            isError = !isPasswordValid,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null
                    )
                }
            },
            supportingText = {
                if (!isPasswordValid) Text("Ít nhất 8 ký tự, 1 hoa, 1 thường, 1 số, 1 đặc biệt")
            },
            leadingIcon = { Icon(Icons.Default.Lock, null) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = viewModel::onConfirmPasswordChange,
            label = { Text("Nhập lại mật khẩu") },
            modifier = Modifier.fillMaxWidth(),
            isError = !isConfirmPasswordValid,
            visualTransformation = PasswordVisualTransformation(),
            supportingText = { if (!isConfirmPasswordValid) Text("Mật khẩu không khớp") },
            leadingIcon = { Icon(Icons.Default.Lock, null) }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = viewModel::register,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            enabled = registerState !is UiState.Loading
        ) {
            if (registerState is UiState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Đăng ký")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        TextButton(onClick = onNavigateToLogin) {
            Text("Quay lại trang đăng nhập")
        }
    }
}
