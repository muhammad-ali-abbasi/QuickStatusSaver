package com.techseedrive.quickstatussaver.ui.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.techseedrive.quickstatussaver.utils.AppUtils
import com.techseedrive.quickstatussaver.utils.PreferencesUtils

@Composable
fun SAFPermissionRequester(
    isBusiness: Boolean = false,
    onFolderSelected: (Uri) -> Unit
) {
    val context = LocalContext.current
    var showInstructionScreen by remember { mutableStateOf(false) }

    // Check if app is installed
    if (!AppUtils.checkAndHandleAppInstall(context, isBusiness)) {
        return
    }

    // Check for saved URI
    val savedUri = if (isBusiness) {
        PreferencesUtils.getSavedWhatsAppBusinessUri(context)
    } else {
        PreferencesUtils.getSavedWhatsAppUri(context)
    }

    // Check if we still have permissions for the saved URI
    val hasValidUri = savedUri?.let { uri ->
        val persistedUriPermissions = context.contentResolver.persistedUriPermissions
        persistedUriPermissions.any { it.uri == uri && it.isReadPermission && it.isWritePermission }
    } ?: false

    if (hasValidUri) {
        // If we have a valid URI with permissions, use it
        if (savedUri != null) {
            onFolderSelected(savedUri)
        }
        return
    } else if (savedUri != null) {
        // If URI exists but permissions are gone (after reinstall), clear it
        if (isBusiness) {
            PreferencesUtils.clearWhatsAppBusinessUri(context)
        } else {
            PreferencesUtils.clearWhatsAppUri(context)
        }
    }

    // Permission launcher
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                // Take persistable permissions
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )

                // Save the URI
                if (isBusiness) {
                    PreferencesUtils.saveWhatsAppBusinessUri(context, uri)
                } else {
                    PreferencesUtils.saveWhatsAppUri(context, uri)
                }

                onFolderSelected(uri)
            }
        }
        showInstructionScreen = false
    }

    // Show instruction screen first, don't auto-launch
    if (!showInstructionScreen) {
        showInstructionScreen = true
    }

    if (showInstructionScreen) {
        SAFPermissionInstructionScreen(
            onGrantPermissionClick = {
                val initialUri = getPreloadUri(context, isBusiness)
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                    putExtra(DocumentsContract.EXTRA_INITIAL_URI, initialUri)
                    addFlags(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                    )
                }
                launcher.launch(intent)
            }
        )
    }
}



// Helper function to get initial SAF Uri
fun getPreloadUri(context: Context, isBusiness: Boolean): Uri {
    // Android 11 (API 30) introduced scoped storage with new path structure
    val encodedPath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        // Android 11+ uses Android/media/com.whatsapp/ path
        when {
            isBusiness -> "Android%2Fmedia%2Fcom.whatsapp.w4b%2FWhatsApp%20Business%2FMedia"
            else -> "Android%2Fmedia%2Fcom.whatsapp%2FWhatsApp%2FMedia"
        }
    } else {
        // Android 10 and below use WhatsApp/ path in root of external storage
        when {
            isBusiness -> "WhatsApp%20Business%2FMedia"
            else -> "WhatsApp%2FMedia"
        }
    }

    return Uri.parse("content://com.android.externalstorage.documents/document/primary:$encodedPath")
}
