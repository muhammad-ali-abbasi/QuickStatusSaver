package com.techseedrive.quickstatussaver.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import com.techseedrive.quickstatussaver.model.StatusMedia
import com.techseedrive.quickstatussaver.utils.AppUtils
import com.techseedrive.quickstatussaver.utils.StatusMediaLoader
import com.techseedrive.quickstatussaver.utils.ThumbnailPreloader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
    val scope = rememberCoroutineScope()
    val cacheKey = treeUri.toString()

    // Check cache first — instant display if available
    val cachedData = remember(cacheKey) { MediaCache.get(cacheKey) }
    var allMedia by remember(cacheKey) { mutableStateOf(cachedData) }
    var isPreloaded by remember(cacheKey) { mutableStateOf(false) }

    // ── Multi-select state ──────────────────────────────────────────────────
    var selectedItems by remember { mutableStateOf<Set<Uri>>(emptySet()) }
    var isSelectionMode by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    // Sync with global state to disable drawer gestures
    LaunchedEffect(isSelectionMode) {
        com.techseedrive.quickstatussaver.GlobalUiState.isSelectionMode = isSelectionMode
    }

    // Reset selection whenever the user switches between Images / Videos tabs
    LaunchedEffect(showVideos) {
        selectedItems = emptySet()
        isSelectionMode = false
    }
    // ───────────────────────────────────────────────────────────────────────

    // Load data only if not in cache
    LaunchedEffect(cacheKey) {
        if (allMedia != null) return@LaunchedEffect

        withContext(Dispatchers.IO) {
            val loaded = StatusMediaLoader.loadStatusFromMediaRoot(context, treeUri)

            withContext(Dispatchers.Main) {
                allMedia = loaded
                MediaCache.put(cacheKey, loaded)
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

    val currentMedia = allMedia
    if (currentMedia != null) {
        val filteredMedia = remember(currentMedia, showVideos) {
            if (showVideos) currentMedia.filter { it.isVideo }
            else currentMedia.filter { !it.isVideo }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            // ── Media grid ──────────────────────────────────────────────────
            MediaGrid(
                items = filteredMedia,
                fromSavedStatus = false,
                navController = navController,
                selectedItems = selectedItems,
                isSelectionMode = isSelectionMode,
                onToggleSelection = { media ->
                    selectedItems = if (selectedItems.contains(media.uri)) {
                        val updated = selectedItems - media.uri
                        // Auto-exit selection mode when last item is deselected
                        if (updated.isEmpty()) isSelectionMode = false
                        updated
                    } else {
                        selectedItems + media.uri
                    }
                },
                onSelectionModeChange = { isSelectionMode = it },
                onBulkSelect = { uris ->
                    selectedItems = selectedItems + uris
                }
            )

            // ── Multi-select action bar (slides up from bottom) ─────────────
            AnimatedVisibility(
                visible = isSelectionMode,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                MultiSelectActionBar(
                    selectedCount = selectedItems.size,
                    totalCount = filteredMedia.size,
                    isProcessing = isSaving,
                    onSelectAll = {
                        selectedItems = if (selectedItems.size == filteredMedia.size) {
                            emptySet()          // deselect all
                        } else {
                            filteredMedia.map { it.uri }.toSet()   // select all
                        }
                    },
                    onConfirmAction = {
                        val toSave = filteredMedia.filter { selectedItems.contains(it.uri) }
                        scope.launch {
                            isSaving = true
                            val savedCount = withContext(Dispatchers.IO) {
                                AppUtils.saveMultipleMedia(context, toSave)
                            }
                            // Show summary toast on main thread
                            Toast.makeText(
                                context,
                                "Saved $savedCount of ${toSave.size} items",
                                Toast.LENGTH_SHORT
                            ).show()
                            isSaving = false
                            selectedItems = emptySet()
                            isSelectionMode = false
                        }
                    },
                    onCancel = {
                        selectedItems = emptySet()
                        isSelectionMode = false
                    }
                )
            }
        }
    } else {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}
