// utils/ThumbnailCache.kt
package com.example.quickstatussaver.utils

import android.graphics.Bitmap
import android.net.Uri
import android.util.LruCache

object ThumbnailCache {
    // Use 1/3 of available memory for thumbnail cache - more cache = smoother scrolling
    private val cacheSize = (Runtime.getRuntime().maxMemory() / 1024 / 3).toInt()
    private val lruCache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount / 1024
        }
    }

    fun get(uri: Uri): Bitmap? = lruCache.get(uri.toString())

    fun put(uri: Uri, bitmap: Bitmap) {
        if (get(uri) == null) {
            lruCache.put(uri.toString(), bitmap)
        }
    }

    fun clear() = lruCache.evictAll()
}
