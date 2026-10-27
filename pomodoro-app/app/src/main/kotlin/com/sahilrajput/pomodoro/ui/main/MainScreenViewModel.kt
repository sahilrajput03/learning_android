package com.sahilrajput.pomodoro.ui.main

import android.app.Application
import android.content.Context
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.ArrayDeque
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class TimerPhase(val label: String) {
  Focus("Focus"),
  ShortBreak("Short break"),
  LongBreak("Long break")
}

data class TimerUiState(
  val phase: TimerPhase = TimerPhase.Focus,
  val remainingSeconds: Int = 25 * 60,
  val focusDurationSeconds: Int = 25 * 60,
  val shortBreakMinutes: Int = 5,
  val completedFocusSessions: Int = 0,
  val autoStartNext: Boolean = false,
  val gradientStyleIndex: Int = 0,
  val alertSoundEnabled: Boolean = true,
  val alertSoundUri: String? = null,
  val alertSoundName: String = "Default chime",
  val isAlertPlaying: Boolean = false,
  val musicFolderUri: String? = null,
  val musicTracks: List<MusicTrack> = emptyList(),
  val currentMusicTrackIndex: Int = 0,
  val isMusicPlaying: Boolean = false,
  val isMusicLoading: Boolean = false,
  val isMusicLibraryLoading: Boolean = false,
  val shuffleEnabled: Boolean = true,
  val focusMusicFolderUri: String? = null,
  val focusMusicTracks: List<MusicTrack> = emptyList(),
  val currentFocusMusicTrackIndex: Int = 0,
  val isFocusMusicPlaying: Boolean = false,
  val isFocusMusicLoading: Boolean = false,
  val isFocusMusicLibraryLoading: Boolean = false,
  val isRunning: Boolean = false,
  val message: String = "Ready when you are"
)

class MainScreenViewModel(application: Application) : AndroidViewModel(application) {
  private val preferences = application.getSharedPreferences("pomodoro_settings", Application.MODE_PRIVATE)
  private val savedAlertSound = preferences.getString("alert_sound_uri", null)
  private val savedMusicFolder = preferences.getString("music_folder_uri", null)
  private val savedFocusMusicFolder = preferences.getString("focus_music_folder_uri", null)
  private val _uiState = MutableStateFlow(
    TimerUiState(
      autoStartNext = preferences.getBoolean("auto_start_next", false),
      shortBreakMinutes = preferences.getInt("short_break_minutes", 5).coerceIn(1, 30),
      gradientStyleIndex = preferences.getInt("gradient_style_index", 0).coerceIn(0, 2),
      alertSoundEnabled = preferences.getBoolean("alert_sound_enabled", true),
      shuffleEnabled = preferences.getBoolean("music_shuffle_enabled", true),
      alertSoundUri = savedAlertSound,
      alertSoundName = savedAlertSound?.let { displayName(application, Uri.parse(it)) } ?: "Default chime",
      musicFolderUri = savedMusicFolder,
      isMusicLibraryLoading = savedMusicFolder != null,
      focusMusicFolderUri = savedFocusMusicFolder,
      isFocusMusicLibraryLoading = savedFocusMusicFolder != null
    )
  )
  val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()
  private var isViewModelCleared = false
  private var breakMusicPending = false
  private var breakMusicHasPlayed = false
  private var focusMusicPending = false
  private val alerts = SessionAlerts(application) { playing ->
    _uiState.update { it.copy(isAlertPlaying = playing) }
    if (!playing && !isViewModelCleared) {
      val state = _uiState.value
      if (focusMusicPending && state.phase == TimerPhase.Focus && state.isRunning) {
        focusMusicPending = false
        startFocusMusic()
      } else if (breakMusicPending && state.phase != TimerPhase.Focus && state.musicTracks.isNotEmpty()) {
        startBreakMusic()
      }
    }
  }
  private val breakMusicPlayer = BreakMusicPlayer(
    application,
    onPlayingChanged = { playing -> _uiState.update { it.copy(isMusicPlaying = playing) } },
    onLoadingChanged = { loading -> _uiState.update { it.copy(isMusicLoading = loading) } },
    onTrackCompleted = { advanceMusicTrack(autoplay = true) }
  )
  private val focusMusicPlayer = BreakMusicPlayer(
    application,
    onPlayingChanged = { playing -> _uiState.update { it.copy(isFocusMusicPlaying = playing) } },
    onLoadingChanged = { loading -> _uiState.update { it.copy(isFocusMusicLoading = loading) } },
    onTrackCompleted = { advanceFocusMusicTrack(autoplay = true) }
  )
  private var timerJob: Job? = null
  private var sessionNumber = 0
  private var countDate = todayKey()

