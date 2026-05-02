package com.nht.gif

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Owns UI state and orchestrates frame loading/saving for the GIF Split screen.
 * Survives configuration changes; [GifSplitRepository.cleanup] is called on final destruction.
 */
class GifSplitViewModel(
  private val repository: GifSplitRepository,
  private val gifPath: String,
) : ViewModel() {

  /** Represents the full render state of the GIF Split screen. */
  sealed class UiState {
    object Loading : UiState()
    data class FramesReady(val frames: List<Bitmap>, val isSaving: Boolean = false) : UiState()
    object Error : UiState()
  }

  /** One-shot UI events that do not belong in persistent state. */
  sealed class Event {
    object SaveSuccess : Event()
  }

  private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
  val uiState: StateFlow<UiState> = _uiState.asStateFlow()

  private val _events = MutableSharedFlow<Event>()
  val events: SharedFlow<Event> = _events.asSharedFlow()

  init {
    loadFrames()
  }

  private fun loadFrames() {
    viewModelScope.launch {
      val frames = repository.extractFrames(gifPath)
      _uiState.value = if (frames != null) UiState.FramesReady(frames) else UiState.Error
    }
  }

  /**
   * Saves the frame at [frameIndex] (1-based) to the gallery.
   * No-op if the current state is not [UiState.FramesReady].
   */
  fun saveFrame(frameIndex: Int) {
    val current = _uiState.value as? UiState.FramesReady ?: return
    viewModelScope.launch {
      _uiState.value = current.copy(isSaving = true)
      repository.saveFrame(gifPath, frameIndex)
      _uiState.value = current.copy(isSaving = false)
      _events.emit(Event.SaveSuccess)
    }
  }

  override fun onCleared() {
    super.onCleared()
    repository.cleanup()
  }

  companion object {
    /** Factory that wires a default [GifSplitRepository] with the given [gifPath]. */
    fun factory(gifPath: String) = object : ViewModelProvider.Factory {
      @Suppress("UNCHECKED_CAST")
      override fun <T : ViewModel> create(modelClass: Class<T>): T =
        GifSplitViewModel(GifSplitRepository(), gifPath) as T
    }
  }
}
