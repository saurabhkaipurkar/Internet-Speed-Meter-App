package com.project.internetspeedmeterapp.util

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.widget.RemoteViews
import com.project.internetspeedmeterapp.R
import com.project.internetspeedmeterapp.widgets.WidgetProvider

class NetworkMonitor(private val context: Context) {

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            updateWidget()
        }

        override fun onLost(network: Network) {
            updateWidget()
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            updateWidget()
        }
    }
    fun register() {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }

    fun unregister() {
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }

    private fun updateWidget() {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val widget = ComponentName(context, WidgetProvider::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(widget)

        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_internet_speed)
            val networkType = WidgetProvider.getNetworkType(context)
            views.setTextViewText(R.id.widget_network_type, "Network: $networkType")
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
