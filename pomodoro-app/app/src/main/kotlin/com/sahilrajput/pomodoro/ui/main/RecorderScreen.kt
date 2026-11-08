package com.sahilrajput.pomodoro.ui.main

import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun RecorderScreen(
  storageFolderUri: String?,
  onStartRecording: () -> Unit,
  onStopRecording: () -> Unit,
  onPauseRecording: () -> Unit,
  onResumeRecording: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: RecordingViewModel = viewModel()
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  LaunchedEffect(storageFolderUri) {
    val uri = storageFolderUri?.let(Uri::parse)
    if (uri != state.rootUri) viewModel.setStorageFolder(uri)
  }

  var showCreateFolder by rememberSaveable { mutableStateOf(false) }
  var folderName by rememberSaveable { mutableStateOf("") }
  var deleteTarget by remember { mutableStateOf<Uri?>(null) }
  var showDeleteSelected by rememberSaveable { mutableStateOf(false) }
  var showMovePicker by rememberSaveable { mutableStateOf(false) }
  val destinationStack = remember { mutableStateListOf<Uri>() }

  Box(modifier = modifier.fillMaxSize()) {
    Column(
      modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
    if (state.rootUri == null) {
      Text("Set a storage folder in Recorder settings before recording.", style = MaterialTheme.typography.bodyMedium)
      Spacer(Modifier.height(4.dp))
    } else {
      if (state.folderStack.size > 1) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
          OutlinedButton(onClick = viewModel::navigateUp, enabled = !state.isRecording) { Text("Up") }
          Text("Inside: ${state.folderStack.lastOrNull()?.let(viewModel::folderName) ?: "Folder"}", style = MaterialTheme.typography.titleMedium)
        }
      }
      if (state.message != null) {
        Text(state.message!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }

      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
          onClick = { showCreateFolder = true },
          enabled = !state.isRecording,
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
        ) { Text("New folder", style = MaterialTheme.typography.labelSmall) }
        if (state.selectedUris.isNotEmpty()) {
          OutlinedButton(onClick = {
            destinationStack.clear()
            state.rootUri?.let(destinationStack::add)
            showMovePicker = true
          }, enabled = !state.isRecording, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)) {
            Text("Move (${state.selectedUris.size})", style = MaterialTheme.typography.labelSmall)
          }
          OutlinedButton(
            onClick = { showDeleteSelected = true },
            enabled = !state.isRecording,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
          ) { Text("Delete (${state.selectedUris.size})", style = MaterialTheme.typography.labelSmall) }
          TextButton(onClick = viewModel::clearSelection) { Text("Clear") }
        }
      }

      if (state.entries.isEmpty()) {
        Text("This folder is empty. Start a recording or create a folder.", style = MaterialTheme.typography.bodySmall)
      } else {
        LazyColumn(
          verticalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.weight(1f),
          contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 88.dp)
        ) {
          items(state.entries, key = { it.uri.toString() }) { entry ->
            if (entry.isDirectory) {
              FolderRow(entry, enabled = !state.isRecording) { viewModel.openFolder(entry.uri) }
            } else {
              RecordingFileRow(
                entry = entry,
                selected = entry.uri.toString() in state.selectedUris,
                enabled = !state.isRecording,
                onSelect = { viewModel.toggleSelected(entry.uri) },
                isPlaying = state.playbackUri == entry.uri.toString() && state.isPlaybackPlaying,
                isLoading = state.playbackUri == entry.uri.toString() && state.isPlaybackLoading,
                onPlayPause = { viewModel.togglePlayback(entry.uri) },
                onDelete = { deleteTarget = entry.uri }
              )
            }
          }
        }
      }
    }
    }
    if (state.rootUri != null) {
      if (state.isRecording) {
        Row(
          modifier = Modifier.align(Alignment.BottomEnd).padding(end = 24.dp, bottom = 24.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          ExtendedFloatingActionButton(onClick = onStopRecording) { Text("Stop") }
          ExtendedFloatingActionButton(
            onClick = if (state.isRecordingPaused) onResumeRecording else onPauseRecording
          ) {
            Text(if (state.isRecordingPaused) "Resume" else "Pause")
          }
        }
      } else {
        ExtendedFloatingActionButton(
          onClick = onStartRecording,
          modifier = Modifier.align(Alignment.BottomEnd).padding(end = 24.dp, bottom = 24.dp)
        ) {
          Text("Start")
        }
      }
    }
  }

  if (showCreateFolder) {
    AlertDialog(
      onDismissRequest = { showCreateFolder = false },
      title = { Text("Create folder") },
      text = {
        OutlinedTextField(value = folderName, onValueChange = { folderName = it }, label = { Text("Folder name") }, singleLine = true)
      },
      confirmButton = {
        TextButton(onClick = {
          viewModel.createFolder(folderName)
          folderName = ""
          showCreateFolder = false
        }) { Text("Create") }
      },
      dismissButton = { TextButton(onClick = { showCreateFolder = false }) { Text("Cancel") } }
    )
  }

  if (deleteTarget != null) {
    AlertDialog(
      onDismissRequest = { deleteTarget = null },
      title = { Text("Delete recording?") },
      text = { Text("This file will be permanently deleted.") },
      confirmButton = {
        TextButton(onClick = { deleteTarget?.let(viewModel::delete); deleteTarget = null }) { Text("Delete") }
      },
      dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel") } }
    )
  }

  if (showDeleteSelected) {
    AlertDialog(
      onDismissRequest = { showDeleteSelected = false },
      title = { Text("Delete selected files?") },
      text = { Text("${state.selectedUris.size} file(s) will be permanently deleted.") },
      confirmButton = {
        TextButton(onClick = { viewModel.deleteSelected(); showDeleteSelected = false }) { Text("Delete") }
      },
      dismissButton = { TextButton(onClick = { showDeleteSelected = false }) { Text("Cancel") } }
    )
  }

  if (showMovePicker) {
    val currentDestination = destinationStack.lastOrNull()
    AlertDialog(
      onDismissRequest = { showMovePicker = false },
      title = { Text("Move to ${currentDestination?.let(viewModel::folderName) ?: "folder"}") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          if (destinationStack.size > 1) {
            TextButton(onClick = { destinationStack.removeAt(destinationStack.lastIndex) }) { Text("← Parent folder") }
          }
          val folders = currentDestination?.let(viewModel::listFolders).orEmpty()
          if (folders.isEmpty()) Text("No folders inside")
          folders.forEach { folder ->
            TextButton(onClick = { destinationStack.add(folder.uri) }) { Text("📁  ${folder.name}") }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = {
          currentDestination?.let(viewModel::moveSelected)
          showMovePicker = false
        }) { Text("Move here") }
      },
      dismissButton = { TextButton(onClick = { showMovePicker = false }) { Text("Cancel") } }
    )
  }
}

