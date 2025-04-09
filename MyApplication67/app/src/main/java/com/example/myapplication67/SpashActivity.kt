package com.example.myapplication67

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

// Splash screen activity that shows the app logo with animation before opening MainActivity
class SplashActivity : AppCompatActivity() {

    // Called when the activity is created
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        Log.d("SplashActivity", "Splash screen started")

        // Get reference to the logo ImageView from layout
        val logoImageView: ImageView = findViewById(R.id.splashLogo)

        // Load animation resource defined in res/anim/logo_animation.xml
        val animation = AnimationUtils.loadAnimation(this, R.anim.logo_animation)

        // Set a listener to detect when the animation starts/ends
        animation.setAnimationListener(object : android.view.animation.Animation.AnimationListener {

            // Called when the animation starts
            override fun onAnimationStart(animation: android.view.animation.Animation?) {
                Log.d("SplashActivity", "Animation started")
            }

            // Called when the animation finishes
            override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                Log.d("SplashActivity", "Animation ended, starting MainActivity")
                try {
                    // Start the main screen of the app
                    startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                    finish() // Close the splash screen
                } catch (e: Exception) {
                    Log.e("SplashActivity", "Failed to start MainActivity", e)
                    finish() // Exit even if failed, to prevent getting stuck
                }
            }

            // Not used, but required to override
            override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
        })

        // Start the animation on the logo
        logoImageView.startAnimation(animation)
    }
}
