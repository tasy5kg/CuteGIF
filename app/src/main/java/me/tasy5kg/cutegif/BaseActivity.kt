package me.tasy5kg.cutegif

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlin.system.exitProcess

abstract class BaseActivity : AppCompatActivity() {

  abstract fun onCreateIfEulaAccepted(savedInstanceState: Bundle?)

  final override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    if (!MySettings.eulaAccepted) {
      /** Ask user to accept EULA */
      EulaActivity.start(this)
      finish()
    } else if (BetaEndedActivity.testVersionRemainingDays() < 0) {
      BetaEndedActivity.start(this)
      finish()
    } else {
      if (BuildConfig.DEBUG || true) {
        /** Show a dialog with logs when app crashed */
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
          AppCrashedActivity.start(this, e.stackTraceToString())
          exitProcess(1)
        }
      }
      onCreateIfEulaAccepted(savedInstanceState)
    }
  }
}