package com.nht.gif

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.nht.gif.databinding.ActivityOpenSourceLicenseBinding
import com.nht.gif.toolbox.Toolbox.onClick

class OpenSourceLicenseActivity : BaseActivity() {
  private val binding by lazy { ActivityOpenSourceLicenseBinding.inflate(layoutInflater) }

  override fun onCreateIfEulaAccepted(savedInstanceState: Bundle?) {
    setContentView(binding.root)
    binding.mbDone.onClick(HapticFeedbackType.CONFIRM) {
      finish()
    }
  }

  companion object {
    fun start(context: Context) {
      context.startActivity(Intent(context, OpenSourceLicenseActivity::class.java))
    }
  }
}