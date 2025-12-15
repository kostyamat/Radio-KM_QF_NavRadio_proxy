package com.android.fmradio.ext

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.util.Log

class NavRadioHelper(private val context: Context) {

    companion object {
        private const val TAG = "NavRadioHelper"

        // The order in this list defines the priority.
        // Paid version is first, so it will be picked if installed.
        private val COMPATIBLE_PACKAGES = listOf(
            "com.navimods.radio",       // NavRadio+ Paid
            "com.navimods.radio_free"  // NavRadio+ Free
        )
    }

    val selectedNavRadioPackage: String? by lazy {
        determineBestRadioApp()
    }

    private fun determineBestRadioApp(): String? {
        Log.d(TAG, "Determining best available radio app...")
        for (packageName in COMPATIBLE_PACKAGES) {
            if (isPackageInstalled(packageName)) {
                Log.d(TAG, "Found installed compatible app: $packageName. Selecting it.")
                return packageName
            }
        }
        Log.w(TAG, "No compatible NavRadio app found.")
        return null
    }

    private fun isPackageInstalled(packageName: String): Boolean {
        return try {
            @Suppress("DEPRECATION") // getPackageInfo is deprecated on newer APIs, but fine for API 29
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * Finds the ComponentName of the media button receiver for the given package.
     * This is crucial for correctly dispatching media key events on Android 10.
     */
    fun getMediaButtonReceiverComponent(packageName: String): ComponentName? {
        val intent = Intent(Intent.ACTION_MEDIA_BUTTON)
        intent.setPackage(packageName)

        val pm = context.packageManager
        @Suppress("DEPRECATION")
        val receivers: List<ResolveInfo> = pm.queryBroadcastReceivers(intent, PackageManager.GET_RESOLVED_FILTER)

        if (receivers.isNotEmpty()) {
            val receiverInfo = receivers.first().activityInfo
            if (receiverInfo != null) {
                val componentName = ComponentName(packageName, receiverInfo.name)
                Log.d(TAG, "Found media button receiver for $packageName: ${componentName.flattenToString()}")
                return componentName
            }
        }

        Log.w(TAG, "Could not find media button receiver for $packageName")
        return null
    }
}
