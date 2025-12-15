package com.android.fmradio.ext

import android.annotation.SuppressLint
import android.util.Log

object SystemPropertiesReader {

    private const val TAG = "SysPropReader"

    @SuppressLint("PrivateApi")
    fun get(key: String, defaultValue: String = ""): String {
        try {
            val systemProperties = Class.forName("android.os.SystemProperties")
            val getMethod = systemProperties.getMethod("get", String::class.java, String::class.java)
            return getMethod.invoke(null, key, defaultValue) as String
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read system property '$key'", e)
            return defaultValue
        }
    }
}
