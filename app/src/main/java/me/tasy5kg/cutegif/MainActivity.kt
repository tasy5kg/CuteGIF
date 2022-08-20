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
    thread {
      try {
        val hu60Response = OkHttpClient().newCall(Request
          .Builder()
          .url("https://hu60.cn/q.php/bbs.topic.103545.24.json?pageSize=1")
          .build()).execute()
        val responseString = hu60Response.body!!.string()
        val latestVersionCode = JSONObject(responseString).getJSONArray("tContents").getJSONObject(0).getString("content").toInt()
        MyToolbox.logging("CheckLatestVersion", "successfully")
        MyToolbox.logging("CurrentVersionCode", BuildConfig.VERSION_CODE.toString())
        MyToolbox.logging("LatestVersionCode", latestVersionCode.toString())
        MyToolbox.logging("IsLatestVersion", (BuildConfig.VERSION_CODE.toString()==latestVersionCode.toString()).toString())
      } catch (e: Exception) {
        e.printStackTrace()
        MyToolbox.logging("CheckLatestVersion", "failed")
      }
    }
     */
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