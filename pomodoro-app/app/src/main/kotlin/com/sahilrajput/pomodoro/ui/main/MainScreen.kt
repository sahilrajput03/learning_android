package com.sahilrajput.pomodoro.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sahilrajput.pomodoro.R
import com.sahilrajput.pomodoro.ui.theme.PomodoroAppTheme

@Composable
fun MainScreen(
  modifier: Modifier = Modifier,
  viewModel: MainScreenViewModel = viewModel(),
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  val context = LocalContext.current
  var musicFolderTarget by rememberSaveable { mutableStateOf(TimerPhase.Focus.name) }
  val notificationPermissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { }
  val audioPickerLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.OpenDocument()
  ) { uri ->
    if (uri != null) {
      try {
        context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
      } catch (_: SecurityException) {
        // Some document providers only grant access for the current app session.
      }
      viewModel.setAlertSound(uri)
    }
  }
  val musicFolderPickerLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.OpenDocumentTree()
  ) { uri ->
    if (uri != null) {
      try {
        context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
      } catch (_: SecurityException) {
        // Some document providers only grant access for the current app session.
      }
      if (musicFolderTarget == TimerPhase.Focus.name) viewModel.setFocusMusicFolder(uri)
      else viewModel.setMusicFolder(uri)
    }
  }
  MainScreenContent(
    state = state,
    onStartPause = {
      if (
        !state.isRunning &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
      ) {
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
      }
      viewModel.startPause()
    },
    onStopAlert = viewModel::stopAlert,
    onReset = viewModel::reset,
    onSelectDuration = viewModel::selectFocusDuration,
    onSelectGradient = viewModel::setGradientStyle,
    onAutoStartChanged = viewModel::setAutoStart,
    onShortBreakMinutesChanged = viewModel::setShortBreakMinutes,
    onAlertSoundEnabledChanged = viewModel::setAlertSoundEnabled,
    onChooseAudio = { audioPickerLauncher.launch(arrayOf("audio/*")) },
    onUseDefaultAudio = { viewModel.setAlertSound(null) },
    onResetSessionData = viewModel::resetSessionData,
    onChooseMusicFolder = { phase ->
      musicFolderTarget = phase.name
      musicFolderPickerLauncher.launch(null)
    },
    onPlayPauseMusic = { phase ->
      if (phase == TimerPhase.Focus) viewModel.playPauseFocusMusic() else viewModel.playPauseBreakMusic()
    },
    onNextMusicTrack = { phase ->
      if (phase == TimerPhase.Focus) viewModel.nextFocusMusicTrack() else viewModel.nextMusicTrack()
    },
    onPreviousMusicTrack = { phase ->
      if (phase == TimerPhase.Focus) viewModel.previousFocusMusicTrack() else viewModel.previousMusicTrack()
    },
    onToggleShuffle = viewModel::toggleShuffle,
    modifier = modifier
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreenContent(
  state: TimerUiState,
  onStartPause: () -> Unit,
  onStopAlert: () -> Unit,
  onReset: () -> Unit,
  onSelectDuration: (Int) -> Unit,
  onSelectGradient: (Int) -> Unit,
  onAutoStartChanged: (Boolean) -> Unit,
  onShortBreakMinutesChanged: (Int) -> Unit,
  onAlertSoundEnabledChanged: (Boolean) -> Unit,
  onChooseAudio: () -> Unit,
  onUseDefaultAudio: () -> Unit,
  onResetSessionData: () -> Unit,
  onChooseMusicFolder: (TimerPhase) -> Unit,
  onPlayPauseMusic: (TimerPhase) -> Unit,
  onNextMusicTrack: (TimerPhase) -> Unit,
  onPreviousMusicTrack: (TimerPhase) -> Unit,
  onToggleShuffle: () -> Unit,
  modifier: Modifier = Modifier
) {
  var showSettings by rememberSaveable { mutableStateOf(false) }
  BackHandler(enabled = showSettings) { showSettings = false }
  Scaffold(
    modifier = modifier,
    topBar = {
      CenterAlignedTopAppBar(
        title = { Text(if (showSettings) "Settings" else stringResource(R.string.app_name)) },
        navigationIcon = {
          if (showSettings) {
            IconButton(
              onClick = { showSettings = false },
              modifier = Modifier.semantics { contentDescription = "Back" }
            ) { Text("←", fontSize = 24.sp) }
          }
        },
        actions = {
          if (!showSettings) {
            IconButton(
              onClick = { showSettings = true },
              modifier = Modifier.semantics { contentDescription = "Settings" }
            ) { Text("⚙", fontSize = 22.sp) }
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.primaryContainer,
          titleContentColor = MaterialTheme.colorScheme.primary
        )
      )
    }
  ) { padding ->
    if (showSettings) {
      SettingsContent(
        state = state,
        onAutoStartChanged = onAutoStartChanged,
        onShortBreakMinutesChanged = onShortBreakMinutesChanged,
        onAlertSoundEnabledChanged = onAlertSoundEnabledChanged,
        onChooseAudio = onChooseAudio,
        onUseDefaultAudio = onUseDefaultAudio,
        onResetSessionData = onResetSessionData,
        modifier = Modifier.fillMaxSize().padding(padding)
      )
    } else {
      TimerContent(
        state = state,
        onStartPause = onStartPause,
        onStopAlert = onStopAlert,
        onReset = onReset,
        onSelectDuration = onSelectDuration,
        onSelectGradient = onSelectGradient,
        onChooseMusicFolder = onChooseMusicFolder,
        onPlayPauseMusic = onPlayPauseMusic,
        onNextMusicTrack = onNextMusicTrack,
        onPreviousMusicTrack = onPreviousMusicTrack,
        onToggleShuffle = onToggleShuffle,
        modifier = Modifier.fillMaxSize().padding(padding)
      )
    }
  }
}

@Composable
private fun TimerContent(
  state: TimerUiState,
  onStartPause: () -> Unit,
  onStopAlert: () -> Unit,
  onReset: () -> Unit,
  onSelectDuration: (Int) -> Unit,
  onSelectGradient: (Int) -> Unit,
  onChooseMusicFolder: (TimerPhase) -> Unit,
  onPlayPauseMusic: (TimerPhase) -> Unit,
  onNextMusicTrack: (TimerPhase) -> Unit,
  onPreviousMusicTrack: (TimerPhase) -> Unit,
  onToggleShuffle: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier.verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 20.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(20.dp)
  ) {
    GradientStyleSelector(state.gradientStyleIndex, onSelectGradient)
    Text(state.phase.label.uppercase(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    Box(
      modifier = Modifier
        .size(250.dp)
        .clip(CircleShape)
        .background(animatedGradientBrush(state.gradientStyleIndex))
        .clickable(role = Role.Button, onClick = onStartPause)
        .semantics { contentDescription = "Timer ${formatTime(state.remainingSeconds)}. ${if (state.isRunning) "Tap to pause" else "Tap to start"}" },
      contentAlignment = Alignment.Center
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(formatTime(state.remainingSeconds), fontSize = 56.sp, fontWeight = FontWeight.Light, color = Color.White)
        Text(if (state.isRunning) "Tap to pause" else "Tap to start", style = MaterialTheme.typography.labelLarge, color = Color.White)
      }
    }
    Text(state.message, style = MaterialTheme.typography.bodyMedium)
    val canReset = state.isRunning || state.phase != TimerPhase.Focus || state.remainingSeconds != state.focusDurationSeconds
    if (canReset) {
      OutlinedButton(onClick = onReset) { Text("Reset") }
    }
    if (state.isAlertPlaying) {
      Button(onClick = onStopAlert) { Text("Stop ringing") }
    }
    SessionMusicControls(
      state = state,
      onChooseFolder = onChooseMusicFolder,
      onPlayPause = onPlayPauseMusic,
      onNext = onNextMusicTrack,
      onPrevious = onPreviousMusicTrack,
      onToggleShuffle = onToggleShuffle
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text("Focus length", style = MaterialTheme.typography.titleSmall)
      Spacer(Modifier.height(8.dp))
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val presets = listOf("45 min" to 45, "25 min" to 25, "1 min" to 1)
        val visiblePresets = if (state.isRunning) {
          presets.filter { (_, minutes) -> minutes * 60 == state.focusDurationSeconds }
        } else {
          presets
        }
        visiblePresets
          .chunked(3)
          .forEach { rowPresets ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              rowPresets.forEach { (label, minutes) ->
                val presetSeconds = minutes * 60
                if (state.focusDurationSeconds == presetSeconds) {
                  Button(onClick = { onSelectDuration(minutes) }, enabled = state.phase == TimerPhase.Focus && !state.isRunning) { Text(label) }
                } else {
                  OutlinedButton(onClick = { onSelectDuration(minutes) }, enabled = state.phase == TimerPhase.Focus && !state.isRunning) { Text(label) }
                }
              }
            }
          }
      }
    }

    Spacer(Modifier.height(4.dp))
    Text("${state.completedFocusSessions} focus sessions today", style = MaterialTheme.typography.titleMedium)
    Text("A long break follows every 4 focus sessions", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
  }
}

@Composable
private fun SessionMusicControls(
  state: TimerUiState,
  onChooseFolder: (TimerPhase) -> Unit,
  onPlayPause: (TimerPhase) -> Unit,
  onNext: (TimerPhase) -> Unit,
  onPrevious: (TimerPhase) -> Unit,
  onToggleShuffle: () -> Unit
) {
  val isFocus = state.phase == TimerPhase.Focus
  val tracks = if (isFocus) state.focusMusicTracks else state.musicTracks
  val folderUri = if (isFocus) state.focusMusicFolderUri else state.musicFolderUri
  val trackIndex = if (isFocus) state.currentFocusMusicTrackIndex else state.currentMusicTrackIndex
  val positionMs = if (isFocus) state.focusMusicPositionMs else state.musicPositionMs
  val durationMs = if (isFocus) state.focusMusicDurationMs else state.musicDurationMs
  val isPlaying = if (isFocus) state.isFocusMusicPlaying else state.isMusicPlaying
  val isLoading = if (isFocus) state.isFocusMusicLoading else state.isMusicLoading
  val isLibraryLoading = if (isFocus) state.isFocusMusicLibraryLoading else state.isMusicLibraryLoading
  Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceVariant) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Text(if (isFocus) "Pomodoro music" else "Break music", style = MaterialTheme.typography.titleSmall)
      when {
        isLibraryLoading -> Text("Scanning music folder…", style = MaterialTheme.typography.bodySmall)
        tracks.isNotEmpty() -> {
          val safeTrackIndex = trackIndex.coerceIn(0, tracks.lastIndex)
          Text("${safeTrackIndex + 1} / ${tracks.size} · ${tracks[safeTrackIndex].name}", style = MaterialTheme.typography.bodySmall)
        }
        folderUri == null -> Text("Choose a local folder with music files", style = MaterialTheme.typography.bodySmall)
        else -> Text("No supported music files found in this folder", style = MaterialTheme.typography.bodySmall)
      }
      if (durationMs > 0) {
        Text(
          "${formatTime(positionMs / 1000)} / ${formatTime(durationMs / 1000)}",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(onClick = { onChooseFolder(state.phase) }) {
          Text(if (folderUri == null) "Choose music folder" else "Change folder")
        }
        OutlinedButton(
          onClick = onToggleShuffle,
          enabled = tracks.isNotEmpty() && !isLibraryLoading
        ) {
          Text(if (state.shuffleEnabled) "Shuffle: On" else "Shuffle: Off")
        }
      }
      if (tracks.isNotEmpty()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = { onPrevious(state.phase) },
            enabled = !isLoading,
            modifier = Modifier.semantics { contentDescription = "Previous track" }
          ) { Text("⏮", fontSize = 24.sp) }
          IconButton(
            onClick = { onPlayPause(state.phase) },
            enabled = !isLoading && !state.isAlertPlaying,
            modifier = Modifier.semantics { contentDescription = if (isPlaying) "Pause music" else "Play music" }
          ) {
            if (isPlaying) {
              Text("Ⅱ", fontSize = 26.sp, color = MaterialTheme.colorScheme.primary)
            } else {
              Text("▶", fontSize = 28.sp)
            }
          }
          IconButton(
            onClick = { onNext(state.phase) },
            enabled = !isLoading,
            modifier = Modifier.semantics { contentDescription = "Next track" }
          ) { Text("⏭", fontSize = 24.sp) }
        }
      }
    }
  }
}

