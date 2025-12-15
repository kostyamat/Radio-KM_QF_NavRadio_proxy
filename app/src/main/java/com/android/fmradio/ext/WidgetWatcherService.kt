package com.android.fmradio.ext

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.android.fmradio.FmMainActivity

class WidgetWatcherService : Service() {

    companion object {
        private const val TAG = "WidgetWatcherService"
        private const val NOTIFICATION_CHANNEL_ID = "WidgetWatcherChannel"
        private const val NOTIFICATION_ID = 1
        private const val NAVRADIO_PACKAGE = "com.navimods.radio"
        private const val LAST_AUDIO_SRC_PROP = "sys.qf.last_audio_src"
    }

    private val homeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val reason = intent?.getStringExtra("reason")
            if (reason == "homekey") {
                Log.d(TAG, "Home key pressed.")
                checkAndFixWidget()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service creating.")
        startForegroundWithNotification()

        val filter = IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
        registerReceiver(homeReceiver, filter)
        Log.d(TAG, "Home key receiver registered.")
    }

    private fun checkAndFixWidget() {
        val lastAudioSource = SystemPropertiesReader.get(LAST_AUDIO_SRC_PROP)
        Log.d(TAG, "Checking audio source. Current source: '$lastAudioSource'")

        if (lastAudioSource.contains(NAVRADIO_PACKAGE)) {
            Log.i(TAG, "NavRadio is the audio source. Triggering hack via transparent activity.")

            val intent = Intent(FmMainActivity.ACTION_PERFORM_HACK).apply {
                component = ComponentName("com.android.fmradio.ext", "com.android.fmradio.FmMainActivity")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        }
    }

    private fun startForegroundWithNotification() {
        val channelId = NOTIFICATION_CHANNEL_ID
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_MIN
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started.")
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(homeReceiver)
        Log.d(TAG, "Service destroyed.")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
