package com.example.quickstatussaver.ui.components

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.quickstatussaver.model.StatusMedia
import com.example.quickstatussaver.utils.StatusMediaLoader
import com.example.quickstatussaver.utils.ThumbnailPreloader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Cache that survives navigation
private object MediaCache {
    private val cache = mutableMapOf<String, List<StatusMedia>>()

    fun get(key: String): List<StatusMedia>? = cache[key]
    fun put(key: String, data: List<StatusMedia>) {
        cache[key] = data
    }
}

@Composable
fun StatusTabContent(
    treeUri: Uri,
    showVideos: Boolean,
    navController: NavHostController
) {
    val context = LocalContext.current
    val cacheKey = treeUri.toString()

    // Check cache first - instant display if available
    val cachedData = remember(cacheKey) { MediaCache.get(cacheKey) }
    var allMedia by remember(cacheKey) { mutableStateOf(cachedData) }
    var isPreloaded by remember(cacheKey) { mutableStateOf(false) }

    // Load data only if not in cache
    LaunchedEffect(cacheKey) {
        if (allMedia != null) return@LaunchedEffect // Already have data

        // Load on IO dispatcher
        withContext(Dispatchers.IO) {
            val loaded = StatusMediaLoader.loadStatusFromMediaRoot(context, treeUri)

            withContext(Dispatchers.Main) {
                allMedia = loaded
                MediaCache.put(cacheKey, loaded) // Cache for next time
            }

            // Preload thumbnails in background
            if (!isPreloaded) {
                ThumbnailPreloader.preload(context, loaded)
                withContext(Dispatchers.Main) {
                    isPreloaded = true
                }
            }
        }
    }

    // Show grid immediately if data is available
    val currentMedia = allMedia
    if (currentMedia != null) {
        MediaGrid(
            items = currentMedia,
            fromSavedStatus = false,
            navController = navController
        )
    } else {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

