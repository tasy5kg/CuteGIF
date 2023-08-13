package me.tasy5kg.cutegif

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import com.arthenica.ffmpegkit.FFmpegKit
import me.tasy5kg.cutegif.MyConstants.OUTPUT_SPLIT_DIR
import me.tasy5kg.cutegif.Toolbox.createFfSafForRead
import me.tasy5kg.cutegif.Toolbox.getExtra
import me.tasy5kg.cutegif.Toolbox.makeDirEmpty
import me.tasy5kg.cutegif.Toolbox.onClick
import me.tasy5kg.cutegif.Toolbox.toast
import me.tasy5kg.cutegif.databinding.ActivityGifSplitBinding
import java.io.File

class GifSplitActivity : BaseActivity() {
  private val binding by lazy { ActivityGifSplitBinding.inflate(layoutInflater) }
  private val inputGifUri by lazy {
    with(intent) {
      getExtra(MyConstants.EXTRA_GIF_URI) ?: getExtra(Intent.EXTRA_STREAM) ?: data ?: Uri.EMPTY
    }
  }

  override fun onCreateIfEulaAccepted(savedInstanceState: Bundle?) {
    setContentView(binding.root)
    if (inputGifUri == Uri.EMPTY) {
      finish()
      return
    }
    val aciv = binding.aciv
    val slider = binding.slider
    binding.mbClose.onClick { finish() }
    binding.mbSliderMinus.onClick {
      if (slider.value > slider.valueFrom) {
        slider.value--
      }
    }
    binding.mbSliderPlus.onClick {
      if (slider.value < slider.valueTo) {
        slider.value++
      }
    }
    makeDirEmpty(OUTPUT_SPLIT_DIR)
    val mlo = mutableListOf<Bitmap>()
    FFmpegKit.execute(
      "${MyConstants.FFMPEG_COMMAND_PREFIX_FOR_ALL_AN} -i ${inputGifUri.createFfSafForRead()} " +
          "$OUTPUT_SPLIT_DIR%05d.png"
    )
    var frameIndex = 1
    while (File("$OUTPUT_SPLIT_DIR${String.format("%05d", frameIndex)}.png").exists()) {
      mlo.add(
        BitmapFactory.decodeFile("$OUTPUT_SPLIT_DIR${String.format("%05d", frameIndex)}.png")
      )
      frameIndex++
    }
    slider.apply {
      valueTo = mlo.size.toFloat()
      setLabelFormatter { "${it.toInt()}/${valueTo.toInt()}" }
      addOnChangeListener { slider, value, _ ->
        slider.performHapticFeedback(HapticFeedbackConstants.TEXT_HANDLE_MOVE)
        aciv.setImageBitmap(mlo[value.toInt() - 1])
      }
    }
    aciv.setImageBitmap(mlo[0])
    binding.mbSave.onClick {
      Toolbox.copyFile(
        "$OUTPUT_SPLIT_DIR${String.format("%05d", slider.value.toInt())}.png",
        Toolbox.createNewFile(inputGifUri, "png")
      )
      toast("截图已保存至相册")
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
    fun start(context: Context, gifUri: Uri) =
      context.startActivity(
        Intent(context, GifSplitActivity::class.java)
          .putExtra(MyConstants.EXTRA_GIF_URI, gifUri)
      )
  }
}