@Composable
private fun SettingsContent(
  state: TimerUiState,
  onAutoStartChanged: (Boolean) -> Unit,
  onShortBreakMinutesChanged: (Int) -> Unit,
  onAlertSoundEnabledChanged: (Boolean) -> Unit,
  onChooseAudio: () -> Unit,
  onUseDefaultAudio: () -> Unit,
  onResetSessionData: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val versionName = try {
    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "Unknown"
  } catch (_: Exception) {
    "Unknown"
  }
  Column(
    modifier = modifier.verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 20.dp),
    verticalArrangement = Arrangement.spacedBy(20.dp)
  ) {
    PreferenceSwitch(
      title = "Auto-start next session",
      description = "Start the next focus or break automatically",
      checked = state.autoStartNext,
      onCheckedChange = onAutoStartChanged
    )
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceVariant) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text("Short break duration", style = MaterialTheme.typography.titleSmall)
          Text("${state.shortBreakMinutes} min", style = MaterialTheme.typography.bodySmall)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          OutlinedButton(
            onClick = { onShortBreakMinutesChanged(state.shortBreakMinutes - 1) },
            enabled = state.shortBreakMinutes > 1
          ) { Text("−") }
          OutlinedButton(
            onClick = { onShortBreakMinutesChanged(state.shortBreakMinutes + 1) },
            enabled = state.shortBreakMinutes < 30
          ) { Text("+") }
        }
      }
    }
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceVariant) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
          Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text("Alert audio", style = MaterialTheme.typography.titleSmall)
            Text("Play a sound when a timer ends", style = MaterialTheme.typography.bodySmall)
          }
          Switch(checked = state.alertSoundEnabled, onCheckedChange = onAlertSoundEnabledChanged)
        }
        Text(state.alertSoundName, style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedButton(onClick = onChooseAudio) { Text("Choose audio") }
          if (state.alertSoundUri != null) {
            OutlinedButton(onClick = onUseDefaultAudio) { Text("Use default") }
          }
        }
      }
    }
    OutlinedButton(onClick = onResetSessionData) { Text("Reset session data") }
    Text(
      text = "Version $versionName",
      modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
      textAlign = TextAlign.Center,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}

