package com.techseedrive.quickstatussaver.ui.screens

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.techseedrive.quickstatussaver.model.StatusMedia
import com.techseedrive.quickstatussaver.ui.components.MediaGrid
import com.techseedrive.quickstatussaver.utils.ThumbnailPreloader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Cache for saved media that survives navigation
private object SavedMediaCache {
    private var cachedData: List<StatusMedia>? = null
    private var isPreloaded = false

    fun get(): List<StatusMedia>? = cachedData
    fun put(data: List<StatusMedia>) {
        cachedData = data
    }
    fun isPreloaded(): Boolean = isPreloaded
    fun setPreloaded() {
        isPreloaded = true
    }
    fun clear() {
        cachedData = null
        isPreloaded = false
    }
}

@Composable
fun SavedStatusScreen(navController: NavHostController) {
    val context = LocalContext.current

    // Add refresh trigger
    var refreshTrigger by remember { mutableStateOf(0) }

    val mediaList = remember { mutableStateListOf<StatusMedia>() }
    var hasLoaded by remember { mutableStateOf(false) }

    // Clear cache when screen is first entered to ensure fresh data
    DisposableEffect(Unit) {
        // Clear cache on screen entry to get fresh data after saves from other screens
        SavedMediaCache.clear()
        onDispose { }
    }

    LaunchedEffect(refreshTrigger) {
        // Always reload from MediaStore to get latest saved files
        withContext(Dispatchers.IO) {
            val loaded = getSavedMedia(context)

            withContext(Dispatchers.Main) {
                mediaList.clear()
                mediaList.addAll(loaded)
                SavedMediaCache.put(loaded)
                hasLoaded = true
            }

            // Preload thumbnails in background
            if (!SavedMediaCache.isPreloaded() || refreshTrigger > 0) {
                ThumbnailPreloader.preload(context, loaded)
                withContext(Dispatchers.Main) {
                    SavedMediaCache.setPreloaded()
                }
            }
        }
    }

    // Show grid immediately if data is loaded
    if (hasLoaded) {
        MediaGrid(
            items = mediaList,
            fromSavedStatus = true,
            navController = navController,
            deleteItem = { media ->
                // Remove from UI immediately
                mediaList.remove(media)
                // Clear cache and trigger refresh
                SavedMediaCache.clear()
                refreshTrigger++
            }
        )
    } else {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}


@SuppressLint("InlinedApi")
fun getSavedMedia(context: Context): List<StatusMedia> {
    val mediaList = mutableListOf<StatusMedia>()

    // Query both Pictures and Movies folders for QuickStatusSaver files
    val collections: List<Uri> = listOf(
        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
        MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    )

    val projection = arrayOf(
        MediaStore.MediaColumns._ID,
        MediaStore.MediaColumns.DISPLAY_NAME,
        MediaStore.MediaColumns.MIME_TYPE,
        MediaStore.MediaColumns.DATE_MODIFIED,
        MediaStore.MediaColumns.RELATIVE_PATH
    )

    val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ? OR ${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
    val selectionArgs = arrayOf(
        "%Pictures/QuickStatusSaver%",
        "%Movies/QuickStatusSaver%"
    )

    for (collection in collections) {
        val cursor = context.contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
        )

        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameColumn = it.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val mimeTypeColumn = it.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            val dateModifiedColumn = it.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)

            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val displayName = it.getString(nameColumn)
                val mimeType = it.getString(mimeTypeColumn) ?: ""
                val lastModified = it.getLong(dateModifiedColumn) * 1000  // Convert to milliseconds

                val contentUri = ContentUris.withAppendedId(collection, id)
                val isVideo = mimeType.startsWith("video")
                Log.d("SavedMedia", "Found saved file: $contentUri, type: $mimeType")
                mediaList.add(
                    StatusMedia(
                        uri = contentUri,
                        isVideo = isVideo,
                        displayName = displayName,
                        lastModified = lastModified
                    )
                )
            }
        }
    }

    // Sort by date modified descending
    return mediaList.sortedByDescending { it.lastModified }
}