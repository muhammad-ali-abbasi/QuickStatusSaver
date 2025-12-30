// 📁 utils/AppUtils.kt

package com.techseedrive.quickstatussaver.utils

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.RecoverableSecurityException
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
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

    @SuppressLint("NewApi")
    fun deleteMedia(context: Context, media: StatusMedia): IntentSender? {
        try {
            val uri = media.uri
            val contentResolver = context.contentResolver

            Log.d("DeleteMedia", "Attempting to delete: $uri")
            Log.d("DeleteMedia", "Android version: ${Build.VERSION.SDK_INT}")

            // Try to delete the file
            val rowsDeleted = contentResolver.delete(uri, null, null)

            if (rowsDeleted > 0) {
                Log.d("DeleteMedia", "Successfully deleted $rowsDeleted rows")
                Toast.makeText(context, "Deleted successfully", Toast.LENGTH_SHORT).show()
                return null
            } else {
                Log.w("DeleteMedia", "Delete returned 0 rows - file may not be deletable or doesn't exist")
                Toast.makeText(context, "Cannot delete this file", Toast.LENGTH_SHORT).show()
                return null
            }

        } catch (e: RecoverableSecurityException) {
            // On Android 10+, we need user permission for files we don't own
            Log.d("DeleteMedia", "RecoverableSecurityException caught - requesting user permission")
            Toast.makeText(context, "Please grant permission to delete", Toast.LENGTH_SHORT).show()
            return e.userAction.actionIntent.intentSender

        } catch (e: SecurityException) {
            Log.e("DeleteMedia", "SecurityException: ${e.message}", e)
            Toast.makeText(context, "Permission denied to delete", Toast.LENGTH_SHORT).show()
            return null

        } catch (e: Exception) {
            Log.e("DeleteMedia", "Error deleting file: ${e.message}", e)
            Toast.makeText(context, "Error deleting file: ${e.message}", Toast.LENGTH_SHORT).show()
            return null
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

            // Use proper collection based on media type for better delete support
            val (collection, relativePath) = if (media.isVideo) {
                Pair(
                    MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                    "${Environment.DIRECTORY_MOVIES}/$appName"
                )
            } else {
                Pair(
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                    "${Environment.DIRECTORY_PICTURES}/$appName"
                )
            }

            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, media.displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }

            val fileUri = context.contentResolver.insert(collection, values)

            fileUri?.let { uri ->
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    context.contentResolver.openInputStream(media.uri)?.use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }

                // Make file visible
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                context.contentResolver.update(uri, values, null, null)

                val folderName = if (media.isVideo) "Movies" else "Pictures"
                Toast.makeText(context, "Saved to $folderName/$appName", Toast.LENGTH_LONG).show()
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