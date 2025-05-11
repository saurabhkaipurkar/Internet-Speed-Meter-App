package com.project.internetspeedmeterapp.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.*
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.os.Handler
import android.os.Looper
import android.widget.RemoteViews
import com.project.internetspeedmeterapp.MainActivity
import com.project.internetspeedmeterapp.R
import com.project.internetspeedmeterapp.util.NetworkMonitor
import java.text.DecimalFormat
import androidx.core.content.edit
import com.project.internetspeedmeterapp.util.SpeedCalculator

class WidgetProvider : AppWidgetProvider() {

    companion object {
        private val handler = Handler(Looper.getMainLooper())
        private var previousRxBytes: Long = 0
        private var previousTxBytes: Long = 0
        private var previousTime: Long = 0
        private const val PREF_NAME = "WidgetPrefs"

        fun getNetworkType(context: Context): String {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
                ?: return "No Connection"

            val activeNetwork = connectivityManager.activeNetwork ?: return "No Connection"
            val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return "No Connection"

            return when {
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile Data"
                else -> "Unknown Network"
            }
        }

        // 🔴 Moved `updateAppWidget` inside the same companion object
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_internet_speed).apply {
                setTextViewText(R.id.widget_network_type, "Network: ${getNetworkType(context)}")

                val intent = Intent(context, MainActivity::class.java)
                val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
                setOnClickPendingIntent(R.id.widget_root_layout, pendingIntent)
            }
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    private var networkMonitor: NetworkMonitor? = null

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        loadTrafficData(context)
        appWidgetIds.forEach { updateAppWidget(context, appWidgetManager, it) }
        startUpdatingSpeed(context, appWidgetManager)
    }

    private fun startUpdatingSpeed(context: Context, appWidgetManager: AppWidgetManager) {
        handler.removeCallbacksAndMessages(null) // Clear previous handler callbacks

        val updateSpeedRunnable = object : Runnable {
            override fun run() {
                val currentRxBytes = SpeedCalculator.getTotalRxBytes()
                val currentTxBytes = SpeedCalculator.getTotalTxBytes()
                val currentTime = System.currentTimeMillis()

                val rxSpeed = SpeedCalculator.calculateSpeed(currentRxBytes, previousRxBytes, currentTime, previousTime)
                val txSpeed = SpeedCalculator.calculateSpeed(currentTxBytes, previousTxBytes, currentTime, previousTime)

                previousRxBytes = currentRxBytes
                previousTxBytes = currentTxBytes
                previousTime = currentTime

                val views = RemoteViews(context.packageName, R.layout.widget_internet_speed).apply {
                    setTextViewText(R.id.widget_speed_text, " ↓ ${SpeedCalculator.formatSpeed(rxSpeed)} | ↑ ${SpeedCalculator.formatSpeed(txSpeed)}")
                    setTextViewText(R.id.widget_data_usage, "Data: ${formatDataUsage(currentRxBytes + currentTxBytes)}")
                    setTextViewText(R.id.widget_network_type, "Network: ${getNetworkType(context)}")
                }

                val widget = ComponentName(context, WidgetProvider::class.java)
                appWidgetManager.updateAppWidget(widget, views)

                saveTrafficData(context, currentRxBytes, currentTxBytes, currentTime)

                handler.postDelayed(this, 1000)
            }
        }
        handler.post(updateSpeedRunnable)
    }

    private fun formatDataUsage(bytes: Long): String {
        val df = DecimalFormat("0.00")
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb > 1 -> "${df.format(gb)} GB"
            mb > 1 -> "${df.format(mb)} MB"
            else -> "${df.format(kb)} KB"
        }
    }

    private fun saveTrafficData(context: Context, rx: Long, tx: Long, time: Long) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit {
            putLong("rxBytes", rx)
                .putLong("txBytes", tx)
                .putLong("previousTime", time)
        }
    }

    private fun loadTrafficData(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        previousRxBytes = prefs.getLong("rxBytes", TrafficStats.getTotalRxBytes())
        previousTxBytes = prefs.getLong("txBytes", TrafficStats.getTotalTxBytes())
        previousTime = prefs.getLong("previousTime", System.currentTimeMillis())
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        handler.removeCallbacksAndMessages(null)
        super.onDeleted(context, appWidgetIds)
    }

    override fun onEnabled(context: Context) {
        networkMonitor = NetworkMonitor(context).also { it.register() }
    }

    override fun onDisabled(context: Context) {
        networkMonitor?.unregister()
        handler.removeCallbacksAndMessages(null)
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit { clear() }
        super.onDisabled(context)
    }
}
