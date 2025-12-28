// utils/ThumbnailPreloader.kt
package com.example.quickstatussaver.utils

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import com.example.quickstatussaver.model.StatusMedia
import com.example.quickstatussaver.ui.components.ThumbnailCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

object ThumbnailPreloader {
    /**
     * Preload thumbnails for both images and videos
     * Only preloads first 30 items for instant display
     */
    suspend fun preload(context: Context, items: List<StatusMedia>) = withContext(Dispatchers.IO) {
        // Preload both images and videos
        items.take(30).chunked(15).forEach { batch ->
            batch.map { media ->
                async {
                    if (ThumbnailCache.get(media.uri) != null) return@async

                    val thumb: Bitmap? = try {
                        if (media.isVideo) {
                            // Extract frame from video using MediaMetadataRetriever
                            val retriever = MediaMetadataRetriever()
                            var result: Bitmap? = null
                            try {
                                retriever.setDataSource(context, media.uri)
                                val frame = retriever.getFrameAtTime(
                                    1000000, // Get frame at 1 second
                                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                                )
                                result = frame?.let { Bitmap.createScaledBitmap(it, 400, 400, true) }
                            } finally {
                                try { retriever.release() } catch (_: Exception) {}
                            }
                            result
                        } else {
                            // For images
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                                context.contentResolver.loadThumbnail(
                                    media.uri,
                                    android.util.Size(400, 400),
                                    null
                                )
                            } else {
                                val options = android.graphics.BitmapFactory.Options().apply {
                                    inJustDecodeBounds = false
                                    inSampleSize = 2
                                    inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
                                }
                                context.contentResolver.openInputStream(media.uri)?.use { inputStream ->
                                    android.graphics.BitmapFactory.decodeStream(inputStream, null, options)?.let {
                                        Bitmap.createScaledBitmap(it, 400, 400, true)
                                    }
                                }
                            }
                        }
                    } catch (_: Exception) { null }

                    thumb?.let { ThumbnailCache.put(media.uri, it) }
                }
            }.awaitAll()
        }
    }
}
