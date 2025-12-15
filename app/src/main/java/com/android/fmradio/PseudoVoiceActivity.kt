package com.android.fmradio

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * An invisible activity that handles the VOICE_COMMAND intent to trigger a media session update.
 * It grabs and releases audio focus to force a media state re-evaluation, then goes home.
 */
class PseudoVoiceActivity : Activity() {
    companion object {
        private const val TAG = "FmProxy"
        private const val FINISH_DELAY_MS = 250L
    }

    private val handler = Handler(Looper.getMainLooper())
    // A single listener instance is required for request and abandon
    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "PseudoVoiceActivity created. Requesting audio focus and going home.")

        // --- The "Everyone be quiet!" part (your brilliant idea) ---
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val result = audioManager.requestAudioFocus(
            focusChangeListener,
            AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
        )
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Log.d(TAG, "Audio focus grabbed and released.")
            audioManager.abandonAudioFocus(focusChangeListener)
        } else {
            Log.w(TAG, "Failed to grab audio focus.")
        }
        // --- End of the audio focus part ---

        // Immediately send the user to the home screen.
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(homeIntent)

        // Delay finishing this activity to ensure the system processes everything.
        handler.postDelayed({
            Log.d(TAG, "Finishing PseudoVoiceActivity now.")
            finish()
        }, FINISH_DELAY_MS)
    }
}
