package me.tasy5kg.cutegif

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.view.View.GONE
import android.view.View.VISIBLE
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import me.tasy5kg.cutegif.MyConstants.FFMPEG_COMMAND_PREFIX_FOR_ALL
import me.tasy5kg.cutegif.MyConstants.FFMPEG_COMMAND_PREFIX_FOR_ALL_AN
import me.tasy5kg.cutegif.MyConstants.VIDSTABDETECT_RESULT_PATH
import me.tasy5kg.cutegif.databinding.ActivityImportMvimgBinding
import me.tasy5kg.cutegif.toolbox.FileTools
import me.tasy5kg.cutegif.toolbox.MediaTools
import me.tasy5kg.cutegif.toolbox.Toolbox.getExtra
import me.tasy5kg.cutegif.toolbox.Toolbox.logRed
import me.tasy5kg.cutegif.toolbox.Toolbox.onClick
import me.tasy5kg.cutegif.toolbox.Toolbox.toast

class ImportMvimgActivity : BaseActivity() {
  private val binding by lazy { ActivityImportMvimgBinding.inflate(layoutInflater) }
  private val inputMvimgPath by lazy { intent.getExtra<String>(MyConstants.EXTRA_MVIMG_PATH) }
  private val extractedVideoPath by lazy { "$inputMvimgPath.mp4" }
  private val extractedVideoStabilizedPath by lazy { "${inputMvimgPath}_stabilized.mp4" }
  private var stabilizationCompleted = false
  private var taskQuit = false

  override fun onCreateIfEulaAccepted(savedInstanceState: Bundle?) {
    setContentView(binding.root)
    setFinishOnTouchOutside(false)
    binding.mbCancel.onClick { stablizeVideoCancel() }
    binding.mbBack.onClick { stablizeVideoCancel() }
    val extractSuccess = MediaTools.extractVideoFromMvimg(inputMvimgPath, extractedVideoPath)
    if (!extractSuccess) {
      toast(getString(R.string.seems_not_a_mvimg))
      finish()
      return
    }
    binding.vvPreview.apply {
      setOnPreparedListener {
        it.apply {
          setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT)
          isLooping = true
        }
      }
      setVideoPath(extractedVideoPath)
      start()
    }
    binding.vvPreviewStabilized.apply {
      setOnPreparedListener {
        it.apply {
          setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT)
          isLooping = true
          setVolume(0f, 0f)
        }
      }
    }
    binding.mbImport.onClick(HapticFeedbackType.CONFIRM) {
      VideoToGifActivity.start(
        this@ImportMvimgActivity,
        if (binding.mcbStabilization.isChecked) extractedVideoStabilizedPath else extractedVideoPath
      )
      finish()
    }
    binding.mcbStabilization.setOnCheckedChangeListener { buttonView, isChecked ->
      if (isChecked) {
        if (stabilizationCompleted) {
          binding.vvPreviewStabilized.visibility = VISIBLE
          binding.vvPreview.start()
          binding.vvPreviewStabilized.start()
        } else {
          buttonView.isEnabled = false
          buttonView.text = getString(R.string.enabling_stabilization)
          binding.mbImport.isEnabled = false
          stablizeVideoStep1()
        }
      } else {
        binding.vvPreviewStabilized.visibility = GONE
      }
    }
    binding.mbSaveAsVideo.onClick {
      val outputUri = FileTools.createNewFile(FileTools.FileName(extractedVideoPath).nameWithoutExtension, "mp4")
      FileTools.copyFile(extractedVideoPath, outputUri, false)
      isEnabled = false
      setTextColor(getColor(R.color.grey))
      text = getString(R.string.video_saved_to_gallery)
    }
  }

  private fun stablizeVideoStep1() {
    val command1 =
      "$FFMPEG_COMMAND_PREFIX_FOR_ALL_AN -i $extractedVideoPath -vf vidstabdetect=shakiness=10:accuracy=15:tripod=1:result=\"$VIDSTABDETECT_RESULT_PATH\" -f null -"
    logRed("stablizeCmd1", command1)
    FFmpegKit.executeAsync(command1, { completeCallback ->
      when {
        completeCallback.returnCode.isValueSuccess -> stablizeVideoStep2()
        completeCallback.returnCode.isValueError -> stablizeVideoError()
      }
    }, { logCallback ->
      logRed("logcallback", logCallback.message.toString())
    }, { _ -> })
  }

  private fun stablizeVideoStep2() {
    if (!taskQuit) {
      val command2 =
        "$FFMPEG_COMMAND_PREFIX_FOR_ALL -i $extractedVideoPath -c:a copy -c:v libx264 -crf 17 -preset:v veryfast -vf vidstabtransform=input=\"$VIDSTABDETECT_RESULT_PATH\":tripod=1 -y $extractedVideoStabilizedPath"
      logRed("stablizeCmd2", command2)
      FFmpegKit.executeAsync(command2, { completeCallback ->
        when {
          completeCallback.returnCode.isValueSuccess -> stablizeVideoSuccess()
          completeCallback.returnCode.isValueError -> stablizeVideoError()
        }
      }, { logCallback ->
        logRed("logcallback", logCallback.message.toString())
      }, { _ -> })
    }
  }

  private fun stablizeVideoCancel() {
    runOnUiThread {
      taskQuit = true
      FFmpegKit.cancel()
      FFmpegKitConfig.clearSessions()
      finish()
    }
  }

  private fun stablizeVideoError() {
    runOnUiThread {
      binding.mcbStabilization.apply {
        isEnabled = true
        isChecked = false
        text = context.getString(R.string.failed_to_enable_stabilization)
      }
      binding.mbImport.isEnabled = true
    }
  }

  private fun stablizeVideoSuccess() {
    runOnUiThread {
      binding.mcbStabilization.apply {
        isEnabled = true
        text = getText(R.string.stabilization)
      }
      binding.mbImport.isEnabled = true
      binding.vvPreviewStabilized.apply {
        visibility = VISIBLE
        setVideoPath(extractedVideoStabilizedPath)
        start()
      }
      stabilizationCompleted = true
      binding.vvPreview.start()
    }
  }

  override fun onPause() {
    super.onPause()
    binding.vvPreview.pause()
    binding.vvPreviewStabilized.pause()
  }

  override fun onResume() {
    super.onResume()
    binding.vvPreview.start()
    binding.vvPreviewStabilized.start()
  }

  companion object {
    fun start(context: Context, mvimgPath: String) =
      context.startActivity(Intent(context, ImportMvimgActivity::class.java).putExtra(MyConstants.EXTRA_MVIMG_PATH, mvimgPath))

  }
}