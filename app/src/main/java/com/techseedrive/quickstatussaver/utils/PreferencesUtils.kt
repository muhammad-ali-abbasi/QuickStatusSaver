package com.techseedrive.quickstatussaver.utils

import android.content.Context
import android.net.Uri
import android.preference.PreferenceManager

object PreferencesUtils {

    private const val KEY_WHATSAPP_URI = "key_whatsapp"
    private const val KEY_WHATSAPP_BUSINESS_URI = "key_whatsapp_business"
    private const val KEY_FIRST_RUN = "key_first_run"

    // Initialize this in your Application class
    fun init(context: Context) {
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
        val isFirstRun = sharedPrefs.getBoolean(KEY_FIRST_RUN, true)

        if (isFirstRun) {
            // Clear all preferences on first run (after install/reinstall)
            clearAllPreferences(context)
            sharedPrefs.edit().putBoolean(KEY_FIRST_RUN, false).apply()
        }
    }

    // Clear all preferences (useful for uninstall)
    private fun clearAllPreferences(context: Context) {
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .clear()
            .apply()
    }
    // Save the URI for WhatsApp

    fun saveWhatsAppUri(context: Context, uri: Uri) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        sharedPreferences.edit().putString(KEY_WHATSAPP_URI, uri.toString()).apply()
    }
    // Save the URI for WhatsApp Business
    fun saveWhatsAppBusinessUri(context: Context, uri: Uri) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        sharedPreferences.edit().putString(KEY_WHATSAPP_BUSINESS_URI, uri.toString()).apply()
    }

    // Retrieve the saved URI for WhatsApp
    fun getSavedWhatsAppUri(context: Context): Uri? {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val uriString = sharedPreferences.getString(KEY_WHATSAPP_URI, null) ?: return null
        return Uri.parse(uriString)
    }

    // Retrieve the saved URI for WhatsApp Business
    fun getSavedWhatsAppBusinessUri(context: Context): Uri? {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val uriString = sharedPreferences.getString(KEY_WHATSAPP_BUSINESS_URI, null) ?: return null
        return Uri.parse(uriString)
    }

    // Clear the saved URI for WhatsApp
    fun clearWhatsAppUri(context: Context) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        sharedPreferences.edit().remove(KEY_WHATSAPP_URI).apply()
    }

    // Clear the saved URI for WhatsApp Business
    fun clearWhatsAppBusinessUri(context: Context) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        sharedPreferences.edit().remove(KEY_WHATSAPP_BUSINESS_URI).apply()
    }
}
