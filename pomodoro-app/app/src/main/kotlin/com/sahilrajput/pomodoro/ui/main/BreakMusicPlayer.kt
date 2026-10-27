package com.sahilrajput.pomodoro.ui.main

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper

data class MusicTrack(val name: String, val uri: String)

internal class BreakMusicPlayer(
  private val context: Context,
  private val onPlayingChanged: (Boolean) -> Unit,
  private val onLoadingChanged: (Boolean) -> Unit,
  private val onProgressChanged: (positionMs: Int, durationMs: Int) -> Unit,
  private val onTrackCompleted: () -> Unit
) {
  private val handler = Handler(Looper.getMainLooper())
  private var player: MediaPlayer? = null
  private var isPrepared = false
  private var isPaused = false
  private var progressTicker: Runnable? = null
  val hasCurrentTrack: Boolean get() = player != null

  fun play(uri: Uri) {
    stop()
    val newPlayer = MediaPlayer()
    player = newPlayer
    isPrepared = false
    isPaused = false
    newPlayer.setAudioAttributes(
      AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()
    )
    newPlayer.setOnPreparedListener { prepared ->
      isPrepared = true
      onLoadingChanged(false)
      if (isPaused) {
        onPlayingChanged(false)
        publishProgress(prepared)
      } else {
        prepared.start()
        onPlayingChanged(true)
        startProgressUpdates(prepared)
      }
    }
    newPlayer.setOnCompletionListener { completed ->
      if (player === completed) {
        player = null
        isPrepared = false
        stopProgressUpdates()
        onProgressChanged(0, 0)
        completed.release()
        onPlayingChanged(false)
        onTrackCompleted()
      }
    }
    newPlayer.setOnErrorListener { failed, _, _ ->
      if (player === failed) player = null
      isPrepared = false
      failed.release()
      onLoadingChanged(false)
      onPlayingChanged(false)
      true
    }
    try {
      newPlayer.setDataSource(context, uri)
      onLoadingChanged(true)
      newPlayer.prepareAsync()
    } catch (_: Exception) {
      if (player === newPlayer) player = null
      isPrepared = false
      newPlayer.release()
      onLoadingChanged(false)
      onPlayingChanged(false)
    }
  }

  fun pause() {
    val current = player ?: return
    isPaused = true
    if (!isPrepared) return
    try {
      if (current.isPlaying) {
        current.pause()
        publishProgress(current)
        stopProgressUpdates()
      }
      onPlayingChanged(false)
    } catch (_: IllegalStateException) {
      stop()
    }
  }

  fun resume() {
    val current = player ?: return
    isPaused = false
    if (!isPrepared) return
    try {
      current.start()
      onPlayingChanged(true)
      publishProgress(current)
      startProgressUpdates(current)
    } catch (_: IllegalStateException) {
      stop()
    }
  }

  fun stop() {
    val current = player
    player = null
    isPrepared = false
    isPaused = false
    stopProgressUpdates()
    if (current != null) {
      try {
        if (current.isPlaying) current.stop()
      } catch (_: IllegalStateException) {
        // The player may still be preparing.
      } finally {
        current.release()
      }
    }
    onProgressChanged(0, 0)
    onLoadingChanged(false)
    onPlayingChanged(false)
  }

  private fun startProgressUpdates(current: MediaPlayer) {
    stopProgressUpdates()
    val ticker = object : Runnable {
      override fun run() {
        if (player === current && isPrepared && current.isPlaying) {
          publishProgress(current)
          handler.postDelayed(this, PROGRESS_UPDATE_INTERVAL_MS)
        }
      }
    }
    progressTicker = ticker
    handler.post(ticker)
  }

  private fun stopProgressUpdates() {
    progressTicker?.let(handler::removeCallbacks)
    progressTicker = null
  }

  private fun publishProgress(current: MediaPlayer) {
    try {
      onProgressChanged(current.currentPosition, current.duration)
    } catch (_: IllegalStateException) {
      // The player may be releasing after a track change.
    }
  }

  private companion object {
    const val PROGRESS_UPDATE_INTERVAL_MS = 1000L
  }
}
