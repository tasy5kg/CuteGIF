package me.tasy5kg.cutegif

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.View.GONE
import com.arthenica.ffmpegkit.FFmpegKit
import me.tasy5kg.cutegif.MyConstants.OUTPUT_SPLIT_DIR
import me.tasy5kg.cutegif.databinding.ActivityGifSplitBinding
import me.tasy5kg.cutegif.toolbox.FileTools.copyFile
import me.tasy5kg.cutegif.toolbox.FileTools.createNewFile
import me.tasy5kg.cutegif.toolbox.FileTools.makeDirEmpty
import me.tasy5kg.cutegif.toolbox.Toolbox.getExtra
import me.tasy5kg.cutegif.toolbox.Toolbox.onClick
import me.tasy5kg.cutegif.toolbox.Toolbox.toast
import java.io.File

class GifSplitActivity : BaseActivity() {
  private val binding by lazy { ActivityGifSplitBinding.inflate(layoutInflater) }
  private val inputGifPath by lazy { intent.getExtra<String>(MyConstants.EXTRA_GIF_PATH) }

  override fun onCreateIfEulaAccepted(savedInstanceState: Bundle?) {
    setContentView(binding.root)
    binding.mbClose.onClick { finish() }
    binding.mbSliderMinus.onClick { if (binding.slider.value > binding.slider.valueFrom) binding.slider.value-- }
    binding.mbSliderPlus.onClick { if (binding.slider.value < binding.slider.valueTo) binding.slider.value++ }
    makeDirEmpty(OUTPUT_SPLIT_DIR)
    val mlo = mutableListOf<Bitmap>()
    FFmpegKit.execute("${MyConstants.FFMPEG_COMMAND_PREFIX_FOR_ALL_AN} -i \"$inputGifPath\" \"$OUTPUT_SPLIT_DIR%05d.png\"")
    var frameIndex = 1
    while (File("$OUTPUT_SPLIT_DIR${String.format("%05d", frameIndex)}.png").exists()) {
      mlo.add(BitmapFactory.decodeFile("$OUTPUT_SPLIT_DIR${String.format("%05d", frameIndex)}.png"))
      frameIndex++
    }
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
        "$OUTPUT_SPLIT_DIR${String.format("%05d", binding.slider.value.toInt())}.png",
        createNewFile(inputGifPath, "png")
      )
      toast(context.getString(R.string.saved_this_frame_to_gallery))
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
    makeDirEmpty(OUTPUT_SPLIT_DIR)
  }

  companion object {
    fun start(context: Context, gifPath: String) = context.startActivity(
      Intent(
        context, GifSplitActivity::class.java
      ).putExtra(MyConstants.EXTRA_GIF_PATH, gifPath)
    )
  }
}