  init {
    savedMusicFolder?.let { loadMusicFolder(it, focus = false) }
    savedFocusMusicFolder?.let { loadMusicFolder(it, focus = true) }
  }

  fun startPause() {
    if (_uiState.value.isRunning) pause() else start()
  }

  fun start() {
    if (_uiState.value.isRunning) return
    resetDailyCountIfNeeded()
    _uiState.update {
      it.copy(
        isRunning = true,
        message = if (it.phase == TimerPhase.Focus) "Stay focused" else "Enjoy your break"
      )
    }
    timerJob = viewModelScope.launch {
      while (isActive && _uiState.value.isRunning) {
        delay(1000)
        val current = _uiState.value
        if (!current.isRunning) break
        if (current.remainingSeconds <= 1) finishPhase() else {
          _uiState.update { it.copy(remainingSeconds = it.remainingSeconds - 1) }
        }
      }
    }
    if (_uiState.value.phase == TimerPhase.Focus) startFocusMusic()
  }

  fun pause() {
    timerJob?.cancel()
    timerJob = null
    _uiState.update { it.copy(isRunning = false, message = "Paused") }
    if (_uiState.value.phase == TimerPhase.Focus) {
      focusMusicPending = false
      focusMusicPlayer.pause()
    }
  }

  fun reset() {
    timerJob?.cancel()
    timerJob = null
    breakMusicPending = false
    focusMusicPending = false
    val state = _uiState.value
    _uiState.value = state.copy(
      phase = TimerPhase.Focus,
      remainingSeconds = state.focusDurationSeconds,
      isRunning = false,
      message = "Focus timer reset"
    )
    breakMusicPlayer.pause()
    focusMusicPlayer.pause()
  }

  fun selectFocusDuration(minutes: Int) {
    if (minutes !in listOf(1, 25, 45)) return
    val durationSeconds = minutes * 60
    if (_uiState.value.phase != TimerPhase.Focus) return
    timerJob?.cancel()
    timerJob = null
    _uiState.update {
      it.copy(focusDurationSeconds = durationSeconds, remainingSeconds = durationSeconds, isRunning = false, message = "Ready when you are")
    }
  }

  fun setAutoStart(enabled: Boolean) {
    preferences.edit().putBoolean("auto_start_next", enabled).apply()
    _uiState.update { it.copy(autoStartNext = enabled) }
  }

  fun setShortBreakMinutes(minutes: Int) {
    if (minutes !in 1..30) return
    preferences.edit().putInt("short_break_minutes", minutes).apply()
    _uiState.update { state ->
      state.copy(
        shortBreakMinutes = minutes,
        remainingSeconds = if (state.phase == TimerPhase.ShortBreak && !state.isRunning) minutes * 60 else state.remainingSeconds
      )
    }
  }

  fun setGradientStyle(index: Int) {
    if (index !in 0..2) return
    preferences.edit().putInt("gradient_style_index", index).apply()
    _uiState.update { it.copy(gradientStyleIndex = index) }
  }

  fun setAlertSoundEnabled(enabled: Boolean) {
    preferences.edit().putBoolean("alert_sound_enabled", enabled).apply()
    _uiState.update { it.copy(alertSoundEnabled = enabled) }
    if (!enabled) alerts.stop()
  }

  fun resetSessionData() {
    sessionNumber = 0
    countDate = todayKey()
    _uiState.update { it.copy(completedFocusSessions = 0) }
  }

