package me.tasy5kg.cutegif

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import me.tasy5kg.cutegif.databinding.ActivityEulaBinding
import me.tasy5kg.cutegif.toolbox.Toolbox.onClick
import me.tasy5kg.cutegif.toolbox.Toolbox.setupTextViewWithClickablePart
import me.tasy5kg.cutegif.toolbox.Toolbox.toast

class EulaActivity : AppCompatActivity() {
  private val binding by lazy { ActivityEulaBinding.inflate(layoutInflater) }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setFinishOnTouchOutside(false)
    setContentView(binding.root)
    onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
      override fun handleOnBackPressed() {
        // Disable back key to force the user to choose to agree or disagree the EULA
      }
    })
    binding.mtvVersionInfo.text = getString(R.string.version_X, BuildConfig.VERSION_NAME)
    binding.mtvUserAgreementAndPrivacyPolicyGlance.movementMethod = ScrollingMovementMethod()
    binding.mtvUserAgreementAndPrivacyPolicyGlance.setupTextViewWithClickablePart(
      R.string.eula_glance_text,
      listOf(
        Pair(R.string._user_agreement_) {
          toast("显示用户协议...") // TODO
        },
        Pair(R.string._privacy_policy_) {
          toast("显示隐私政策...") // TODO
        })
    )
    binding.mbDisagree.onClick {
      finish()
    }
    binding.mbAgree.setOnClickListener {
      MySettings.setEulaLicenseAcceptedLatestToTrue()
      MainActivity.start(this@EulaActivity)
      finish()
    }
  }

  companion object {
    fun start(context: Context) =
      context.startActivity(Intent(context, EulaActivity::class.java))
  }
}