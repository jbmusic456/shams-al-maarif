package com.shamsalmaarif.reader.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.shamsalmaarif.reader.MainActivity
import com.shamsalmaarif.reader.R
import com.shamsalmaarif.reader.tts.TtsEngine
import com.shamsalmaarif.reader.tts.TtsLanguage
import com.shamsalmaarif.reader.tts.TtsState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PlayerService : LifecycleService() {

    @Inject lateinit var ttsEngine: TtsEngine

    private var playJob: Job? = null
    private val channelId = "shams_player"
    private val notifId = 1001

    companion object {
        const val ACTION_PLAY = "com.shamsalmaarif.PLAY"
        const val ACTION_PAUSE = "com.shamsalmaarif.PAUSE"
        const val ACTION_STOP = "com.shamsalmaarif.STOP"
        const val EXTRA_TEXT = "text"
        const val EXTRA_LANG = "lang"
        const val EXTRA_SPEED = "speed"
        const val EXTRA_OFFSET = "offset"
        const val EXTRA_READ_ID = "readId"
        const val EXTRA_TITLE = "title"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_PLAY -> {
                val text = intent.getStringExtra(EXTRA_TEXT) ?: return START_NOT_STICKY
                val lang = TtsLanguage.fromCode(intent.getStringExtra(EXTRA_LANG) ?: "en")
                val speed = intent.getFloatExtra(EXTRA_SPEED, 1.0f)
                val title = intent.getStringExtra(EXTRA_TITLE) ?: "شمس المعارف"
                startForeground(notifId, buildNotification(title, "Playing..."))
                playJob?.cancel()
                playJob = lifecycleScope.launch {
                    ttsEngine.speak(text, lang, speed)
                    stopSelf()
                }
            }
            ACTION_PAUSE -> ttsEngine.pause()
            ACTION_STOP -> {
                ttsEngine.stop()
                playJob?.cancel()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    override fun onDestroy() {
        ttsEngine.stop()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Playback", NotificationManager.IMPORTANCE_LOW)
            channel.description = "Shams Al Maarif playback controls"
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(title: String, content: String): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, PlayerService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE
        )
        val pauseIntent = PendingIntent.getService(
            this, 2,
            Intent(this, PlayerService::class.java).apply { action = ACTION_PAUSE },
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(openIntent)
            .addAction(android.R.drawable.ic_media_pause, "Pause", pauseIntent)
            .addAction(android.R.drawable.ic_delete, "Stop", stopIntent)
            .setOngoing(true)
            .build()
    }
}
