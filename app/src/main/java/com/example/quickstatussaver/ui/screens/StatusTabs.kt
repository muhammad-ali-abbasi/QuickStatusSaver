package com.example.quickstatussaver.ui.screens

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.example.quickstatussaver.ui.components.StatusTabContent

@Composable
fun StatusTabsScreen(treeUri: Uri, navController: NavHostController) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("Images", "Videos")
    val context = LocalContext.current
    Log.d("MediaGrid", "StatusTabsScreen: $treeUri")
    Column {

        StatusTabContent(
            treeUri = treeUri,
            showVideos = selectedTab == 1,
            navController = navController
        )
    }
}
