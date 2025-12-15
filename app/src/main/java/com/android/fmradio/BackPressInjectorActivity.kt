package com.android.fmradio

import android.app.Activity
import android.app.Instrumentation
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent

/**
 * An invisible activity whose only purpose is to inject a KEYCODE_BACK event.
 * It does NOT interact with audio focus, allowing the underlying app to handle the back press naturally.
 */
class BackPressInjectorActivity : Activity() {
    companion object {
        private const val TAG = "FmProxy"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "BackPressInjectorActivity created. Quietly injecting KEYCODE_BACK.")

        // --- The "Now go away" part ---
        Thread {
            try {
                val inst = Instrumentation()
                inst.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
                Log.d(TAG, "Successfully injected KEYCODE_BACK from injector activity.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to inject KEYCODE_BACK from injector activity.", e)
            }
        }.start()

        // Finish this activity immediately.
        finish()
    }
}
