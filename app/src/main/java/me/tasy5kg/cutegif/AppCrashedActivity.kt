package me.tasy5kg.cutegif

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.arthenica.ffmpegkit.FFmpegKit
import me.tasy5kg.cutegif.MyApplication.Companion.appContext
import me.tasy5kg.cutegif.MyConstants.EXTRA_STACK_TRACE_STRING
import me.tasy5kg.cutegif.Toolbox.onClick
import me.tasy5kg.cutegif.databinding.ActivityAppCrashedBinding

class AppCrashedActivity : AppCompatActivity() {
  private val binding by lazy { ActivityAppCrashedBinding.inflate(layoutInflater) }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(binding.root)
    setSupportActionBar(binding.materialToolbar )
    setFinishOnTouchOutside(false)
    val stackTraceString = intent.extras!!.getString(EXTRA_STACK_TRACE_STRING)!!
    val problemLog =
      "[Exception Info]\n${stackTraceString}\n" +
          "[System Info]\n${systemInfo()}\n" +
          "[Application Info]\n${applicationInfo()}"
    binding.mtvProblemLog.text = problemLog
    binding.mbCopy.onClick {
      Toolbox.copyTextToClipboard(problemLog, "已复制到剪贴板")

    }
    binding.mbExit.setOnClickListener {
      finish()
    }
    //todo binding.cmivJoinQqGroup.setUpWithLambda { Toolbox.cmivJoinQqGroupLambda(this@AppCrashedActivity) }
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
    fun start(context: Context, stackTraceString: String) =
      context.startActivity(
        Intent(context, AppCrashedActivity::class.java)
          .putExtra(EXTRA_STACK_TRACE_STRING, stackTraceString)
      )

    private fun getMemInfo(): ActivityManager.MemoryInfo {
      val memInfo = ActivityManager.MemoryInfo()
      appContext.getSystemService(ActivityManager::class.java).getMemoryInfo(memInfo)
      return memInfo
    }

    private fun systemInfo() =
      "Android SDK Version = ${Build.VERSION.SDK_INT}\n" +
          "Supported ABIs = ${Build.SUPPORTED_ABIS.joinToString(separator = ",")}\n" +
          "Manufacturer = ${Build.MANUFACTURER}\n" +
          "Brand = ${Build.BRAND}\n" +
          "Model = ${Build.MODEL}\n" +
          "Languages = ${appContext.resources.configuration.locales.toLanguageTags()}\n" +
          "Current Timestamp = ${System.currentTimeMillis()}\n" +
          "Total Memory = ${getMemInfo().totalMem}\n" +
          "Available Memory = ${getMemInfo().availMem}\n"

    private fun applicationInfo() =
      "Application ID = ${BuildConfig.APPLICATION_ID}\n" +
          "Version Code = ${BuildConfig.VERSION_CODE}\n" +
          "Version Name = ${BuildConfig.VERSION_NAME}\n" +
          "Build Type = ${BuildConfig.BUILD_TYPE}\n" +
          "Debug = ${BuildConfig.DEBUG}\n"

    private fun ffmpegInfo() = FFmpegKit.execute("-version").allLogsAsString
  }
}