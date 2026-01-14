package com.techseedrive.quickstatussaver.ui.screens

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.techseedrive.quickstatussaver.ui.components.StatusTabContent
import kotlinx.coroutines.launch

@OptIn(ExperimentalPagerApi::class)
@Composable
fun StatusTabsScreen(treeUri: Uri, navController: NavHostController) {
    val pagerState = rememberPagerState()
    val tabTitles = listOf("Images", "Videos")
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Log.d("MediaGrid", "StatusTabsScreen: $treeUri")

    Column(modifier = Modifier.fillMaxSize()) {
        // Material3 Tab Row synced with pager
        TabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    text = { Text(title) }
                )
            }
        }

        // HorizontalPager keeps both tabs in memory for instant switching
        HorizontalPager(
            count = 2,
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            StatusTabContent(
                treeUri = treeUri,
                showVideos = page == 1,
                navController = navController
            )
        }
    }
}
