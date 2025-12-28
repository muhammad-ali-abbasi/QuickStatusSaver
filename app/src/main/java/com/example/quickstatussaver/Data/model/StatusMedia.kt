// model/StatusMedia.kt
package com.example.quickstatussaver.model

import android.net.Uri
import androidx.compose.runtime.Immutable

@Immutable
data class StatusMedia(
    val uri: Uri,
    val isVideo: Boolean,
    val displayName: String,
    val lastModified: Long,
)
