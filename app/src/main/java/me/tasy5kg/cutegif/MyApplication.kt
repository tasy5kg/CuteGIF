package me.tasy5kg.cutegif

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context

class MyApplication : Application() {
  override fun onCreate() {
    super.onCreate()
    appContext = applicationContext
  }

  companion object {
    @SuppressLint("StaticFieldLeak")
    lateinit var appContext: Context
  }
}
