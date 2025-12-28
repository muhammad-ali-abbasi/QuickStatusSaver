package com.techseedrive.quickstatussaver.ui.screens

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
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
}

@Composable
fun SavedStatusScreen(navController: NavHostController) {
    val context = LocalContext.current

    // Check cache first for instant display
    val initialData = remember { SavedMediaCache.get() }
    val mediaList = remember { mutableStateListOf<StatusMedia>().apply {
        initialData?.let { addAll(it) }
    }}
    var hasLoaded by remember { mutableStateOf(initialData != null) }

    LaunchedEffect(Unit) {
        if (hasLoaded) return@LaunchedEffect // Already have cached data

        // Load on IO dispatcher
        withContext(Dispatchers.IO) {
            val loaded = getSavedMedia(context)

            withContext(Dispatchers.Main) {
                mediaList.clear()
                mediaList.addAll(loaded)
                SavedMediaCache.put(loaded) // Cache for next time
                hasLoaded = true
            }

            // Preload thumbnails in background
            if (!SavedMediaCache.isPreloaded()) {
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
                mediaList.remove(media)
                SavedMediaCache.put(mediaList.toList()) // Update cache
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
    val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

    val projection = arrayOf(
        MediaStore.Files.FileColumns._ID,
        MediaStore.Files.FileColumns.DISPLAY_NAME,
        MediaStore.Files.FileColumns.MIME_TYPE,
        MediaStore.Files.FileColumns.DATE_MODIFIED
    )

    val selection = "${MediaStore.Files.FileColumns.RELATIVE_PATH} LIKE ?"
    val selectionArgs = arrayOf("Download/QuickStatusSaver/%")

    val cursor = context.contentResolver.query(
        collection,
        projection,
        selection,
        selectionArgs,
        "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"
    )

    cursor?.use {
        val idColumn = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
        val nameColumn = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
        val mimeTypeColumn = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
        val dateModifiedColumn = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)

        while (it.moveToNext()) {
            val id = it.getLong(idColumn)
            val displayName = it.getString(nameColumn)
            val mimeType = it.getString(mimeTypeColumn) ?: ""
            val lastModified = it.getLong(dateModifiedColumn) * 1000  // Convert to milliseconds

            val contentUri = ContentUris.withAppendedId(collection, id)
            val isVideo = mimeType.startsWith("video")
            Log.d("MediaGrid", "contentUri $contentUri")
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

    return mediaList
}