  fun setMusicFolder(uri: Uri) {
    breakMusicPending = false
    breakMusicPlayer.stop()
    breakMusicHasPlayed = false
    breakMusicPending = _uiState.value.phase != TimerPhase.Focus
    val uriString = uri.toString()
    preferences.edit().putString("music_folder_uri", uriString).apply()
    _uiState.update {
      it.copy(
        musicFolderUri = uriString,
        musicTracks = emptyList(),
        currentMusicTrackIndex = 0,
        isMusicLibraryLoading = true
      )
    }
    loadMusicFolder(uriString, focus = false)
  }

  fun setFocusMusicFolder(uri: Uri) {
    focusMusicPending = false
    focusMusicPlayer.stop()
    val uriString = uri.toString()
    preferences.edit().putString("focus_music_folder_uri", uriString).apply()
    _uiState.update {
      it.copy(
        focusMusicFolderUri = uriString,
        focusMusicTracks = emptyList(),
        currentFocusMusicTrackIndex = 0,
        isFocusMusicLibraryLoading = true
      )
    }
    loadMusicFolder(uriString, focus = true)
  }

  fun playPauseFocusMusic() {
    val state = _uiState.value
    if (state.phase != TimerPhase.Focus || state.focusMusicTracks.isEmpty() || state.isFocusMusicLoading) return
    if (state.isFocusMusicPlaying) focusMusicPlayer.pause()
    else if (focusMusicPlayer.hasCurrentTrack) focusMusicPlayer.resume()
    else playCurrentFocusTrack()
  }

  fun nextFocusMusicTrack() = advanceFocusMusicTrack(direction = 1, autoplay = _uiState.value.isFocusMusicPlaying || _uiState.value.isFocusMusicLoading)

  fun previousFocusMusicTrack() = advanceFocusMusicTrack(direction = -1, autoplay = _uiState.value.isFocusMusicPlaying || _uiState.value.isFocusMusicLoading)

  fun playPauseBreakMusic() {
    val state = _uiState.value
    if (state.phase == TimerPhase.Focus || state.musicTracks.isEmpty() || state.isMusicLoading) return
    if (state.isMusicPlaying) {
      breakMusicPlayer.pause()
    } else if (breakMusicPlayer.hasCurrentTrack) {
      breakMusicPlayer.resume()
    } else {
      breakMusicPending = false
      playCurrentTrack()
    }
  }

  fun nextMusicTrack() = advanceMusicTrack(autoplay = _uiState.value.isMusicPlaying || _uiState.value.isMusicLoading)

  fun previousMusicTrack() = advanceMusicTrack(direction = -1, autoplay = _uiState.value.isMusicPlaying || _uiState.value.isMusicLoading)

  fun toggleShuffle() {
    val enabled = !_uiState.value.shuffleEnabled
    preferences.edit().putBoolean("music_shuffle_enabled", enabled).apply()
    _uiState.update { it.copy(shuffleEnabled = enabled) }
  }

  fun setAlertSound(uri: Uri?) {
    val soundUri = uri?.toString()
    if (soundUri == null) preferences.edit().remove("alert_sound_uri").apply()
    else preferences.edit().putString("alert_sound_uri", soundUri).apply()
    _uiState.update {
      it.copy(
        alertSoundUri = soundUri,
        alertSoundName = uri?.let { selected -> displayName(getApplication(), selected) } ?: "Default chime"
      )
    }
  }

  fun stopAlert() {
    alerts.stop()
  }

  private fun finishPhase() {
    val current = _uiState.value
    val nextPhase: TimerPhase
    val completed: Int
    val nextMessage: String
    if (current.phase == TimerPhase.Focus) {
      val priorCount = resetDailyCountIfNeeded()
      sessionNumber += 1
      completed = priorCount + 1
      nextPhase = if (sessionNumber % 4 == 0) TimerPhase.LongBreak else TimerPhase.ShortBreak
      nextMessage = "Focus session complete. Time for a ${nextPhase.label.lowercase(Locale.getDefault())}."
    } else {
      completed = current.completedFocusSessions
      nextPhase = TimerPhase.Focus
      nextMessage = "Break complete. Ready for another focus session?"
    }
    alertForCompletion(current)
    timerJob?.cancel()
    timerJob = null
    val autoStart = current.autoStartNext
    _uiState.value = current.copy(
      phase = nextPhase,
      remainingSeconds = durationFor(nextPhase, current.focusDurationSeconds, current.shortBreakMinutes),
      completedFocusSessions = completed,
      isRunning = false,
      message = nextMessage
    )
    if (nextPhase == TimerPhase.Focus) {
      breakMusicPending = false
      breakMusicPlayer.pause()
    } else {
      focusMusicPending = false
      focusMusicPlayer.pause()
      breakMusicPending = true
      startBreakMusic()
    }
    if (autoStart) start()
  }