@Composable
private fun GradientStyleSelector(selectedStyle: Int, onSelectStyle: (Int) -> Unit) {
  val styleNames = listOf("Sunset", "Ocean", "Aurora")
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text("Color", style = MaterialTheme.typography.labelMedium)
    styleNames.forEachIndexed { index, name ->
      val selected = index == selectedStyle
      Row(
        modifier = Modifier
          .toggleable(
            value = selected,
            role = Role.RadioButton,
            onValueChange = { onSelectStyle(index) }
          )
          .semantics { contentDescription = "$name animated timer color" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        Box(
          modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(animatedGradientBrush(index))
            .border(
              width = if (selected) 2.dp else 1.dp,
              color = if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.8f),
              shape = CircleShape
            )
        )
        Text(name, style = MaterialTheme.typography.labelSmall)
      }
    }
  }
}

@Composable
private fun animatedGradientBrush(styleIndex: Int): Brush {
  val colors = when (styleIndex) {
    1 -> listOf(Color(0xFF0369A1), Color(0xFF0F766E), Color(0xFF1D4ED8))
    2 -> listOf(Color(0xFF047857), Color(0xFF0891B2), Color(0xFF4338CA))
    else -> listOf(Color(0xFFBE123C), Color(0xFF9D174D), Color(0xFF7E22CE))
  }
  val transition = rememberInfiniteTransition(label = "timer-gradient-$styleIndex")
  val animation = infiniteRepeatable<Color>(
    animation = tween(durationMillis = 2800, easing = LinearEasing),
    repeatMode = RepeatMode.Reverse
  )
  val first by transition.animateColor(colors[0], colors[1], animation, label = "gradient-first")
  val second by transition.animateColor(colors[1], colors[2], animation, label = "gradient-second")
  val third by transition.animateColor(colors[2], colors[0], animation, label = "gradient-third")
  return Brush.linearGradient(
    colors = listOf(first, second, third, first),
    start = Offset.Zero,
    end = Offset(500f, 500f)
  )
}

private fun formatTime(seconds: Int): String = "%02d:%02d".format(seconds / 60, seconds % 60)

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun MainScreenPreview() {
  PomodoroAppTheme {
    MainScreenContent(TimerUiState(), {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
  }
}

@Composable
private fun PreferenceSwitch(
  title: String,
  description: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit
) {
  Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceVariant) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Text(description, style = MaterialTheme.typography.bodySmall)
      }
      Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
  }
}
