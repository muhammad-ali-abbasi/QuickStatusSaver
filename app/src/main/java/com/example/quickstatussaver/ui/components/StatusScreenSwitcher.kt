package com.example.quickstatussaver.ui.components

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.*
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.example.quickstatussaver.ui.screens.StatusTabsScreen

@Composable
fun StatusScreenSwitcher(
    isBusiness: Boolean = false,
    navController: NavHostController
) {
    var treeUri by remember { mutableStateOf<Uri?>(null) }

    SAFPermissionRequester(
        isBusiness = isBusiness
    ) {
        treeUri = it
    }
    Log.d("MediaGrid", "StatusScreenSwitcher: $treeUri")
    treeUri?.let {
        StatusTabsScreen(treeUri = it, navController)
    }
}


