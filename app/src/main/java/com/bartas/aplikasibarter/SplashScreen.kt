package com.bartas.aplikasibarter

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.WindowInsets
import androidx.appcompat.app.AppCompatActivity

@SuppressLint("CustomSplashScreen")
class SplashScreen : AppCompatActivity() {
    private val splashDelay: Long = 2000
    private val prefName = "MyAppPrefs"
    private val isFirstRunKey = "isFirstRun"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        val preferences = getSharedPreferences(prefName, MODE_PRIVATE)
        val isFirstRun = preferences.getBoolean(isFirstRunKey, true)

        Handler().postDelayed({
            if (isFirstRun) {
                preferences.edit().putBoolean(isFirstRunKey, false).apply()
                val onboardingIntent = Intent(this@SplashScreen, OnboardingActivity::class.java)
                startActivity(onboardingIntent)
            } else {
                val loginIntent = Intent(this@SplashScreen, LoginActivity::class.java)
                startActivity(loginIntent)
            }
            finish()
        }, splashDelay)

        // Hide the status bar on Android 12 and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            window.insetsController?.hide(WindowInsets.Type.systemBars())
        } else {
            // For versions below Android 12, use the old method
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        }
    }
}
