package com.sahilrajput.pomodoro.ui.main

import android.app.Application
import android.media.MediaRecorder
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RecordingEntry(
  val uri: Uri,
  val name: String,
  val isDirectory: Boolean,
  val mimeType: String?
)

data class RecordingUiState(
  val rootUri: Uri? = null,
  val currentFolderUri: Uri? = null,
  val folderStack: List<Uri> = emptyList(),
  val entries: List<RecordingEntry> = emptyList(),
  val selectedUris: Set<String> = emptySet(),
  val isRecording: Boolean = false,
  val isRecordingPaused: Boolean = false,
  val playbackUri: String? = null,
  val isPlaybackPlaying: Boolean = false,
  val isPlaybackLoading: Boolean = false,
  val message: String? = null
)

class RecordingViewModel(application: Application) : AndroidViewModel(application) {
  private val preferences = application.getSharedPreferences("pomodoro_settings", Application.MODE_PRIVATE)
  private val _uiState = MutableStateFlow(RecordingUiState())
  val uiState: StateFlow<RecordingUiState> = _uiState.asStateFlow()

  private var recorder: MediaRecorder? = null
  private var outputDescriptor: ParcelFileDescriptor? = null
  private var activeRecording: Uri? = null
  private var mediaPlayer: MediaPlayer? = null

  fun setStorageFolder(uri: Uri?) {
    if (uri == null) {
      _uiState.value = RecordingUiState()
      return
    }
    val root = DocumentFile.fromTreeUri(getApplication(), uri)
    if (root == null || !root.isDirectory) {
      _uiState.update { it.copy(message = "Could not open that folder") }
      return
    }
    preferences.edit().putString("recording_folder_uri", uri.toString()).apply()
    _uiState.value = RecordingUiState(rootUri = uri, currentFolderUri = uri, folderStack = listOf(uri))
    refresh()
  }

  fun openFolder(uri: Uri) {
    if (_uiState.value.isRecording) return
    val folder = document(uri) ?: return
    if (!folder.isDirectory) return
    _uiState.update { state ->
      state.copy(currentFolderUri = uri, folderStack = state.folderStack + uri, selectedUris = emptySet())
    }
    refresh()
  }

  fun navigateUp() {
    if (_uiState.value.isRecording) return
    _uiState.update { state ->
      if (state.folderStack.size <= 1) state
      else state.copy(folderStack = state.folderStack.dropLast(1), currentFolderUri = state.folderStack[state.folderStack.lastIndex - 1], selectedUris = emptySet())
    }
    refresh()
  }

  fun createFolder(name: String) {
    val cleanName = name.trim()
    if (cleanName.isEmpty()) return
    val folder = _uiState.value.currentFolderUri?.let(::document) ?: return
    if (_uiState.value.isRecording) return
    val created = folder.createDirectory(cleanName)
    _uiState.update { it.copy(message = if (created != null) "Folder created" else "Couldn't create folder") }
    refresh()
  }

  fun toggleSelected(uri: Uri) {
    val key = uri.toString()
    _uiState.update { state ->
      val updated = state.selectedUris.toMutableSet()
      if (!updated.add(key)) updated.remove(key)
      state.copy(selectedUris = updated)
    }
  }

  fun clearSelection() = _uiState.update { it.copy(selectedUris = emptySet()) }