  private fun playCurrentTrack() {
    val state = _uiState.value
    val track = state.musicTracks.getOrNull(state.currentMusicTrackIndex) ?: return
    breakMusicPlayer.play(Uri.parse(track.uri))
  }

  private fun playCurrentFocusTrack() {
    val state = _uiState.value
    val track = state.focusMusicTracks.getOrNull(state.currentFocusMusicTrackIndex) ?: return
    focusMusicPlayer.play(Uri.parse(track.uri))
  }

  private fun startFocusMusic() {
    val state = _uiState.value
    if (state.phase != TimerPhase.Focus || state.focusMusicTracks.isEmpty() || state.isFocusMusicLibraryLoading) return
    if (state.isAlertPlaying) {
      focusMusicPending = true
      return
    }
    focusMusicPending = false
    if (focusMusicPlayer.hasCurrentTrack) {
      if (!state.isFocusMusicPlaying) focusMusicPlayer.resume()
      return
    }
    val index = if (state.shuffleEnabled && state.focusMusicTracks.size > 1) {
      var randomIndex = state.currentFocusMusicTrackIndex
      while (randomIndex == state.currentFocusMusicTrackIndex) randomIndex = state.focusMusicTracks.indices.random()
      randomIndex
    } else state.currentFocusMusicTrackIndex
    _uiState.update { it.copy(currentFocusMusicTrackIndex = index) }
    playCurrentFocusTrack()
  }

  private fun startBreakMusic() {
    val state = _uiState.value
    if (state.phase == TimerPhase.Focus || state.isAlertPlaying || state.musicTracks.isEmpty() || state.isMusicLibraryLoading) return
    breakMusicPending = false
    if (breakMusicPlayer.hasCurrentTrack) {
      if (!state.isMusicPlaying) breakMusicPlayer.resume()
      breakMusicHasPlayed = true
      return
    }
    val trackIndex = if (!breakMusicHasPlayed && state.shuffleEnabled && state.musicTracks.size > 1) {
      var randomIndex = state.currentMusicTrackIndex
      while (randomIndex == state.currentMusicTrackIndex) {
        randomIndex = state.musicTracks.indices.random()
      }
      randomIndex
    } else {
      state.currentMusicTrackIndex
    }
    _uiState.update { it.copy(currentMusicTrackIndex = trackIndex) }
    breakMusicHasPlayed = true
    playCurrentTrack()
  }

  private fun advanceMusicTrack(direction: Int = 1, autoplay: Boolean) {
    val state = _uiState.value
    if (state.musicTracks.isEmpty()) return
    val nextIndex = if (state.shuffleEnabled && state.musicTracks.size > 1) {
      var randomIndex = state.currentMusicTrackIndex
      while (randomIndex == state.currentMusicTrackIndex) {
        randomIndex = state.musicTracks.indices.random()
      }
      randomIndex
    } else {
      (state.currentMusicTrackIndex + direction + state.musicTracks.size) % state.musicTracks.size
    }
    breakMusicPlayer.stop()
    _uiState.update { it.copy(currentMusicTrackIndex = nextIndex) }
    breakMusicHasPlayed = true
    if (autoplay) playCurrentTrack()
  }

  private fun advanceFocusMusicTrack(direction: Int = 1, autoplay: Boolean) {
    val state = _uiState.value
    if (state.focusMusicTracks.isEmpty()) return
    val nextIndex = if (state.shuffleEnabled && state.focusMusicTracks.size > 1) {
      var randomIndex = state.currentFocusMusicTrackIndex
      while (randomIndex == state.currentFocusMusicTrackIndex) randomIndex = state.focusMusicTracks.indices.random()
      randomIndex
    } else {
      (state.currentFocusMusicTrackIndex + direction + state.focusMusicTracks.size) % state.focusMusicTracks.size
    }
    focusMusicPlayer.stop()
    _uiState.update { it.copy(currentFocusMusicTrackIndex = nextIndex) }
    if (autoplay) playCurrentFocusTrack()
  }

