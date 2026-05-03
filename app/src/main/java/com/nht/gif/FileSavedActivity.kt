package com.nht.gif

import android.content.Context
import android.content.Intent
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.View.GONE
import android.view.View.VISIBLE
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.nht.gif.MyConstants.EXTRA_SAVED_FILE_URI
import com.nht.gif.MyConstants.MIME_TYPE_IMAGE_GIF
import com.nht.gif.MyConstants.MIME_TYPE_IMAGE_WEBP
import com.nht.gif.MyConstants.MIME_TYPE_VIDEO_MP4
import com.nht.gif.databinding.ActivityFileSavedBinding
import com.nht.gif.toolbox.FileTools
import com.nht.gif.toolbox.FileTools.deleteFile
import com.nht.gif.toolbox.FileTools.fileSize
import com.nht.gif.toolbox.FileTools.formattedFileSize
import com.nht.gif.toolbox.FileTools.mimeType
import com.nht.gif.toolbox.Toolbox.getExtra
import com.nht.gif.toolbox.Toolbox.onClick
import com.nht.gif.toolbox.Toolbox.toast
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class FileSavedActivity : BaseActivity() {
  private val binding by lazy { ActivityFileSavedBinding.inflate(layoutInflater) }
  private val fileUri by lazy { intent.getExtra<Uri>(EXTRA_SAVED_FILE_URI) }

  override fun onCreateIfEulaAccepted(savedInstanceState: Bundle?) {
    setContentView(binding.root)
    setFinishOnTouchOutside(false)
    binding.mtvXxxSavedToGallery.text = getString(R.string._ext__saved_to_gallery, FileTools.FileName(fileUri).extension.uppercase(Locale.ROOT))
    when (fileUri.mimeType()) {
      MIME_TYPE_IMAGE_GIF, MIME_TYPE_IMAGE_WEBP -> {
        binding.acivPreview.visibility = VISIBLE
        binding.vvPreview.visibility = GONE
        if (fileUri.mimeType() == MIME_TYPE_IMAGE_WEBP) {
          lifecycleScope.launch {
            val drawable = withContext(Dispatchers.IO) {
              ImageDecoder.decodeDrawable(ImageDecoder.createSource(contentResolver, fileUri))
            }
            binding.acivPreview.setImageDrawable(drawable)
            (drawable as? AnimatedImageDrawable)?.start()
          }
        } else {
          Glide.with(this).load(fileUri).fitCenter().diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true)
            .transition(DrawableTransitionOptions.withCrossFade()).into(binding.acivPreview)
        }
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
      toast(R.string.file_deleted)
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