  fun startRecording() {
    if (_uiState.value.isRecording) return
    val folder = _uiState.value.currentFolderUri?.let(::document)
    if (folder == null || !folder.isDirectory) {
      _uiState.update { it.copy(message = "Choose a folder to save recordings") }
      return
    }
    stopPlayback()
    val timestamp = Calendar.getInstance()
    val formatter = SimpleDateFormat("yyyy-MM-dd_hh-mm-a", Locale.US)
    var fileName = formatter.format(timestamp.time) + ".m4a"
    while (folder.findFile(fileName) != null) {
      timestamp.add(Calendar.MINUTE, 1)
      fileName = formatter.format(timestamp.time) + ".m4a"
    }
    val destination = folder.createFile("audio/mp4", fileName)
    if (destination == null) {
      _uiState.update { it.copy(message = "Couldn't create the recording file") }
      return
    }
    try {
      val descriptor = getApplication<Application>().contentResolver.openFileDescriptor(destination.uri, "rw")
        ?: throw IOException("Couldn't open the recording file")
      outputDescriptor = descriptor
      val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        MediaRecorder(getApplication())
      } else {
        @Suppress("DEPRECATION")
        MediaRecorder()
      }
      mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
      mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
      mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
      mediaRecorder.setOutputFile(descriptor.fileDescriptor)
      recorder = mediaRecorder
      mediaRecorder.prepare()
      mediaRecorder.start()
      activeRecording = destination.uri
      _uiState.update { it.copy(isRecording = true, message = "Recording to $fileName") }
    } catch (error: Exception) {
      destination.delete()
      _uiState.update { it.copy(message = "Couldn't start recording: ${error.localizedMessage ?: "microphone unavailable"}") }
      recorder?.release()
      recorder = null
      outputDescriptor?.close()
      outputDescriptor = null
      activeRecording = null
    }
  }

  fun stopRecording() {
    val currentRecorder = recorder ?: return
    var errorMessage: String? = null
    try {
      currentRecorder.stop()
    } catch (_: RuntimeException) {
      activeRecording?.let { document(it)?.delete() }
      errorMessage = "Recording was too short and was discarded"
    } finally {
      currentRecorder.release()
      recorder = null
      outputDescriptor?.close()
      outputDescriptor = null
      activeRecording = null
      _uiState.update { it.copy(isRecording = false, isRecordingPaused = false, message = errorMessage ?: "Recording saved") }
      refresh()
    }
  }

  fun pauseRecording() {
    if (!_uiState.value.isRecording || _uiState.value.isRecordingPaused) return
    try {
      recorder?.pause()
      _uiState.update { it.copy(isRecordingPaused = true, message = "Recording paused") }
    } catch (error: RuntimeException) {
      _uiState.update { it.copy(message = "Couldn't pause recording: ${error.localizedMessage ?: "recorder unavailable"}") }
    }
  }

  fun resumeRecording() {
    if (!_uiState.value.isRecording || !_uiState.value.isRecordingPaused) return
    try {
      recorder?.resume()
      _uiState.update { it.copy(isRecordingPaused = false, message = "Recording resumed") }
    } catch (error: RuntimeException) {
      _uiState.update { it.copy(message = "Couldn't resume recording: ${error.localizedMessage ?: "recorder unavailable"}") }
    }
  }

  fun delete(uri: Uri) {
    if (_uiState.value.isRecording) return
    if (_uiState.value.playbackUri == uri.toString()) stopPlayback()
    val deleted = document(uri)?.delete() == true
    _uiState.update { it.copy(message = if (deleted) "File deleted" else "Couldn't delete file") }
    refresh()
  }

  fun deleteFolder(uri: Uri) {
    if (_uiState.value.isRecording) return
    val folder = document(uri)
    if (folder == null || !folder.isDirectory) {
      _uiState.update { it.copy(message = "Couldn't delete folder") }
      return
    }
    val deleted = deleteRecursively(folder)
    _uiState.update { it.copy(message = if (deleted) "Folder deleted" else "Couldn't delete folder") }
    refresh()
  }

  private fun deleteRecursively(file: DocumentFile): Boolean {
    if (file.isDirectory && !file.listFiles().map(::deleteRecursively).all { it }) return false
    if (_uiState.value.playbackUri == file.uri.toString()) stopPlayback()
    return file.delete()
  }

  fun rename(uri: Uri, requestedName: String) {
    if (_uiState.value.isRecording) return
    val file = document(uri)
    val oldName = file?.name
    if (file == null || oldName == null) {
      _uiState.update { it.copy(message = "Couldn't find item to rename") }
      return
    }
    val baseName = requestedName.trim()
    val extension = oldName.substringAfterLast('.', "").takeIf { !file.isDirectory && oldName.contains('.') }
    val cleanBaseName = if (extension != null && baseName.endsWith(".$extension", ignoreCase = true)) {
      baseName.dropLast(extension.length + 1)
    } else baseName
    if (cleanBaseName.isBlank() || cleanBaseName == "." || cleanBaseName == ".." ||
      cleanBaseName.contains('/') || cleanBaseName.contains('\\')
    ) {
      _uiState.update { it.copy(message = "Enter a valid file name") }
      return
    }
    val newName = if (extension == null) cleanBaseName else "$cleanBaseName.$extension"
    val parent = _uiState.value.currentFolderUri?.let(::document)
    if (parent == null) {
      _uiState.update { it.copy(message = "Couldn't find the current folder") }
      return
    }
    val duplicate = parent.findFile(newName)
    if (duplicate != null && duplicate.uri != uri) {
      _uiState.update { it.copy(message = "An item with that name already exists") }
      return
    }
    if (_uiState.value.playbackUri == uri.toString()) stopPlayback()
    val renamed = file.renameTo(newName)
    _uiState.update {
      it.copy(
        selectedUris = emptySet(),
        message = if (renamed) "Renamed successfully" else "Couldn't rename item"
      )
    }
    refresh()
  }

  fun deleteSelected() {
    if (_uiState.value.isRecording) return
    val selected = _uiState.value.selectedUris.toList()
    if (_uiState.value.playbackUri in selected) stopPlayback()
    val deleted = selected.count { document(Uri.parse(it))?.delete() == true }
    _uiState.update { it.copy(selectedUris = emptySet(), message = "$deleted file${if (deleted == 1) "" else "s"} deleted") }
    refresh()
  }

  fun moveSelected(destinationFolderUri: Uri) {
    if (_uiState.value.isRecording) return
    val state = _uiState.value
    if (state.playbackUri != null && state.playbackUri in state.selectedUris) stopPlayback()
    val target = document(destinationFolderUri)
    if (target == null || !target.isDirectory) return
    var moved = 0
    var skipped = 0
    state.selectedUris.forEach { value ->
      val sourceUri = Uri.parse(value)
      val source = document(sourceUri) ?: return@forEach
      if (source.isDirectory) {
        skipped++
        return@forEach
      }
      // fromTreeUri() creates a fresh DocumentFile whose parentFile is null;
      // the selected files are all children of the folder currently displayed.
      val sourceParent = state.currentFolderUri?.let(::document) ?: return@forEach
      if (sourceParent.uri == target.uri) {
        skipped++
        return@forEach
      }
      if (target.findFile(source.name.orEmpty()) != null) {
        skipped++
        return@forEach
      }
      val success = try {
        val movedUri = DocumentsContract.moveDocument(
          getApplication<Application>().contentResolver,
          sourceUri,
          sourceParent.uri,
          target.uri
        )
        movedUri != null || copyThenDelete(source, target)
      } catch (_: Exception) {
        copyThenDelete(source, target)
      }
      if (success) moved++
    }
    val total = state.selectedUris.size
    val message = if (moved == total) {
      "$moved file${if (moved == 1) "" else "s"} moved"
    } else if (moved == 0 && skipped == total) {
      "Files are already in this folder or have duplicate names."
    } else {
      "Moved $moved of $total files. Some files couldn't be moved."
    }
    _uiState.update { it.copy(selectedUris = emptySet(), message = message) }
    refresh()
  }

  fun listFolders(uri: Uri): List<RecordingEntry> = document(uri)?.listFiles()
    ?.filter { it.isDirectory }
    ?.map { RecordingEntry(it.uri, it.name ?: "Folder", true, it.type) }
    ?.sortedBy { it.name.lowercase() }
    .orEmpty()

  fun folderName(uri: Uri): String = document(uri)?.name ?: "Folder"

  fun togglePlayback(uri: Uri) {
    val uriString = uri.toString()
    if (_uiState.value.playbackUri == uriString) {
      val currentPlayer = mediaPlayer ?: return
      if (_uiState.value.isPlaybackLoading) return
      if (_uiState.value.isPlaybackPlaying) {
        currentPlayer.pause()
        _uiState.update { it.copy(isPlaybackPlaying = false) }
      } else {
        currentPlayer.start()
        _uiState.update { it.copy(isPlaybackPlaying = true) }
      }
      return
    }
    stopPlayback()
    val player = MediaPlayer()
    mediaPlayer = player
    _uiState.update { it.copy(playbackUri = uriString, isPlaybackLoading = true, isPlaybackPlaying = false, message = null) }
    try {
      player.setDataSource(getApplication(), uri)
      player.setOnPreparedListener { prepared ->
        if (mediaPlayer === prepared) {
          prepared.start()
          _uiState.update { it.copy(playbackUri = uriString, isPlaybackLoading = false, isPlaybackPlaying = true) }
        }
      }
      player.setOnCompletionListener { completed ->
        if (mediaPlayer === completed) {
          completed.release()
          mediaPlayer = null
          _uiState.update { it.copy(playbackUri = null, isPlaybackLoading = false, isPlaybackPlaying = false) }
        }
      }
      player.setOnErrorListener { failed, _, _ ->
        if (mediaPlayer === failed) {
          failed.release()
          mediaPlayer = null
          _uiState.update { it.copy(playbackUri = null, isPlaybackLoading = false, isPlaybackPlaying = false, message = "Couldn't play this audio file") }
        }
        true
      }
      player.prepareAsync()
    } catch (error: Exception) {
      player.release()
      mediaPlayer = null
      _uiState.update { it.copy(playbackUri = null, isPlaybackLoading = false, isPlaybackPlaying = false, message = "Couldn't play this audio file") }
    }
  }

  private fun stopPlayback() {
    try {
      mediaPlayer?.stop()
    } catch (_: IllegalStateException) {
      // The player may still be preparing.
    }
    mediaPlayer?.release()
    mediaPlayer = null
    _uiState.update { it.copy(playbackUri = null, isPlaybackLoading = false, isPlaybackPlaying = false) }
  }

  private fun copyThenDelete(source: DocumentFile, destinationFolder: DocumentFile): Boolean {
    var copy: DocumentFile? = null
    return try {
      copy = destinationFolder.createFile(source.type ?: "application/octet-stream", source.name ?: return false)
        ?: return false
      val resolver = getApplication<Application>().contentResolver
      val input = resolver.openInputStream(source.uri) ?: run { copy.delete(); return false }
      val output = resolver.openOutputStream(copy.uri, "w") ?: run { input.close(); copy.delete(); return false }
      input.use { sourceStream -> output.use { outputStream -> sourceStream.copyTo(outputStream) } }
      if (source.delete()) true else { copy.delete(); false }
    } catch (_: Exception) {
      copy?.delete()
      false
    }
  }

  private fun refresh() {
    val folderUri = _uiState.value.currentFolderUri ?: return
    viewModelScope.launch(Dispatchers.IO) {
      val entries = document(folderUri)?.listFiles()
        ?.mapNotNull { file ->
          val name = file.name ?: return@mapNotNull null
          if (!file.isDirectory && name.startsWith(".")) return@mapNotNull null
          RecordingEntry(file.uri, name, file.isDirectory, file.type)
        }
        ?.sortedWith(compareByDescending<RecordingEntry> { it.isDirectory }.thenBy { it.name.lowercase() })
        .orEmpty()
      _uiState.update { state ->
        if (state.currentFolderUri == folderUri) state.copy(entries = entries) else state
      }
    }
  }

  private fun document(uri: Uri): DocumentFile? = DocumentFile.fromTreeUri(getApplication(), uri)

  override fun onCleared() {
    stopRecording()
    stopPlayback()
    super.onCleared()
  }
}
