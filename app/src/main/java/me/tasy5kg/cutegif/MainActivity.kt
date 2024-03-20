package me.tasy5kg.cutegif

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import me.tasy5kg.cutegif.MySettings.INT_FILE_OPEN_WAY_13
import me.tasy5kg.cutegif.MySettings.INT_FILE_OPEN_WAY_DOCUMENT
import me.tasy5kg.cutegif.MySettings.INT_FILE_OPEN_WAY_GALLERY
import me.tasy5kg.cutegif.databinding.ActivityMainBinding
import me.tasy5kg.cutegif.toolbox.FileTools.copyToInputFileDir
import me.tasy5kg.cutegif.toolbox.Toolbox.enableDropFile
import me.tasy5kg.cutegif.toolbox.Toolbox.onClick

class MainActivity : BaseActivity() {
  private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

  private val arlImportVideoToGifDocument = registerForActivityResult(ActivityResultContracts.GetContent()) {
    it?.let { _ -> VideoToGifActivity.start(this, it.copyToInputFileDir()) }
  }
  private val arlImportVideoToGifElse = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
    it?.data?.data?.let { uri -> VideoToGifActivity.start(this, uri.copyToInputFileDir()) }
  }
  private val arlImportGifSplitDocument = registerForActivityResult(ActivityResultContracts.GetContent()) {
    it?.let { _ -> GifSplitActivity.start(this, it.copyToInputFileDir()) }
  }
  private val arlImportGifSplit13 = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
    it?.data?.data?.let { uri -> GifSplitActivity.start(this, uri.copyToInputFileDir()) }
  }
  private val arlImportGifToVideoDocument = registerForActivityResult(ActivityResultContracts.GetContent()) {
    it?.let { _ -> GifToVideoActivity.start(this, it.copyToInputFileDir()) }
  }
  private val arlImportGifToVideo13 = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
    it?.data?.data?.let { uri -> GifToVideoActivity.start(this, uri.copyToInputFileDir()) }
  }
  private val arlImportMvimgToGifDocument = registerForActivityResult(ActivityResultContracts.GetContent()) {
    it?.let { _ -> ImportMvimgActivity.start(this, it.copyToInputFileDir()) }
  }
  private val arlImportMvimgToGif13 = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
    it?.data?.data?.let { uri -> ImportMvimgActivity.start(this, uri.copyToInputFileDir()) }
  }

  override fun onCreateIfEulaAccepted(savedInstanceState: Bundle?) {
    setContentView(binding.root)
    setSupportActionBar(binding.materialToolbar)
    binding.materialToolbar.subtitle = getString(R.string.version_X, BuildConfig.VERSION_NAME)
    binding.mtvBetaInfo.text = getString(R.string.beta_info, BetaEndedActivity.testVersionRemainingDays())
    binding.mcvVideoToGif.apply {
      onClick { importVideoToGif() }
      enableDropFile(this@MainActivity, "video/*") {
        VideoToGifActivity.start(
          this@MainActivity, it.copyToInputFileDir()
        )
      }
    }
    binding.mcvGifSplit.apply {
      onClick { importForGifSplit() }
      enableDropFile(this@MainActivity, "image/gif") {
        GifSplitActivity.start(
          this@MainActivity, it.copyToInputFileDir()
        )
      }
    }
    binding.mcvGifToVideo.apply {
      onClick { importForGifToVideo() }
      enableDropFile(this@MainActivity, "image/gif") {
        GifToVideoActivity.start(
          this@MainActivity, it.copyToInputFileDir()
        )
      }
    }
    binding.mcvMvimgToVideo.apply {
      onClick { importForMvimgToGif() }
      enableDropFile(this@MainActivity, "image/jpeg") {
        ImportMvimgActivity.start(
          this@MainActivity, it.copyToInputFileDir()
        )
      }
    }

    @Suppress("DEPRECATION")
    val uriFromActionViewOrSend = intent?.extras?.getParcelable(Intent.EXTRA_STREAM) ?: intent.data
    if (uriFromActionViewOrSend != null) {
      VideoToGifActivity.start(this, uriFromActionViewOrSend.copyToInputFileDir())
    }
    if (!MySettings.whatsNewRead) WhatsNewActivity.start(this)
  }

  private fun importVideoToGif() {
    when (MySettings.fileOpenWay) {
      INT_FILE_OPEN_WAY_DOCUMENT -> arlImportVideoToGifDocument.launch("video/*")
      INT_FILE_OPEN_WAY_GALLERY -> arlImportVideoToGifElse.launch(Intent(
        Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI
      ).apply {
        type = "video/*"
      })

      INT_FILE_OPEN_WAY_13 -> arlImportVideoToGifElse.launch(Intent(MediaStore.ACTION_PICK_IMAGES).apply {
        type = "video/*"
      })
    }
  }

  private fun importForGifSplit(intFileOpenWay: Int = MySettings.fileOpenWay) {
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

  private fun importForMvimgToGif(intFileOpenWay: Int = MySettings.fileOpenWay) {
    when (intFileOpenWay) {
      INT_FILE_OPEN_WAY_DOCUMENT -> arlImportMvimgToGifDocument.launch("image/jpeg")
      INT_FILE_OPEN_WAY_13 -> arlImportMvimgToGif13.launch(Intent(MediaStore.ACTION_PICK_IMAGES).apply {
        type = "image/jpeg"
      })

      else -> importForMvimgToGif(
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

  companion object {
    fun start(context: Context) {
      context.startActivity(Intent(context, MainActivity::class.java))
    }
  }
}