@Composable
fun RecorderSettingsContent(
  storageFolderUri: String?,
  onChooseStorageFolder: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = androidx.compose.ui.platform.LocalContext.current
  val folderName = remember(storageFolderUri) {
    storageFolderUri?.let { value -> DocumentFile.fromTreeUri(context, Uri.parse(value))?.name }
  }
  Column(
    modifier = modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 20.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    Text("Recording storage", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Text("New audio recordings are saved in this folder. You can organize them with nested folders from the Recorder tab.", style = MaterialTheme.typography.bodyMedium)
    Text("Folder: ${folderName ?: "Not selected"}", style = MaterialTheme.typography.bodyLarge)
    OutlinedButton(onClick = onChooseStorageFolder) {
      Text(if (storageFolderUri == null) "Choose folder" else "Change folder")
    }
  }
}

@Composable
private fun FolderRow(entry: RecordingEntry, enabled: Boolean, onClick: () -> Unit) {
  Card(modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick), shape = RoundedCornerShape(14.dp)) {
    Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
      Text("📁", modifier = Modifier.padding(end = 12.dp))
      Text(entry.name, style = MaterialTheme.typography.bodyLarge)
    }
  }
}

@Composable
private fun RecordingFileRow(
  entry: RecordingEntry,
  selected: Boolean,
  enabled: Boolean,
  isPlaying: Boolean,
  isLoading: Boolean,
  onSelect: () -> Unit,
  onPlayPause: () -> Unit,
  onDelete: () -> Unit
) {
  Card(shape = RoundedCornerShape(14.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Checkbox(checked = selected, onCheckedChange = { onSelect() }, enabled = enabled)
      Column(modifier = Modifier.weight(1f).clickable(enabled = enabled, onClick = onSelect).padding(vertical = 8.dp)) {
        Text(entry.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        Text("Audio recording", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
      TextButton(onClick = onPlayPause, enabled = enabled && !isLoading) {
        Text(if (isLoading) "Loading…" else if (isPlaying) "Pause" else "Play")
      }
      TextButton(onClick = onDelete, enabled = enabled) { Text("Delete") }
    }
  }
}
