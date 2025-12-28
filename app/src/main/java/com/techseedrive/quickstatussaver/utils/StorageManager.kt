package com.techseedrive.quickstatussaver.utils

// utils/StorageManager.kt

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri

object StorageManager {
    private const val PREFS_NAME = "status_saver_prefs"
    private const val WHATSAPP_URI_KEY = "whatsapp_tree_uri"
    private const val BUSINESS_URI_KEY = "business_tree_uri"

    fun saveTreeUri(context: Context, uri: Uri, isBusiness: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(if (isBusiness) BUSINESS_URI_KEY else WHATSAPP_URI_KEY, uri.toString())
            .apply()
    }

    fun getSavedTreeUri(context: Context, isBusiness: Boolean): Uri? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val uriString = prefs.getString(if (isBusiness) BUSINESS_URI_KEY else WHATSAPP_URI_KEY, null)
        return uriString?.let { Uri.parse(it) }
    }
}
