package com.techseedrive.quickstatussaver.utils

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.techseedrive.quickstatussaver.model.StatusMedia

object StatusMediaLoader {

    fun loadStatusFromMediaRoot(context: Context, mediaRootUri: Uri): List<StatusMedia> {
        val pickedDir = DocumentFile.fromTreeUri(context, mediaRootUri) ?: return emptyList()

        // Try WhatsApp .Statuses folder first
        var statusDir = pickedDir.findFile(".Statuses")

        // If not found, use the root directory itself
        if (statusDir == null || !statusDir.isDirectory) {
            statusDir = pickedDir
        }

        return statusDir.listFiles().mapNotNull { file ->
            val name = file.name ?: return@mapNotNull null

            // Skip non-media files
            if (file.isDirectory) return@mapNotNull null

            val isVideo = name.endsWith(".mp4", ignoreCase = true) ||
                         name.endsWith(".mov", ignoreCase = true)
            val isImage = name.endsWith(".jpg", ignoreCase = true) ||
                         name.endsWith(".jpeg", ignoreCase = true) ||
                         name.endsWith(".png", ignoreCase = true) ||
                         name.endsWith(".webp", ignoreCase = true)

            if (isVideo || isImage) {
                StatusMedia(
                    uri = file.uri,
                    isVideo = isVideo,
                    displayName = name,
                    lastModified = file.lastModified()
                )
            } else null
        }.sortedByDescending { it.lastModified }
    }

}
