package com.sahilrajput.pomodoro.ui.main

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri

data class MusicTrack(val name: String, val uri: String)

internal class BreakMusicPlayer(
  private val context: Context,
  private val onPlayingChanged: (Boolean) -> Unit,
  private val onLoadingChanged: (Boolean) -> Unit,
  private val onTrackCompleted: () -> Unit
) {
  private var player: MediaPlayer? = null
  private var isPrepared = false
  private var isPaused = false
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
      } else {
        prepared.start()
        onPlayingChanged(true)
      }
    }
    newPlayer.setOnCompletionListener { completed ->
      if (player === completed) {
        player = null
        isPrepared = false
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
      if (current.isPlaying) current.pause()
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
    } catch (_: IllegalStateException) {
      stop()
    }
  }

  fun stop() {
    val current = player
    player = null
    isPrepared = false
    isPaused = false
    if (current != null) {
      try {
        if (current.isPlaying) current.stop()
      } catch (_: IllegalStateException) {
        // The player may still be preparing.
      } finally {
        current.release()
      }
    }
    onLoadingChanged(false)
    onPlayingChanged(false)
  }
}
