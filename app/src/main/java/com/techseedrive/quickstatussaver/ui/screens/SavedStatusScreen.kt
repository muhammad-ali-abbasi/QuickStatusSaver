package com.techseedrive.quickstatussaver.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.techseedrive.quickstatussaver.model.StatusMedia
import com.techseedrive.quickstatussaver.ui.components.MediaGrid
import com.techseedrive.quickstatussaver.ui.components.MultiSelectActionBar
import com.techseedrive.quickstatussaver.utils.AppUtils
import com.techseedrive.quickstatussaver.utils.ThumbnailPreloader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Cache for saved media that survives navigation
private object SavedMediaCache {
    private var cache: List<StatusMedia>? = null

    fun get(): List<StatusMedia>? = cache
    fun put(data: List<StatusMedia>) {
        cache = data
    }
    fun clear() {
        cache = null
    }
}

@Composable
fun SavedStatusScreen(navController: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var refreshTrigger by remember { mutableStateOf(0) }
    val mediaList = remember { mutableStateListOf<StatusMedia>() }
    var hasLoaded by remember { mutableStateOf(false) }

    // ── Multi-select state ────────────────────────────────────────────────────
    var selectedItems by remember { mutableStateOf<Set<Uri>>(emptySet()) }
    var isSelectionMode by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    // ───────────────────────────────────────────────────────────────

    // Clear cache when screen is first entered to ensure fresh data
    DisposableEffect(Unit) {
        // Clear cache on screen entry to get fresh data after saves from other screens
        SavedMediaCache.clear()
        onDispose { }
    }

    LaunchedEffect(refreshTrigger) {
        val cached = SavedMediaCache.get()
        if (cached != null) {
            mediaList.clear()
            mediaList.addAll(cached)
            hasLoaded = true
            return@LaunchedEffect
        }

        withContext(Dispatchers.IO) {
            val list = querySavedMedia(context)
            withContext(Dispatchers.Main) {
                mediaList.clear()
                mediaList.addAll(list)
                SavedMediaCache.put(list)
                hasLoaded = true
            }
            // Preload thumbnails
            ThumbnailPreloader.preload(context, list)
        }
    }

    // Show grid immediately if data is loaded
    if (hasLoaded) {
        Box(modifier = Modifier.fillMaxSize()) {
            MediaGrid(
                items = mediaList,
                fromSavedStatus = true,
                navController = navController,
                deleteItem = { media ->
                    mediaList.remove(media)
                    SavedMediaCache.clear()
                    refreshTrigger++
                },
                selectedItems = selectedItems,
                isSelectionMode = isSelectionMode,
                onToggleSelection = { media ->
                    selectedItems = if (selectedItems.contains(media.uri)) {
                        val updated = selectedItems - media.uri
                        if (updated.isEmpty()) isSelectionMode = false
                        updated
                    } else {
                        selectedItems + media.uri
                    }
                },
                onSelectionModeChange = { isSelectionMode = it }
            )

            // ── Multi-select action bar (slides up from bottom) ──────────────
            AnimatedVisibility(
                visible = isSelectionMode,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                MultiSelectActionBar(
                    selectedCount = selectedItems.size,
                    totalCount = mediaList.size,
                    isProcessing = isSaving,
                    actionLabel = "Delete",
                    actionIcon = Icons.Default.Delete,
                    onSelectAll = {
                        selectedItems = if (selectedItems.size == mediaList.size) {
                            emptySet()
                        } else {
                            mediaList.map { it.uri }.toSet()
                        }
                    },
                    onConfirmAction = {
                        val toDelete = mediaList.filter { selectedItems.contains(it.uri) }
                        scope.launch {
                            isSaving = true
                            val deletedCount = withContext(Dispatchers.IO) {
                                AppUtils.deleteMultipleMedia(context, toDelete)
                            }
                            Toast.makeText(
                                context,
                                "Deleted $deletedCount of ${toDelete.size} items",
                                Toast.LENGTH_SHORT
                            ).show()
                            isSaving = false
                            selectedItems = emptySet()
                            isSelectionMode = false
                            // Refresh list so deleted items disappear
                            SavedMediaCache.clear()
                            refreshTrigger++
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

private fun querySavedMedia(context: Context): List<StatusMedia> {
    val list = mutableListOf<StatusMedia>()
    val appName = "QuickStatusSaver"

    val collections = listOf(
        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
        MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    )

    collections.forEach { collection ->
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.RELATIVE_PATH
        )

        // Only query files in our app's specific folders
        val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf("%/$appName%")

        context.contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val date = cursor.getLong(dateColumn)
                val uri = Uri.withAppendedPath(collection, id.toString())
                val isVideo = collection == MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

                list.add(StatusMedia(uri, isVideo, name, date))
            }
        }
    }

    return list.sortedByDescending { it.lastModified }
}