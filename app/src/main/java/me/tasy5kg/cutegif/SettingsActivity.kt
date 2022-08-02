package me.tasy5kg.cutegif

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import me.tasy5kg.cutegif.MyConstants.REMEMBER_GIF_CONFIG_DEFAULT
import me.tasy5kg.cutegif.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {
  private lateinit var binding: ActivitySettingsBinding
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivitySettingsBinding.inflate(layoutInflater)
    setContentView(binding.root)
    setSupportActionBar(binding.materialToolbar)
    setFinishOnTouchOutside(true)
    binding.cmivRememberGifOptions.setUpAsIntSetting(MyConstants.REMEMBER_GIF_CONFIG_MAP, MySettings.INT_REMEMBER_GIF_CONFIG, REMEMBER_GIF_CONFIG_DEFAULT)
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