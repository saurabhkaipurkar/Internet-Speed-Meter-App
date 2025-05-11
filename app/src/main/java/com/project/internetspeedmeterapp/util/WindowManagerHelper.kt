package com.project.internetspeedmeterapp.util

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.util.DisplayMetrics
import android.view.*
import androidx.annotation.RequiresApi

object WindowManagerHelper {

    fun canCreateOverlay(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

    @RequiresApi(Build.VERSION_CODES.O)
    fun addFloatingView(floatingView: View, windowManager: WindowManager) {
        val params = createLayoutParams()
        windowManager.addView(floatingView, params)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createLayoutParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 10
            y = 10
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun setupTouchListener(floatingView: View, windowManager: WindowManager) {
        floatingView.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                val params = floatingView.layoutParams as WindowManager.LayoutParams
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val deltaX = (event.rawX - initialTouchX).toInt()
                        val deltaY = (event.rawY - initialTouchY).toInt()
                        params.x = clamp(initialX + deltaX, getScreenWidth(floatingView.context) - floatingView.width)
                        params.y = clamp(initialY + deltaY, getScreenHeight(floatingView.context) - floatingView.height)
                        windowManager.updateViewLayout(floatingView, params)
                        return true
                    }
                }
                return false
            }
        })
    }

    private fun clamp(value: Int, max: Int) = value.coerceIn(0, max)

    private fun getScreenWidth(context: Context): Int {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds: Rect = windowManager.currentWindowMetrics.bounds
            bounds.width()
        } else {
            val metrics = DisplayMetrics()
            windowManager.defaultDisplay?.getRealMetrics(metrics)
            metrics.widthPixels
        }
    }

    private fun getScreenHeight(context: Context): Int {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds: Rect = windowManager.currentWindowMetrics.bounds
            bounds.height()
        } else {
            val metrics = DisplayMetrics()
            windowManager.defaultDisplay?.getRealMetrics(metrics)
            metrics.heightPixels
        }
    }
}
