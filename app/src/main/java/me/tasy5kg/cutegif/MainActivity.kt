package me.tasy5kg.cutegif

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import me.tasy5kg.cutegif.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
  private lateinit var binding: ActivityMainBinding
  private val import = registerForActivityResult(
    ActivityResultContracts.GetContent()
  ) {
    it?.let {
      GifActivity.start(this, it)
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)
    setSupportActionBar(binding.materialToolbar)
    binding.llcSelectAVideo.setOnClickListener {
      import.launch("video/*")
    }
    if (MySettings.firstStartCurrentVersion()) {
      AboutActivity.start(this@MainActivity)
    }
    /*
    binding.llcGuideShortcut.setOnClickListener {

    }
     */
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    menuInflater.inflate(R.menu.toolbar_main, menu)
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      R.id.menu_item_settings -> SettingsActivity.start(this@MainActivity)
      R.id.menu_item_about -> AboutActivity.start(this@MainActivity)
    }
    return true
  }
}