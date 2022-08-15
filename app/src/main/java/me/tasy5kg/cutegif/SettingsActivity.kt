package me.tasy5kg.cutegif

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import me.tasy5kg.cutegif.MyConstants.CUSTOM_MENU_ITEM_VIEW_TYPE_GIF_FINAL_DELAY
import me.tasy5kg.cutegif.MyConstants.GIF_FINAL_DELAY_MAP
import me.tasy5kg.cutegif.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {
  private lateinit var binding: ActivitySettingsBinding
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivitySettingsBinding.inflate(layoutInflater)
    setContentView(binding.root)
    setSupportActionBar(binding.materialToolbar)
    setFinishOnTouchOutside(true)
    with(binding.msRememberGifOptions) {
      isChecked = MySettings.rememberGifOptions
      setOnCheckedChangeListener { buttonView, isChecked ->
        MySettings.rememberGifOptions = isChecked
        if (!isChecked) {
          with(MySettings) {
            previousGifConfigSpeed = INT_PREVIOUS_GIF_CONFIG_UNKNOWN_VALUE
            previousGifConfigResolution = INT_PREVIOUS_GIF_CONFIG_UNKNOWN_VALUE
            previousGifConfigFrameRate = INT_PREVIOUS_GIF_CONFIG_UNKNOWN_VALUE
            previousGifConfigColorQuality = INT_PREVIOUS_GIF_CONFIG_UNKNOWN_VALUE
          }
        }
      }
    }
    with(binding.msAnalyzeVideoSlowly) {
      isChecked = MySettings.analyzeVideoSlowly
      setOnCheckedChangeListener { buttonView, isChecked ->
        MySettings.analyzeVideoSlowly = isChecked
      }
    }
    // binding.cmivAnalyzeVideoSlowly.setUpAsIntSetting(MyConstants.ANALYZE_VIDEO_SLOWLY_MAP, MySettings.INT_ANALYZE_VIDEO_SLOWLY, ANALYZE_VIDEO_SLOWLY_CONFIG_DEFAULT)
    binding.cmivFinalDelay.apply {
      setUpWithDropDownConfig(GIF_FINAL_DELAY_MAP, CUSTOM_MENU_ITEM_VIEW_TYPE_GIF_FINAL_DELAY)
      setSelectedValue(MySettings.gifFinalDelay)
    }
    binding.mbDone.setOnClickListener { finish() }
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
    fun start(context: Context) {
      context.startActivity(Intent(context, SettingsActivity::class.java))
    }
  }
}