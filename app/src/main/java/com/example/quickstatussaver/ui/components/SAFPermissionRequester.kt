package com.example.quickstatussaver.ui.components

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.example.quickstatussaver.utils.AppUtils
import com.example.quickstatussaver.utils.PreferencesUtils

@Composable
fun SAFPermissionRequester(
    isBusiness: Boolean = false,
    onFolderSelected: (Uri) -> Unit
) {
    val context = LocalContext.current

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

    // Rest of your existing SAF permission request code...
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
    }


    val initialUri = getPreloadUri(context, isBusiness)

    // Launch on first composition
    LaunchedEffect(Unit) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            putExtra(DocumentsContract.EXTRA_INITIAL_URI, initialUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        launcher.launch(intent)
    }
}



// Helper function to get initial SAF Uri
fun getPreloadUri(context: Context, isBusiness: Boolean): Uri {
    val encodedPath = when {
        isBusiness -> "Android%2Fmedia%2Fcom.whatsapp.w4b%2FWhatsApp%20Business%2FMedia"
        else -> "Android%2Fmedia%2Fcom.whatsapp%2FWhatsApp%2FMedia"
    }

    return Uri.parse("content://com.android.externalstorage.documents/document/primary:$encodedPath")
}
