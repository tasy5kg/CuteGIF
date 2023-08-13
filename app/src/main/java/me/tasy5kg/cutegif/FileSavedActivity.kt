package me.tasy5kg.cutegif

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View.GONE
import android.view.View.VISIBLE
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import me.tasy5kg.cutegif.MyConstants.EXTRA_SAVED_FILE_URI
import me.tasy5kg.cutegif.MyConstants.MIME_TYPE_IMAGE_GIF
import me.tasy5kg.cutegif.MyConstants.MIME_TYPE_VIDEO_MP4
import me.tasy5kg.cutegif.Toolbox.deleteFile
import me.tasy5kg.cutegif.Toolbox.fileSize
import me.tasy5kg.cutegif.Toolbox.formatFileSize
import me.tasy5kg.cutegif.Toolbox.mimeType
import me.tasy5kg.cutegif.Toolbox.onClick
import me.tasy5kg.cutegif.Toolbox.toast
import me.tasy5kg.cutegif.databinding.ActivityFileSavedBinding
import java.util.Locale

class FileSavedActivity : BaseActivity() {
  private val binding by lazy { ActivityFileSavedBinding.inflate(layoutInflater) }
  private val fileUri by lazy { intent.extras!!.get(EXTRA_SAVED_FILE_URI) as Uri }

  override fun onCreateIfEulaAccepted(savedInstanceState: Bundle?) {
    setContentView(binding.root)
    setFinishOnTouchOutside(false)
    sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).apply { data = fileUri })
    binding.mtvXxxSavedToGallery.text = fileUri.mimeType()!!.split('/')[1].uppercase(Locale.ROOT) + " 已保存至相册"
    when (fileUri.mimeType()) {
      MIME_TYPE_IMAGE_GIF -> {
        binding.acivPreview.visibility = VISIBLE
        binding.vvPreview.visibility = GONE
        Glide.with(this)
          .load(fileUri)
          .fitCenter()
          .diskCacheStrategy(DiskCacheStrategy.NONE)
          .skipMemoryCache(true)
          .transition(DrawableTransitionOptions.withCrossFade())
          .into(binding.acivPreview)
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
    binding.mtvFileSize.text = "文件大小：${fileUri.fileSize().formatFileSize()}"
    binding.mbDone.onClick { finish() }
    binding.mbBack.onClick { finish() }
    binding.mbDelete.onClick {
      fileUri.deleteFile()
      toast("文件已删除")
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

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    menuInflater.inflate(R.menu.toolbar_close, menu)
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      R.id.menu_item_close -> finish()
    }
    return true
  }

  companion object {
    fun start(context: Context, fileUri: Uri) {
      context.startActivity(
        Intent(context, FileSavedActivity::class.java)
          .putExtra(EXTRA_SAVED_FILE_URI, fileUri)
      )
    }
  }
}