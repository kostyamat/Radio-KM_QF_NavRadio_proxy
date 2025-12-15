package com.android.fmradio

import android.app.Activity
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.widget.Toast
import com.android.fmradio.ext.NavRadioHelper
import com.android.fmradio.ext.WidgetWatcherService

class FmMainActivity : Activity() {

    companion object {
        private const val TAG = "FmProxy"
        const val ACTION_PERFORM_HACK = "com.android.fmradio.PERFORM_HACK"
        private const val CAROUSEL_REFERRER = "com.qf.framework"
        private const val EXTRA_IS_CLOAK_INTENT = "com.android.fmradio.IS_CLOAK_INTENT"
        private const val ACTION_FINISH_PROXY = "android.intent.action.MODE_SWITCH"
        private const val HACK_DELAY_MS = 350L
        private var watcherServiceStarted = false
    }

    private val handler = Handler(Looper.getMainLooper())
    private var isCarouselLaunch = false
    private var launchHandled = false
    private lateinit var navRadioHelper: NavRadioHelper

    private val finishProxyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_FINISH_PROXY) {
                Log.d(TAG, "Finish broadcast received. Finishing proxy.")
                uncloakAndFinish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "---------- FmMainActivity: onCreate ----------")

        if (intent.action == ACTION_PERFORM_HACK) {
            performWidgetHack()
            return
        }

        navRadioHelper = NavRadioHelper(this)
        startWatcherService()
        updateLaunchSource(intent)
        registerFinishReceiver()
        handleRadioLaunch()
    }

    private fun performWidgetHack() {
        Log.i(TAG, "Performing widget fix hack. Handing off to PseudoVoiceActivity.")
        try {
            val voiceIntent = Intent(Intent.ACTION_VOICE_COMMAND).apply {
                component = ComponentName("com.android.fmradio.ext", "com.android.fmradio.PseudoVoiceActivity")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(voiceIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch PseudoVoiceActivity.", e)
        }
        finish() // Finish immediately and let PseudoVoiceActivity handle the rest.
    }

    private fun startWatcherService() {
        if (!watcherServiceStarted) {
            if (Settings.canDrawOverlays(this)) {
                Log.d(TAG, "Overlay permission is granted. Starting WidgetWatcherService.")
                val serviceIntent = Intent(this, WidgetWatcherService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent)
                } else {
                    startService(serviceIntent)
                }
                watcherServiceStarted = true
            } else {
                Log.w(TAG, "Overlay permission is NOT granted. Cannot start watcher service.")
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d(TAG, "---------- FmMainActivity: onNewIntent ----------")
        if (intent == null) return

        // Handle the widget hack if requested, this is crucial for subsequent calls
        if (intent.action == ACTION_PERFORM_HACK) {
            performWidgetHack()
            return
        }

        if (intent.getBooleanExtra(EXTRA_IS_CLOAK_INTENT, false)) {
            Log.d(TAG, "Cloak intent received, ignoring to prevent loop.")
            return
        }

        setIntent(intent)
        updateLaunchSource(intent)
        launchHandled = false
        handleRadioLaunch()
    }

    private fun updateLaunchSource(launchIntent: Intent) {
        logIntent(launchIntent, "updateLaunchSource")

        if (launchIntent.action == Intent.ACTION_MAIN) {
            val referrerUri: Uri? = referrer
            isCarouselLaunch = (referrerUri?.host == CAROUSEL_REFERRER)
            Log.d(TAG, "Launch source analyzed. IsCarousel: $isCarouselLaunch, Referrer: ${referrerUri?.host}")
        } else {
            isCarouselLaunch = false
        }
    }

    private fun handleRadioLaunch() {
        if (launchHandled) {
            Log.d(TAG, "Launch already handled. Ignoring.")
            return
        }
        launchHandled = true

        val radioPackage = navRadioHelper.selectedNavRadioPackage
        if (radioPackage == null) {
            Toast.makeText(this, "No compatible NavRadio app installed.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        Log.d(TAG, "Target radio package: $radioPackage")
        launchRadioApp(radioPackage)
    }

    private fun launchRadioApp(packageName: String) {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)

        if (launchIntent == null) {
            Log.e(TAG, "Could not get launch intent for $packageName.")
            finish()
            return
        }

        try {
            if (isCarouselLaunch) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            startActivity(launchIntent)
            Log.d(TAG, "Launched $packageName.")

            if (isCarouselLaunch) {
                applyCloak()
            } else {
                Log.d(TAG, "Direct launch. Finishing proxy.")
                finish()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error launching radio activity: $e")
            finish()
        }
    }

    private fun applyCloak() {
        Log.d(TAG, "Carousel launch: Applying transparent cloak.")
        handler.postDelayed({
            if (!isFinishing) {
                val selfIntent = Intent(this, FmMainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    putExtra(EXTRA_IS_CLOAK_INTENT, true)
                }
                startActivity(selfIntent)
                Log.d(TAG, "Brought proxy to front to act as cloak.")
            } else {
                Log.w(TAG, "Activity was finishing. Aborted applying cloak.")
            }
        }, 350)
    }

    private fun uncloakAndFinish() {
        if (!isFinishing) {
            Log.d(TAG, "Uncloaking and finishing proxy activity.")
            finish()
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (isCarouselLaunch && event?.action == MotionEvent.ACTION_DOWN) {
            Log.d(TAG, "Touch event on cloak. Uncloaking.")
            uncloakAndFinish()
            return true
        }
        return super.onTouchEvent(event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (!isCarouselLaunch || event == null || event.action != KeyEvent.ACTION_DOWN) {
            return super.onKeyDown(keyCode, event)
        }

        when (keyCode) {
            KeyEvent.KEYCODE_BACK -> {
                if (!isFinishing) {
                    if (!Settings.canDrawOverlays(this)) {
                        showPermissionDialog()
                    } else {
                        Log.d(TAG, "Back pressed. Launching injector and uncloaking.")
                        val intent = Intent(this, BackPressInjectorActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                        uncloakAndFinish()
                    }
                }
                return true
            }

            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
            KeyEvent.KEYCODE_MEDIA_PLAY,
            KeyEvent.KEYCODE_MEDIA_PAUSE,
            KeyEvent.KEYCODE_MEDIA_NEXT,
            KeyEvent.KEYCODE_MEDIA_PREVIOUS,
            KeyEvent.KEYCODE_MEDIA_STOP -> {
                Log.d(TAG, "Media key ($keyCode) pressed. Forwarding to radio and uncloaking.")
                dispatchMediaKeyEvent(event)
                // Add a small delay before finishing to ensure the broadcast is processed by the system,
                // as finishing the foreground activity immediately can cause issues on some systems.
                handler.postDelayed({ uncloakAndFinish() }, 100)
                return true
            }
        }

        return super.onKeyDown(keyCode, event)
    }

    private fun showPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage("To fix the media widget, the app needs permission to display over other apps. Please enable it in the next screen.")
            .setPositiveButton("Open Settings") { dialog, _ ->
                try {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                } catch (settingsEx: Exception) {
                    Log.e(TAG, "Could not open overlay permission settings.", settingsEx)
                    Toast.makeText(this, "Could not open settings. Please grant permission manually.", Toast.LENGTH_LONG).show()
                }
                dialog.dismiss()
                uncloakAndFinish()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                uncloakAndFinish()
            }
            .setOnCancelListener {
                uncloakAndFinish()
            }
            .setCancelable(false)
            .show()
    }

    private fun dispatchMediaKeyEvent(keyEvent: KeyEvent) {
        val radioPackage = navRadioHelper.selectedNavRadioPackage ?: return
        val receiverComponent = navRadioHelper.getMediaButtonReceiverComponent(radioPackage)

        if (receiverComponent == null) {
            Log.w(TAG, "Cannot dispatch key event: Media button receiver not found.")
            return
        }

        val mediaIntent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
            component = receiverComponent
            putExtra(Intent.EXTRA_KEY_EVENT, keyEvent)
        }
        sendBroadcast(mediaIntent)
        Log.d(TAG, "Dispatched key event $keyEvent to ${receiverComponent.flattenToString()}")
    }

    private fun registerFinishReceiver() {
        try {
            val filter = IntentFilter(ACTION_FINISH_PROXY)
            registerReceiver(finishProxyReceiver, filter)
            Log.d(TAG, "Finish receiver registered.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register finish receiver", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        try {
            unregisterReceiver(finishProxyReceiver)
            Log.d(TAG, "Finish receiver unregistered.")
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Finish receiver was not registered.")
        }
        Log.d(TAG, "---------- FmMainActivity: onDestroy ----------")
    }

    private fun logIntent(intent: Intent, context: String) {
        val extrasString = intent.extras?.let { bundle ->
            bundle.keySet().joinToString(", ") { key -> "$key=${bundle[key]}" }
        } ?: "null"
        Log.d(TAG, "Intent received in $context - Action: ${intent.action}, Extras: $extrasString")
    }
}
