package me.tasy5kg.cutegif

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.View.GONE
import android.view.View.VISIBLE
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import me.tasy5kg.cutegif.MyConstants.EXTRA_SAVED_FILE_URI
import me.tasy5kg.cutegif.MyConstants.MIME_TYPE_IMAGE_GIF
import me.tasy5kg.cutegif.MyConstants.MIME_TYPE_VIDEO_MP4
import me.tasy5kg.cutegif.databinding.ActivityFileSavedBinding
import me.tasy5kg.cutegif.toolbox.FileTools
import me.tasy5kg.cutegif.toolbox.FileTools.deleteFile
import me.tasy5kg.cutegif.toolbox.FileTools.fileSize
import me.tasy5kg.cutegif.toolbox.FileTools.formattedFileSize
import me.tasy5kg.cutegif.toolbox.FileTools.mimeType
import me.tasy5kg.cutegif.toolbox.Toolbox.onClick
import me.tasy5kg.cutegif.toolbox.Toolbox.toast
import java.util.Locale

class FileSavedActivity : BaseActivity() {
  private val binding by lazy { ActivityFileSavedBinding.inflate(layoutInflater) }
  private val fileUri by lazy { intent.extras!!.get(EXTRA_SAVED_FILE_URI) as Uri }

  override fun onCreateIfEulaAccepted(savedInstanceState: Bundle?) {
    setContentView(binding.root)
    setFinishOnTouchOutside(false)
    binding.mtvXxxSavedToGallery.text = getString(R.string._ext__saved_to_gallery, FileTools.FileName(fileUri).extension.uppercase(Locale.ROOT))
    when (fileUri.mimeType()) {
      MIME_TYPE_IMAGE_GIF -> {
        binding.acivPreview.visibility = VISIBLE
        binding.vvPreview.visibility = GONE
        Glide.with(this).load(fileUri).fitCenter().diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true)
          .transition(DrawableTransitionOptions.withCrossFade()).into(binding.acivPreview)
      }

      MIME_TYPE_VIDEO_MP4 -> {
        binding.acivPreview.visibility = GONE
        binding.vvPreview.apply {
          visibility = VISIBLE
          setAudioFocusRequest(AudioManager.AUDIOFOCUS_NONE)
          setOnPreparedListener {
            it.apply {
              setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT)
              isLooping = true
            }
          }
          setVideoURI(fileUri)
          start()
        }
      }

      else -> {
        throw NotImplementedError()
      }
    }
    binding.mtvFileSize.text = getString(R.string.file_size_s, fileUri.fileSize().formattedFileSize())
    binding.mbDone.onClick { finish() }
    binding.mbBack.onClick { finish() }
    binding.mbDelete.onClick {
      fileUri.deleteFile()
      toast(context.getString(R.string.file_deleted))
      finish()
    }
    binding.mbShare.onClick {
      startActivity(
        Intent.createChooser(
          Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, fileUri)
            type = fileUri.mimeType()
          }, null
        )
      )
    }
  }

  override fun onPause() {
    super.onPause()
    if (binding.vvPreview.visibility == VISIBLE) binding.vvPreview.pause()
  }

  override fun onResume() {
    super.onResume()
    if (binding.vvPreview.visibility == VISIBLE) binding.vvPreview.start()
  }

  companion object {
    fun start(context: Context, fileUri: Uri) {
      context.startActivity(
        Intent(context, FileSavedActivity::class.java).putExtra(EXTRA_SAVED_FILE_URI, fileUri)
      )
    }
  }
}