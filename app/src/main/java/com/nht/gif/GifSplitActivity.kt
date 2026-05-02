package com.nht.gif

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.View.GONE
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.nht.gif.databinding.ActivityGifSplitBinding
import com.nht.gif.toolbox.Toolbox.getExtra
import com.nht.gif.toolbox.Toolbox.onClick
import com.nht.gif.toolbox.Toolbox.toast
import kotlinx.coroutines.launch

/**
 * Pure View layer for the GIF Split screen.
 * All business logic lives in [GifSplitViewModel]; this class only renders state and forwards
 * user actions.
 */
class GifSplitActivity : BaseActivity() {

  private val binding by lazy { ActivityGifSplitBinding.inflate(layoutInflater) }
  private val inputGifPath by lazy { intent.getExtra<String>(MyConstants.EXTRA_GIF_PATH) }
  private val viewModel: GifSplitViewModel by lazy {
    ViewModelProvider(this, GifSplitViewModel.factory(inputGifPath))[GifSplitViewModel::class.java]
  }

  /**
   * True once the frame section (slider + initial image) has been initialized for this
   * Activity instance. Prevents re-initialization on [isSaving] state changes while correctly
   * re-initializing after rotation (new Activity instance resets this flag).
   */
  private var framesSectionInitialized = false

  override fun onCreateIfEulaAccepted(savedInstanceState: Bundle?) {
    setContentView(binding.root)
    binding.mbClose.onClick { finish() }
    binding.mbSliderMinus.onClick { if (binding.slider.value > binding.slider.valueFrom) binding.slider.value-- }
    binding.mbSliderPlus.onClick { if (binding.slider.value < binding.slider.valueTo) binding.slider.value++ }
    binding.mbSave.onClick { viewModel.saveFrame(binding.slider.value.toInt()) }

    lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        launch { viewModel.uiState.collect { render(it) } }
        launch { viewModel.events.collect { handle(it) } }
      }
    }
  }

  private fun render(state: GifSplitViewModel.UiState) {
    when (state) {
      GifSplitViewModel.UiState.Loading -> setControlsEnabled(false)
      is GifSplitViewModel.UiState.FramesReady -> {
        if (!framesSectionInitialized) {
          framesSectionInitialized = true
          initFrameSection(state)
        }
        setControlsEnabled(!state.isSaving)
      }
      GifSplitViewModel.UiState.Error -> {
        if (!isFinishing) {
          toast(R.string.unable_to_load_gif)
          finish()
        }
      }
    }
  }

  private fun initFrameSection(state: GifSplitViewModel.UiState.FramesReady) {
    if (state.frames.size == 1) {
      binding.llcFrameSelector.visibility = GONE
    } else {
      binding.slider.apply {
        valueTo = state.frames.size.toFloat()
        setLabelFormatter { "${it.toInt()}/${valueTo.toInt()}" }
        addOnChangeListener { slider, value, _ ->
          slider.performHapticFeedback(HapticFeedbackConstants.TEXT_HANDLE_MOVE)
          binding.aciv.setImageBitmap(state.frames[value.toInt() - 1])
        }
      }
    }
    binding.aciv.setImageBitmap(state.frames[0])
  }

  private fun handle(event: GifSplitViewModel.Event) {
    when (event) {
      GifSplitViewModel.Event.SaveSuccess -> {
        toast(R.string.saved_this_frame_to_gallery)
        binding.view.apply {
          visibility = View.VISIBLE
          postDelayed({ visibility = View.INVISIBLE }, 50)
        }
      }
    }
  }

  /** Toggles Save, Slider, and ± buttons as a group to prevent concurrent operations. */
  private fun setControlsEnabled(enabled: Boolean) {
    binding.mbSave.isEnabled = enabled
    binding.mbSliderMinus.isEnabled = enabled
    binding.mbSliderPlus.isEnabled = enabled
    binding.slider.isEnabled = enabled
  }

  companion object {
    fun start(context: Context, gifPath: String) = context.startActivity(
      Intent(context, GifSplitActivity::class.java).putExtra(MyConstants.EXTRA_GIF_PATH, gifPath)
    )
  }
}
