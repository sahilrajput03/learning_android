package com.sahilrajput.pomodoro.ui.main

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.sahilrajput.pomodoro.MainActivity

internal class SessionAlerts(
  private val context: Context,
  private val onPlaybackChanged: (Boolean) -> Unit
) {
  private val toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 75)
  private val handler = Handler(Looper.getMainLooper())
  private var toneStopper: Runnable? = null
  private var mediaPlayer: MediaPlayer? = null

  fun playChime(soundUri: Uri?) {
    stop()
    if (soundUri == null) {
      toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, DEFAULT_TONE_DURATION_MS)
      onPlaybackChanged(true)
      val stopper = Runnable {
        toneGenerator.stopTone()
        toneStopper = null
        onPlaybackChanged(false)
      }
      toneStopper = stopper
      handler.postDelayed(stopper, DEFAULT_TONE_DURATION_MS.toLong())
      return
    }
    try {
      mediaPlayer?.release()
      val player = MediaPlayer()
      mediaPlayer = player
      player.setAudioAttributes(
        AudioAttributes.Builder()
          .setUsage(AudioAttributes.USAGE_NOTIFICATION)
          .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
          .build()
      )
      player.isLooping = true
      player.setDataSource(context, soundUri)
      player.setOnPreparedListener {
        it.start()
        onPlaybackChanged(true)
      }
      player.setOnCompletionListener {
        if (mediaPlayer === it) mediaPlayer = null
        it.release()
        onPlaybackChanged(false)
      }
      player.setOnErrorListener { failedPlayer, _, _ ->
        if (mediaPlayer === failedPlayer) mediaPlayer = null
        failedPlayer.release()
        playChime(null)
        true
      }
      player.prepareAsync()
    } catch (_: Exception) {
      mediaPlayer?.release()
      mediaPlayer = null
      playChime(null)
    }
  }

  fun postNotification(phase: TimerPhase): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
      ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) return false
    if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false

    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channelId = CHANNEL_ID
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        channelId,
        "Pomodoro session alerts",
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        description = "Show when a focus session or break finishes"
        setSound(null, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build())
      }
      manager.createNotificationChannel(channel)
      val savedChannel = manager.getNotificationChannel(channelId)
      if (savedChannel.importance == NotificationManager.IMPORTANCE_NONE) return false
    }

    val pendingIntent = PendingIntent.getActivity(
      context,
      0,
      android.content.Intent(context, MainActivity::class.java),
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val notificationBuilder = NotificationCompat.Builder(context, channelId)
      .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
      .setContentTitle("Pomodoro session complete")
      .setContentText(if (phase == TimerPhase.Focus) "Focus session finished. Time for a break." else "Break finished. Ready to focus again?")
      .setContentIntent(pendingIntent)
      .setAutoCancel(true)
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setCategory(NotificationCompat.CATEGORY_ALARM)
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
      notificationBuilder.setVibrate(longArrayOf(0, 250, 150, 250))
    }
    val notification = notificationBuilder.build()
    return try {
      NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
      true
    } catch (_: SecurityException) {
      false
    }
  }

  fun release() {
    stop()
    handler.removeCallbacksAndMessages(null)
    toneGenerator.release()
  }

  fun stop() {
    toneStopper?.let(handler::removeCallbacks)
    toneStopper = null
    toneGenerator.stopTone()
    mediaPlayer?.release()
    mediaPlayer = null
    onPlaybackChanged(false)
  }

  private companion object {
    const val CHANNEL_ID = "pomodoro_session_alerts_heads_up_v1"
    const val NOTIFICATION_ID = 1001
    const val DEFAULT_TONE_DURATION_MS = 4000
  }
}
