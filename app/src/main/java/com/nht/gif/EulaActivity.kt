package com.nht.gif

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.nht.gif.databinding.ActivityEulaBinding
import com.nht.gif.toolbox.Toolbox.onClick

class EulaActivity : AppCompatActivity() {
  private val binding by lazy { ActivityEulaBinding.inflate(layoutInflater) }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(binding.root)
    binding.mtvVersionInfo.text = getString(R.string.version_X, BuildConfig.VERSION_NAME)
    if (MySettings.eulaAccepted) {
      binding.mbDisagree.visibility = View.GONE
      binding.mbAgree.text = getString(R.string.close)
      binding.mtvPleaseAccept.text = getString(R.string.you_have_read_agreed_eula_to_withdraw)
      binding.mbAgree.onClick(HapticFeedbackType.CONFIRM) {
        finish()
      }
    } else {
      binding.mbAgree.onClick(HapticFeedbackType.CONFIRM) {
        MySettings.eulaAccepted = true
        MainActivity.start(this@EulaActivity)
        finish()
      }
    }
    binding.mbDisagree.onClick {
      finish()
    }


  }


  companion object {
    fun start(context: Context) = context.startActivity(Intent(context, EulaActivity::class.java))
  }
}