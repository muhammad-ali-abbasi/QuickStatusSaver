package com.example.quickstatussaver.utils

import android.content.Context
import android.content.SharedPreferences

class PermissionManager(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("AppPreferenc", Context.MODE_PRIVATE)

    // Save the permission state (whether granted or not)
    fun setPermissionGranted(granted: Boolean) {
        sharedPreferences.edit().putBoolean("permissions_granted", granted).apply()
    }

    // Get the permission state
    fun isPermissionGranted(): Boolean {
        return sharedPreferences.getBoolean("permissions_granted", false)
    }



}
