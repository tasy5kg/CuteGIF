package me.tasy5kg.cutegif

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ContentInfoCompat.SOURCE_DRAG_AND_DROP
import androidx.core.view.OnReceiveContentListener
import androidx.draganddrop.DropHelper
import me.tasy5kg.cutegif.MySettings.INT_FILE_OPEN_WAY_13
import me.tasy5kg.cutegif.MySettings.INT_FILE_OPEN_WAY_DOCUMENT
import me.tasy5kg.cutegif.MySettings.INT_FILE_OPEN_WAY_GALLERY
import me.tasy5kg.cutegif.Toolbox.makeDirEmpty
import me.tasy5kg.cutegif.Toolbox.onClick
import me.tasy5kg.cutegif.databinding.ActivityMainBinding

class MainActivity : BaseActivity() {
  private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

  private val arlImportVideoToGifDocument =
    registerForActivityResult(ActivityResultContracts.GetContent()) {
      it?.let { _ ->
        VideoToGifActivity.start(this, it)
      }
    }

  private val arlImportVideoToGifElse =
    registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
      it?.data?.data?.let {
          uri,
        ->
        VideoToGifActivity.start(this, uri)
      }
    }

  private val arlImportGifSplitDocument =
    registerForActivityResult(ActivityResultContracts.GetContent()) {
      it?.let {
          _,
        ->
        GifSplitActivity.start(this, it)
      }
    }

  private val arlImportGifSplit13 =
    registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
      it?.data?.data?.let { uri ->
        GifSplitActivity.start(this, uri)
      }
    }

  private val arlImportGifToVideoDocument =
    registerForActivityResult(ActivityResultContracts.GetContent()) {
      it?.let { _ ->
        GifToVideoActivity.start(this, it)
      }
    }

  private val arlImportGifToVideo13 =
    registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
      it?.data?.data?.let { uri ->
        GifToVideoActivity.start(this, uri)
      }
    }

  @SuppressLint("InlinedApi")
  override fun onCreateIfEulaAccepted(savedInstanceState: Bundle?) {
    setContentView(binding.root)
    setSupportActionBar(binding.materialToolbar)
    binding.mcvVideoToGif.onClick { importVideoToGif() }
    binding.mcvGifSplit.onClick { importForGifSplit() }
    binding.mcvGifToVideo.onClick { importForGifToVideo() }
    binding.mcvDonate.onClick { DonateActivity.start(this@MainActivity) }
    DropHelper.configureView(this, binding.mcvVideoToGif, arrayOf("video/*"), OnReceiveContentListener { _, payload ->
      try {
        if (payload.source == SOURCE_DRAG_AND_DROP &&
          payload.clip.itemCount == 1 &&
          payload.clip.description.getMimeType(0).run {
            startsWith("video/")
          }
        ) {
          VideoToGifActivity.start(this, payload.clip.getItemAt(0).uri)
          return@OnReceiveContentListener null
        }
      } catch (_: Exception) {
      }
      return@OnReceiveContentListener payload
    })
    DropHelper.configureView(this, binding.mcvGifSplit, arrayOf("image/gif"), OnReceiveContentListener { _, payload ->
      try {
        if (payload.source == SOURCE_DRAG_AND_DROP &&
          payload.clip.itemCount == 1 &&
          payload.clip.description.getMimeType(0).run {
            equals("image/gif")
          }
        ) {
          GifSplitActivity.start(this, payload.clip.getItemAt(0).uri)
          return@OnReceiveContentListener null
        }
      } catch (_: Exception) {
      }
      return@OnReceiveContentListener payload
    })
    DropHelper.configureView(this, binding.mcvGifToVideo, arrayOf("image/gif"), OnReceiveContentListener { _, payload ->
      try {
        if (payload.source == SOURCE_DRAG_AND_DROP &&
          payload.clip.itemCount == 1 &&
          payload.clip.description.getMimeType(0).run {
            equals("image/gif")
          }
        ) {
          GifToVideoActivity.start(this, payload.clip.getItemAt(0).uri)
          return@OnReceiveContentListener null
        }
      } catch (_: Exception) {
      }
      return@OnReceiveContentListener payload
    })
  }

  private fun importVideoToGif() {
    @SuppressLint("InlinedApi")
    when (MySettings.fileOpenWay) {
      INT_FILE_OPEN_WAY_DOCUMENT -> arlImportVideoToGifDocument.launch("video/*")
      INT_FILE_OPEN_WAY_GALLERY -> arlImportVideoToGifElse.launch(
        Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
          .apply {
            type = "video/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
          })

      INT_FILE_OPEN_WAY_13 -> arlImportVideoToGifElse.launch(Intent(MediaStore.ACTION_PICK_IMAGES).apply {
        type = "video/*"
      })
    }
  }

  private fun importForGifSplit(intFileOpenWay: Int = MySettings.fileOpenWay) {
    @SuppressLint("InlinedApi")
    when (intFileOpenWay) {
      INT_FILE_OPEN_WAY_DOCUMENT -> arlImportGifSplitDocument.launch("image/gif")
      INT_FILE_OPEN_WAY_13 -> arlImportGifSplit13.launch(Intent(MediaStore.ACTION_PICK_IMAGES).apply {
        type = "image/gif"
      })

      else -> importForGifSplit(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) INT_FILE_OPEN_WAY_13 else INT_FILE_OPEN_WAY_DOCUMENT
      )
    }
  }

  private fun importForGifToVideo(intFileOpenWay: Int = MySettings.fileOpenWay) {
    @SuppressLint("InlinedApi")
    when (intFileOpenWay) {
      INT_FILE_OPEN_WAY_DOCUMENT -> arlImportGifToVideoDocument.launch("image/gif")
      INT_FILE_OPEN_WAY_13 -> arlImportGifToVideo13.launch(Intent(MediaStore.ACTION_PICK_IMAGES).apply {
        type = "image/gif"
      })

      else -> importForGifToVideo(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) INT_FILE_OPEN_WAY_13 else INT_FILE_OPEN_WAY_DOCUMENT
      )
    }
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    menuInflater.inflate(R.menu.toolbar_main, menu)
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      R.id.menu_item_about -> AboutActivity.start(this)
    }
    return true
  }

  override fun onDestroy() {
    super.onDestroy()
    makeDirEmpty(MyConstants.VIDEO_TO_GIF_VIDEO_FALLBACK_DIR)
  }

  companion object {
    fun start(context: Context) {
      context.startActivity(Intent(context, MainActivity::class.java))
    }
  }
}