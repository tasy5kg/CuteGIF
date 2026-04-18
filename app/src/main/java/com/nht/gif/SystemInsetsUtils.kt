package com.nht.gif

import android.R
import android.content.Context
import android.view.Window
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowCompat.enableEdgeToEdge
import androidx.core.view.WindowInsetsCompat

object SystemInsetsUtils {

    fun adjustSystemBarsInsets(window: Window) {
        enableEdgeToEdge(window)
        // Apply window insets to avoid overlapping with system bars
        ViewCompat.setOnApplyWindowInsetsListener(
            window.decorView.findViewById(R.id.content)
        ) { v, insets ->
            val systemBarsInsets =
                insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                v.paddingLeft,
                systemBarsInsets.top,
                v.paddingRight,
                systemBarsInsets.bottom
            )
            // Keep propagating insets so downstream listeners (e.g. in the Activity layout) run
            insets
        }
    }

    fun setLightSystemBars(window: Window, context: Context) {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            this.isAppearanceLightStatusBars = false
            this.isAppearanceLightNavigationBars = false
        }
    }
}
