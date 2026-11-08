package com.sahilrajput.pomodoro.ui.main

import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.compose.foundation.clickable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
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
  var deleteFolderTarget by remember { mutableStateOf<Uri?>(null) }
  var renameTarget by remember { mutableStateOf<RecordingEntry?>(null) }
  var renameName by rememberSaveable(stateSaver = TextFieldValue.Saver) {
    mutableStateOf(TextFieldValue(""))
  }
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
          CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
            OutlinedButton(
              onClick = viewModel::navigateUp,
              enabled = !state.isRecording,
              modifier = Modifier.height(32.dp),
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
              Text("↑", style = MaterialTheme.typography.labelSmall)
            }
          }
          Text(
            state.folderStack.drop(1)
              .joinToString(separator = "/", prefix = "/") { viewModel.folderName(it) },
            style = MaterialTheme.typography.labelSmall
          )
        }
      }
      if (state.message != null) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            state.message.orEmpty(),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
            IconButton(onClick = viewModel::clearMessage, modifier = Modifier.height(28.dp)) {
              Text("×", style = MaterialTheme.typography.titleMedium)
            }
          }
        }
      }

      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
          OutlinedButton(
            onClick = { showCreateFolder = true },
            enabled = !state.isRecording,
            modifier = Modifier.height(32.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
          ) { Text("New folder", style = MaterialTheme.typography.labelSmall) }
          if (state.selectedUris.isNotEmpty()) {
            OutlinedButton(
              onClick = {
                destinationStack.clear()
                state.rootUri?.let(destinationStack::add)
                showMovePicker = true
              },
              enabled = !state.isRecording,
              modifier = Modifier.height(32.dp),
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
              Text("Move (${state.selectedUris.size})", style = MaterialTheme.typography.labelSmall)
            }
            OutlinedButton(
              onClick = { showDeleteSelected = true },
              enabled = !state.isRecording,
              modifier = Modifier.height(32.dp),
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) { Text("Delete (${state.selectedUris.size})", style = MaterialTheme.typography.labelSmall) }
          }
        }
        if (state.selectedUris.isNotEmpty()) {
          CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
            TextButton(
              onClick = viewModel::clearSelection,
              modifier = Modifier.height(32.dp),
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
              Text("Clear", style = MaterialTheme.typography.labelSmall)
            }
          }
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
              FolderRow(
                entry = entry,
                selected = entry.uri.toString() in state.selectedUris,
                enabled = !state.isRecording,
                onClick = { viewModel.openFolder(entry.uri) },
                onSelect = { viewModel.toggleSelected(entry.uri) },
                onRename = {
                  renameName = TextFieldValue(entry.name, selection = TextRange(entry.name.length))
                  renameTarget = entry
                },
                onDelete = { deleteFolderTarget = entry.uri }
              )
            } else {
              RecordingFileRow(
                entry = entry,
                selected = entry.uri.toString() in state.selectedUris,
                enabled = !state.isRecording,
                onSelect = { viewModel.toggleSelected(entry.uri) },
                isPlaying = state.playbackUri == entry.uri.toString() && state.isPlaybackPlaying,
                isLoading = state.playbackUri == entry.uri.toString() && state.isPlaybackLoading,
                onPlayPause = { viewModel.togglePlayback(entry.uri) },
                onRename = {
                  val name = entry.name.substringBeforeLast('.', entry.name)
                  renameName = TextFieldValue(name, selection = TextRange(name.length))
                  renameTarget = entry
                },
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
        val focusRequester = remember { FocusRequester() }
        val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
        LaunchedEffect(Unit) {
          focusRequester.requestFocus()
          keyboardController?.show()
        }
        OutlinedTextField(
          value = folderName,
          onValueChange = { folderName = it },
          label = { Text("Folder name") },
          singleLine = true,
          modifier = Modifier.focusRequester(focusRequester)
        )
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

  if (deleteFolderTarget != null) {
    AlertDialog(
      onDismissRequest = { deleteFolderTarget = null },
      title = { Text("Delete folder?") },
      text = { Text("This folder and everything inside it will be permanently deleted.") },
      confirmButton = {
        TextButton(onClick = {
          deleteFolderTarget?.let(viewModel::deleteFolder)
          deleteFolderTarget = null
        }) { Text("Delete") }
      },
      dismissButton = { TextButton(onClick = { deleteFolderTarget = null }) { Text("Cancel") } }
    )
  }

  if (renameTarget != null) {
    val target = renameTarget
    val extension = target?.takeIf { !it.isDirectory && it.name.contains('.') }?.name?.substringAfterLast('.')
    AlertDialog(
      onDismissRequest = { renameTarget = null },
      title = { Text(if (target?.isDirectory == true) "Rename folder" else "Rename recording") },
      text = {
        val focusRequester = remember { FocusRequester() }
        val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
        LaunchedEffect(Unit) {
          focusRequester.requestFocus()
          keyboardController?.show()
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = renameName,
            onValueChange = { renameName = it },
            label = { Text(if (target?.isDirectory == true) "Folder name" else "File name") },
            singleLine = true,
            modifier = Modifier.focusRequester(focusRequester)
          )
          if (extension != null) {
            Text("The .$extension extension will be kept.", style = MaterialTheme.typography.bodySmall)
          }
        }
      },
      confirmButton = {
        TextButton(onClick = {
          renameTarget?.let { viewModel.rename(it.uri, renameName.text) }
          renameTarget = null
        }) { Text("Rename") }
      },
      dismissButton = { TextButton(onClick = { renameTarget = null }) { Text("Cancel") } }
    )
  }

  if (showDeleteSelected) {
    AlertDialog(
      onDismissRequest = { showDeleteSelected = false },
      title = { Text("Delete selected items?") },
      text = { Text("${state.selectedUris.size} item(s) will be permanently deleted.") },
      confirmButton = {
        TextButton(onClick = { viewModel.deleteSelected(); showDeleteSelected = false }) { Text("Delete") }
      },
      dismissButton = { TextButton(onClick = { showDeleteSelected = false }) { Text("Cancel") } }
    )
  }

  if (showMovePicker) {
    val currentDestination = destinationStack.lastOrNull()
    val movesIntoSelectedFolder = destinationStack.any { it.toString() in state.selectedUris }
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
          if (movesIntoSelectedFolder) {
            Text("A folder can't be moved into itself or one of its subfolders.")
          }
          folders.forEach { folder ->
            TextButton(onClick = { destinationStack.add(folder.uri) }) { Text("📁  ${folder.name}") }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = {
          currentDestination?.let(viewModel::moveSelected)
          showMovePicker = false
        }, enabled = !movesIntoSelectedFolder) { Text("Move here") }
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
private fun FolderRow(
  entry: RecordingEntry,
  selected: Boolean,
  enabled: Boolean,
  onClick: () -> Unit,
  onSelect: () -> Unit,
  onRename: () -> Unit,
  onDelete: () -> Unit
) {
  Card(shape = RoundedCornerShape(14.dp)) {
    Row(modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
      Checkbox(checked = selected, onCheckedChange = { onSelect() }, enabled = enabled)
      Row(
        modifier = Modifier.weight(1f).clickable(enabled = enabled, onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("📁", modifier = Modifier.padding(end = 12.dp))
        Text(entry.name, style = MaterialTheme.typography.bodyLarge)
      }
      TextButton(onClick = onRename, enabled = enabled) { Text("Rename") }
      TextButton(onClick = onDelete, enabled = enabled) { Text("Delete") }
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
  onRename: () -> Unit,
  onDelete: () -> Unit
) {
  Card(shape = RoundedCornerShape(14.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Checkbox(checked = selected, onCheckedChange = { onSelect() }, enabled = enabled)
      Column(modifier = Modifier.weight(1f).padding(vertical = 4.dp)) {
        Text(
          entry.name,
          modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onSelect).padding(vertical = 1.dp),
          style = MaterialTheme.typography.bodyLarge,
          fontWeight = FontWeight.Medium
        )
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(
              onClick = onPlayPause,
              enabled = enabled && !isLoading,
              modifier = Modifier.height(32.dp),
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
              Text(if (isLoading) "Loading…" else if (isPlaying) "Pause" else "Play")
            }
            TextButton(
              onClick = onRename,
              enabled = enabled,
              modifier = Modifier.height(32.dp),
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) { Text("Rename") }
            TextButton(
              onClick = onDelete,
              enabled = enabled,
              modifier = Modifier.height(32.dp),
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) { Text("Delete") }
          }
        }
      }
    }
  }
}
