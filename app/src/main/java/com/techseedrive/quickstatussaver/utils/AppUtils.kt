// 📁 utils/AppUtils.kt

package com.techseedrive.quickstatussaver.utils

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import android.widget.Toast
import com.techseedrive.quickstatussaver.model.StatusMedia
import java.io.File

object AppUtils {

    fun isAppInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun promptInstallApp(context: Context, packageName: String) {
        Toast.makeText(
            context,
            "App not installed. Redirecting to Play Store...",
            Toast.LENGTH_SHORT
        ).show()

        val playStoreIntent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
            setPackage("com.android.vending")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(playStoreIntent)
    }

    fun checkAndHandleAppInstall(context: Context, isBusiness: Boolean): Boolean {
        val packageName = if (isBusiness) "com.whatsapp.w4b" else "com.whatsapp"
        return if (isAppInstalled(context, packageName)) {
            true
        } else {
            promptInstallApp(context, packageName)
            false
        }
    }


    fun shareMedia(context: Context, media: StatusMedia) {
        val mimeType = if (media.isVideo) "video/*" else "image/*"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, Uri.parse(media.uri.toString()))
        }
        context.startActivity(Intent.createChooser(intent, "Share Media"))
    }

    fun repostMedia(context: Context, media: StatusMedia) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = if (media.isVideo) "video/*" else "image/*"
            putExtra(Intent.EXTRA_STREAM, Uri.parse(media.uri.toString()))
            setPackage("com.whatsapp")
        }
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "Whatsapp not installed", Toast.LENGTH_SHORT).show()
        }
    }

    fun deleteMedia(context: Context, media: StatusMedia) {
        try {
            val uri = media.uri
            val contentResolver = context.contentResolver

            // Works for media from MediaStore
            val rowsDeleted = contentResolver.delete(uri, null, null)

            if (rowsDeleted > 0) {
                Toast.makeText(context, "Deleted successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to delete", Toast.LENGTH_SHORT).show()
            }

        } catch (e: SecurityException) {
            Toast.makeText(context, "Permission denied to delete", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        } catch (e: Exception) {
            Toast.makeText(context, "Error deleting file", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }


    @SuppressLint("NewApi")
    fun saveMedia(context: Context, media: StatusMedia): Boolean {
        return try {
            // Get file extension and infer MIME type
            val extension = media.displayName.substringAfterLast('.', "").lowercase()
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
                ?: "application/octet-stream"
            val appName = "QuickStatusSaver"
            val relativePath = "Download/$appName"

            // First ensure the directory exists
            val downloadsDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val appDir = File(downloadsDir, appName)
            if (!appDir.exists()) {
                appDir.mkdirs()
            }

            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, media.displayName)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
                put(MediaStore.Downloads.RELATIVE_PATH, relativePath)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }

            val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val fileUri = context.contentResolver.insert(collection, values)

            fileUri?.let { uri ->
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    context.contentResolver.openInputStream(media.uri)?.use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }

                // Make file visible
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                context.contentResolver.update(uri, values, null, null)

                Toast.makeText(context, "Saved to Downloads/$appName", Toast.LENGTH_LONG).show()
                return true
            }

            Toast.makeText(context, "Failed to create file", Toast.LENGTH_SHORT).show()
            false
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error saving file: ${e.message}", Toast.LENGTH_SHORT).show()
            false
        }
    }
}