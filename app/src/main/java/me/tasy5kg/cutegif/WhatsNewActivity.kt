package me.tasy5kg.cutegif

import android.content.Context
import android.content.Intent
import android.os.Bundle
import me.tasy5kg.cutegif.databinding.ActivityWhatsNewBinding
import me.tasy5kg.cutegif.toolbox.Toolbox.onClick

class WhatsNewActivity : BaseActivity() {
  private val binding by lazy { ActivityWhatsNewBinding.inflate(layoutInflater) }

  override fun onCreateIfEulaAccepted(savedInstanceState: Bundle?) {
    setContentView(binding.root)
    binding.mtvVersionInfo.text = getString(R.string.version_X, BuildConfig.VERSION_NAME)
    binding.mbGotIt.onClick(HapticFeedbackType.CONFIRM) {
      MySettings.whatsNewRead = true
      finish()
    }
  }

  companion object {
    fun start(context: Context) {
      context.startActivity(Intent(context, WhatsNewActivity::class.java))
    }
  }
}