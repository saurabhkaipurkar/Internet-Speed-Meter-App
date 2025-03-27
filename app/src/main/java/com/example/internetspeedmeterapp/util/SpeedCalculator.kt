package com.example.internetspeedmeterapp.util

import android.annotation.SuppressLint
import android.net.TrafficStats
import kotlin.math.max

object SpeedCalculator {

    fun getTotalRxBytes(): Long = TrafficStats.getTotalRxBytes()

    fun getTotalTxBytes(): Long = TrafficStats.getTotalTxBytes()

    fun calculateSpeed(currentBytes: Long, previousBytes: Long, currentTime: Long, previousTime: Long): Long {
        val elapsedTime = max(1, currentTime - previousTime) // Prevent division by zero
        return max(0, (currentBytes - previousBytes) * 1000 / elapsedTime) // Prevent negative values
    }

    @SuppressLint("DefaultLocale")
    fun formatSpeed(bytesPerSecond: Long): String {
        return when {
            bytesPerSecond >= 1024 * 1024 -> String.format("%.2f MB/s", bytesPerSecond / (1024.0 * 1024.0))
            bytesPerSecond >= 1024 -> String.format("%.2f KB/s", bytesPerSecond / 1024.0)
            else -> "$bytesPerSecond B/s"
        }
    }
}
