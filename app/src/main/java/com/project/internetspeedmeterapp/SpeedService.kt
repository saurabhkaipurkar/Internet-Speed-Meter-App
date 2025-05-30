package com.project.internetspeedmeterapp

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import com.project.internetspeedmeterapp.util.NotificationHelper
import com.project.internetspeedmeterapp.util.SpeedCalculator
import com.project.internetspeedmeterapp.util.WindowManagerHelper

class SpeedService : Service() {

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val UPDATE_INTERVAL = 1000L
    }

    private lateinit var windowManager: WindowManager
    private var floatingView: View? = null
    private lateinit var speedTextView: TextView
    private val handler = Handler(Looper.getMainLooper())
    private var previousRxBytes: Long = 0
    private var previousTxBytes: Long = 0
    private var previousTime: Long = 0
    private var speedText: String = ""

    override fun onCreate() {
        super.onCreate()

        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Permission to display over other apps is required.", Toast.LENGTH_LONG).show()
            stopSelf()
            return
        }

        initializeFloatingView()
    }

    @SuppressLint("InflateParams", "ClickableViewAccessibility")
    private fun initializeFloatingView() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        // Wrap service context with a theme (replace 'R.style.AppTheme' with your actual app theme)
        val themedContext = android.view.ContextThemeWrapper(this, R.style.Theme_InternetSpeedMeterApp)

        floatingView = LayoutInflater.from(themedContext).inflate(R.layout.floating_speed_view, null).also {
            speedTextView = it.findViewById(R.id.speedTextView)

            // Add this block to animate the card view on touch
            val cardView = it.findViewById<CardView>(R.id.on_click_card)
            cardView.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(100).start()
                    }
                    MotionEvent.ACTION_UP -> {
                        v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                    }
                    MotionEvent.ACTION_CANCEL -> {
                        v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                    }
                }
                true
            }
        }

        if (WindowManagerHelper.canCreateOverlay()) {
            WindowManagerHelper.addFloatingView(floatingView!!, windowManager)
            WindowManagerHelper.setupTouchListener(floatingView!!, windowManager)
            updateSpeed()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, NotificationHelper.createNotification(this, speedText))
        return START_STICKY
    }

    private fun updateSpeed() {
        handler.post(object : Runnable {
            @SuppressLint("SetTextI18n")
            override fun run() {
                val currentRxBytes = SpeedCalculator.getTotalRxBytes()
                val currentTxBytes = SpeedCalculator.getTotalTxBytes()
                val currentTime = System.currentTimeMillis()

                val rxSpeed = SpeedCalculator.calculateSpeed(currentRxBytes, previousRxBytes, currentTime, previousTime)
                val txSpeed = SpeedCalculator.calculateSpeed(currentTxBytes, previousTxBytes, currentTime, previousTime)

                previousRxBytes = currentRxBytes
                previousTxBytes = currentTxBytes
                previousTime = currentTime

                speedText = " ↓ ${SpeedCalculator.formatSpeed(rxSpeed)} | ↑ ${SpeedCalculator.formatSpeed(txSpeed)}"
                startForeground(NOTIFICATION_ID, NotificationHelper.createNotification(this@SpeedService, speedText))
                speedTextView.text = speedText
                handler.postDelayed(this, UPDATE_INTERVAL)
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        cleanUpFloatingView()
    }

    private fun cleanUpFloatingView() {
        floatingView?.let {
            windowManager.removeView(it)
            floatingView = null
        }
        handler.removeCallbacksAndMessages(null)
        stopForeground(STOP_FOREGROUND_REMOVE)

    }

    override fun onBind(intent: Intent?): IBinder? = null
}