  private fun loadMusicFolder(uriString: String, focus: Boolean) {
    viewModelScope.launch(Dispatchers.IO) {
      val tracks = findAudioTracks(getApplication(), Uri.parse(uriString))
      _uiState.update { state ->
        if (focus && state.focusMusicFolderUri == uriString) {
          state.copy(focusMusicTracks = tracks, currentFocusMusicTrackIndex = 0, isFocusMusicLibraryLoading = false)
        } else if (!focus && state.musicFolderUri == uriString) {
          state.copy(musicTracks = tracks, currentMusicTrackIndex = 0, isMusicLibraryLoading = false)
        } else state
      }
      if (tracks.isNotEmpty()) {
        withContext(Dispatchers.Main.immediate) {
          val state = _uiState.value
          if (focus && state.focusMusicFolderUri == uriString && state.phase == TimerPhase.Focus && state.isRunning) {
            startFocusMusic()
          } else if (!focus && state.musicFolderUri == uriString && state.phase != TimerPhase.Focus) {
            breakMusicPending = true
            startBreakMusic()
          }
        }
      }
    }
  }

  private fun resetDailyCountIfNeeded(): Int {
    val today = todayKey()
    if (today != countDate) {
      countDate = today
      sessionNumber = 0
      _uiState.update { it.copy(completedFocusSessions = 0) }
      return 0
    }
    return _uiState.value.completedFocusSessions
  }

  private fun todayKey(): String = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())

  private fun alertForCompletion(state: TimerUiState) {
    if (state.alertSoundEnabled) alerts.playChime(state.alertSoundUri?.let(Uri::parse))
    alerts.postNotification(state.phase)
  }

  private fun displayName(application: Application, uri: Uri): String = try {
    application.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
      ?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
      } ?: "Selected audio"
  } catch (_: Exception) {
    "Selected audio"
  }

  private fun findAudioTracks(application: Application, treeUri: Uri): List<MusicTrack> = try {
    val resolver = application.contentResolver
    val folders = ArrayDeque<String>().apply { add(DocumentsContract.getTreeDocumentId(treeUri)) }
    val tracks = mutableListOf<MusicTrack>()
    val projection = arrayOf(
      DocumentsContract.Document.COLUMN_DOCUMENT_ID,
      DocumentsContract.Document.COLUMN_DISPLAY_NAME,
      DocumentsContract.Document.COLUMN_MIME_TYPE
    )
    val supportedExtensions = setOf("mp3", "mp4", "m4a", "aac", "wav", "ogg", "oga", "flac", "opus", "3gp")
    while (folders.isNotEmpty()) {
      val parentId = folders.removeFirst()
      val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentId)
      resolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
        val nameColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
        val mimeColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
        while (cursor.moveToNext()) {
          val documentId = cursor.getString(idColumn)
          val name = cursor.getString(nameColumn) ?: "Audio track"
          val mimeType = cursor.getString(mimeColumn).orEmpty()
          if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
            folders.add(documentId)
          } else if (mimeType.startsWith("audio/") || name.substringAfterLast('.', "").lowercase(Locale.ROOT) in supportedExtensions) {
            tracks.add(MusicTrack(name, DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId).toString()))
          }
        }
      }
    }
    tracks.sortedBy { it.name.lowercase(Locale.ROOT) }
  } catch (_: Exception) {
    emptyList()
  }

  override fun onCleared() {
    isViewModelCleared = true
    breakMusicPending = false
    focusMusicPending = false
    timerJob?.cancel()
    breakMusicPlayer.stop()
    focusMusicPlayer.stop()
    alerts.release()
    super.onCleared()
  }

  private fun durationFor(phase: TimerPhase, focusDurationSeconds: Int, shortBreakMinutes: Int): Int = when (phase) {
    TimerPhase.Focus -> focusDurationSeconds
    TimerPhase.ShortBreak -> shortBreakMinutes * 60
    TimerPhase.LongBreak -> 15 * 60
  }
}
