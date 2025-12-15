package com.android.fmradio.ext

import android.app.Activity
import android.os.Bundle
import android.util.Log

/**
 * An invisible activity that handles the VOICE_COMMAND intent to trigger a media session update.
 * It finishes immediately after being created.
 */
class PseudoVoiceActivity : Activity() {
    companion object {
        private const val TAG = "FmProxy"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "PseudoVoiceActivity created, finishing immediately.")
        finish()
    }
}
