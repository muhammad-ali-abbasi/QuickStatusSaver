package com.techseedrive.quickstatussaver.ui.screens

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.techseedrive.quickstatussaver.ui.components.StatusScreenSwitcher

@Composable
fun WhatsAppStatusScreen(navController: NavHostController) {
    // SAF treeUri encoded string (you may want to store this from SAF picker)
    StatusScreenSwitcher(isBusiness = false, navController = navController)
}
