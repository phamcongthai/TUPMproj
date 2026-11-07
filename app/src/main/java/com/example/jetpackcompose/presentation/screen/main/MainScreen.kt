package com.example.jetpackcompose.presentation.screen.main

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.jetpackcompose.navigation.Screen
import com.example.jetpackcompose.presentation.component.BottomNavigationBar
import com.example.jetpackcompose.presentation.component.LocationPermissionCard
import com.example.jetpackcompose.presentation.screen.account.AccountScreen
import com.example.jetpackcompose.presentation.screen.cart.CartScreen
import com.example.jetpackcompose.presentation.screen.home.HomeScreen
import com.example.jetpackcompose.presentation.screen.menu.MenuScreen
import com.example.jetpackcompose.presentation.screen.orders.OrdersScreen
import com.example.jetpackcompose.viewmodel.*

@Composable
fun MainScreen(
    onLogout: () -> Unit, 
    onNavigateToDetail: (String) -> Unit, 
    onNavigateToCheckout: () -> Unit
) {
    val navController = rememberNavController()
    val homeViewModel: HomeViewModel = hiltViewModel()
    val showPermissionPopup by homeViewModel.shouldShowLocationPopup.collectAsStateWithLifecycle()
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        homeViewModel.markLocationPopupShown()
    }

    Scaffold(
        bottomBar = { BottomNavigationBar(navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToDetail = onNavigateToDetail
                )
            }
            composable(Screen.Menu.route) {
                val viewModel: MenuViewModel = hiltViewModel()
                MenuScreen(viewModel, homeViewModel, onNavigateToDetail)
            }
            composable(Screen.Cart.route) {
                val viewModel: CartViewModel = hiltViewModel()
                CartScreen(
                    viewModel = viewModel,
                    onBack = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(0)
                        }
                    },
                    onNavigateToCheckout = onNavigateToCheckout
                )
            }
            composable(Screen.Account.route) {
                val viewModel: AccountViewModel = hiltViewModel()
                AccountScreen(viewModel, onLogout)
            }
            composable(Screen.Orders.route) {
                val viewModel: OrdersViewModel = hiltViewModel()
                OrdersScreen(viewModel)
            }
        }

        if (showPermissionPopup) {
            LocationPermissionCard(
                onAllow = {
                    permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                },
                onDismiss = { homeViewModel.markLocationPopupShown() }
            )
        }
    }
}
