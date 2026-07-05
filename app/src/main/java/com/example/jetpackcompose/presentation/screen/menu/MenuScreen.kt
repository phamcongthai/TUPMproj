package com.example.jetpackcompose.presentation.screen.menu

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jetpackcompose.data.model.MenuItem
import com.example.jetpackcompose.presentation.screen.home.AddToCartDialog
import com.example.jetpackcompose.presentation.screen.home.CategorySection
import com.example.jetpackcompose.presentation.screen.home.MenuItemCard
import com.example.jetpackcompose.utils.UiState
import com.example.jetpackcompose.viewmodel.HomeViewModel
import com.example.jetpackcompose.viewmodel.MenuViewModel

@Composable
fun MenuScreen(
    viewModel: MenuViewModel,
    homeViewModel: HomeViewModel, // To reuse addToCart logic
    onNavigateToDetail: (String) -> Unit
) {
    val categoriesState by viewModel.categoriesState.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val menuItemsState by viewModel.filteredMenuItems.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var selectedItemForDialog by remember { mutableStateOf<MenuItem?>(null) }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(Color.White)) {
                Text(
                    "Thực đơn",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = viewModel::onSearchQueryChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    placeholder = { Text("Tìm kiếm món ăn...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                Icon(Icons.Default.Clear, null)
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Color(0xFFF5F5F5),
                        focusedContainerColor = Color(0xFFF5F5F5),
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = Color(0xFFFF9800)
                    )
                )
                CategorySection(
                    state = categoriesState,
                    selectedCategory = selectedCategory,
                    onSelect = viewModel::onCategorySelected
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize().background(Color(0xFFF5F5F5))) {
            AnimatedContent(
                targetState = menuItemsState,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                },
                label = "MenuGridAnimation"
            ) { targetState ->
                when (targetState) {
                    is UiState.Success -> {
                        val items = targetState.data
                        if (items.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Không tìm thấy món ăn nào", color = Color.Gray)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(items.chunked(2)) { rowItems ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                        MenuItemCard(
                                            item = rowItems[0],
                                            modifier = Modifier.weight(1f),
                                            onCardClick = { onNavigateToDetail(rowItems[0].id) },
                                            onAddClick = { selectedItemForDialog = rowItems[0] }
                                        )
                                        if (rowItems.size > 1) {
                                            MenuItemCard(
                                                item = rowItems[1],
                                                modifier = Modifier.weight(1f),
                                                onCardClick = { onNavigateToDetail(rowItems[1].id) },
                                                onAddClick = { selectedItemForDialog = rowItems[1] }
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
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color(0xFFFF9800))
                        }
                    }
                    is UiState.Error -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(targetState.message, color = Color.Red)
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    selectedItemForDialog?.let { item ->
        AddToCartDialog(
            menuItem = item,
            onDismiss = { selectedItemForDialog = null },
            onConfirm = { quantity, variant, toppings ->
                homeViewModel.addToCart(item, quantity, variant, toppings)
                selectedItemForDialog = null
            }
        )
    }
}
