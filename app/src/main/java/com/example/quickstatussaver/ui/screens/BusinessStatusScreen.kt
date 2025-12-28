package com.example.quickstatussaver.ui.screens

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.example.quickstatussaver.ui.components.StatusScreenSwitcher

@Composable
fun BusinessStatusScreen(navController:  NavHostController) {
    StatusScreenSwitcher(isBusiness = true, navController = navController)
}
