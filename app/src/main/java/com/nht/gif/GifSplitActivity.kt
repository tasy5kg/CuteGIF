package com.nht.gif

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.View.GONE
import com.arthenica.ffmpegkit.FFmpegKit
import com.nht.gif.MyConstants.OUTPUT_SPLIT_DIR
import com.nht.gif.databinding.ActivityGifSplitBinding
import com.nht.gif.toolbox.FileTools.copyFile
import com.nht.gif.toolbox.FileTools.createNewFile
import com.nht.gif.toolbox.FileTools.resetDirectory
import com.nht.gif.toolbox.Toolbox.getExtra
import com.nht.gif.toolbox.Toolbox.onClick
import com.nht.gif.toolbox.Toolbox.toast
import java.io.File

class GifSplitActivity : BaseActivity() {
  private val binding by lazy { ActivityGifSplitBinding.inflate(layoutInflater) }
  private val inputGifPath by lazy { intent.getExtra<String>(MyConstants.EXTRA_GIF_PATH) }

  override fun onCreateIfEulaAccepted(savedInstanceState: Bundle?) {
    setContentView(binding.root)
    binding.mbClose.onClick { finish() }
    binding.mbSliderMinus.onClick { if (binding.slider.value > binding.slider.valueFrom) binding.slider.value-- }
    binding.mbSliderPlus.onClick { if (binding.slider.value < binding.slider.valueTo) binding.slider.value++ }
    resetDirectory(OUTPUT_SPLIT_DIR)
    FFmpegKit.execute("${MyConstants.FFMPEG_COMMAND_PREFIX_FOR_ALL_AN} -i \"$inputGifPath\" \"$OUTPUT_SPLIT_DIR%06d.png\"")
    val frameCount = File(OUTPUT_SPLIT_DIR).listFiles()?.size
    if (frameCount == null) {
      toast(R.string.unable_to_load_gif)
      finish()
      return
    }
    val mlo = (1..frameCount).map { BitmapFactory.decodeFile(OUTPUT_SPLIT_DIR + String.format("%06d", it) + ".png")!! }
    if (mlo.size == 1) {
      binding.llcFrameSelector.visibility = GONE
    } else {
      binding.slider.apply {
        valueTo = mlo.size.toFloat()
        setLabelFormatter { "${it.toInt()}/${valueTo.toInt()}" }
        addOnChangeListener { slider, value, _ ->
          slider.performHapticFeedback(HapticFeedbackConstants.TEXT_HANDLE_MOVE)
          binding.aciv.setImageBitmap(mlo[value.toInt() - 1])
        }
      }
    }
    binding.aciv.setImageBitmap(mlo[0])
    binding.mbSave.onClick {
      copyFile(
        "$OUTPUT_SPLIT_DIR${String.format("%06d", binding.slider.value.toInt())}.png",
        createNewFile(inputGifPath, "png")
      )
      toast(R.string.saved_this_frame_to_gallery)
      binding.view.apply {
        visibility = View.VISIBLE
        postDelayed({
          visibility = View.INVISIBLE
        }, 50)
      }
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    resetDirectory(OUTPUT_SPLIT_DIR)
  }

  companion object {
    fun start(context: Context, gifPath: String) = context.startActivity(
      Intent(
        context, GifSplitActivity::class.java
      ).putExtra(MyConstants.EXTRA_GIF_PATH, gifPath)
    )
  }
}