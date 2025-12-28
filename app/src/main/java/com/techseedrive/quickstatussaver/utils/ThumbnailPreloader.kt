package com.techseedrive.quickstatussaver.utils

import android.content.Context
import coil.imageLoader
import coil.request.ImageRequest
import com.techseedrive.quickstatussaver.model.StatusMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ThumbnailPreloader {
    suspend fun preload(context: Context, mediaList: List<StatusMedia>) {
        withContext(Dispatchers.IO) {
            val imageLoader = context.imageLoader
            mediaList.forEach { media ->
                try {
                    val request = ImageRequest.Builder(context)
                        .data(media.uri)
                        .build()
                    imageLoader.enqueue(request)
                } catch (e: Exception) {
                    // Ignore preload failures
                }
            }
        }
    }
}
