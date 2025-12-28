package com.techseedrive.quickstatussaver.ui.screens

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.techseedrive.quickstatussaver.ui.components.StatusScreenSwitcher

@Composable
fun BusinessStatusScreen(navController:  NavHostController) {
    StatusScreenSwitcher(isBusiness = true, navController = navController)
}
