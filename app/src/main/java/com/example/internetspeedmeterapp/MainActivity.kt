package com.example.internetspeedmeterapp

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.net.TrafficStats
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.internetspeedmeterapp.adapters.DataUsageAdapter
import com.example.internetspeedmeterapp.databinding.ActivityMainBinding
import com.example.internetspeedmeterapp.datahandler.DataUsageHandler
import java.util.Locale
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.math.ln
import kotlin.math.pow

class MainActivity : AppCompatActivity() {

    companion object {
        private const val PREF_NAME = "AppPreferences"
        private const val KEY_PROCESS_STATE = "processState"
        private const val UPDATE_INTERVAL_MS = 5000L
    }

    private val handler = Handler(Looper.getMainLooper())
    private val dataUsageList = mutableListOf<DataUsageHandler>()
    private lateinit var adapter: DataUsageAdapter
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupRecyclerView()
        checkAndRequestPermissions()
        restoreProcessState()
    }

    private val updateDataTask = object : Runnable {
        @SuppressLint("NotifyDataSetChanged")
        override fun run() {
            if (dataUsageList.size >= 2) {
                dataUsageList[0].usage = formatDataSize(getWiFiDataUsage())
                dataUsageList[1].usage = formatDataSize(getMobileDataUsage())
                adapter.notifyItemRangeChanged(0, 2)
            }
            handler.postDelayed(this, UPDATE_INTERVAL_MS)
        }
    }

    private val overlayPermissionLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (Settings.canDrawOverlays(this)) {
                startFloatingSpeedService()
            } else {
                showToast("Overlay permission is required to use this feature.")
            }
        }

    private fun setupRecyclerView() {
        val recyclerView: RecyclerView = findViewById(R.id.dataUsageRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        dataUsageList.apply {
            add(DataUsageHandler("Wi-Fi Usage: ", formatDataSize(getWiFiDataUsage())))
            add(DataUsageHandler("Mobile Data Usage: ", formatDataSize(getMobileDataUsage())))
        }
        adapter = DataUsageAdapter(dataUsageList)
        recyclerView.adapter = adapter
        startDataUpdates()
    }

    private fun checkAndRequestPermissions() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                "package:$packageName".toUri()
            )
            overlayPermissionLauncher.launch(intent)
        }
    }

    private fun restoreProcessState() {
        val preferences: SharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE)
        if (preferences.getBoolean(KEY_PROCESS_STATE, true)) {
            startFloatingSpeedService()
        }
    }

    private fun startDataUpdates() {
        handler.postDelayed(updateDataTask, UPDATE_INTERVAL_MS)
    }

    private fun getMobileDataUsage(): Long {
        return TrafficStats.getMobileRxBytes() + TrafficStats.getMobileTxBytes()
    }

    private fun getTotalDataUsage(): Long {
        return TrafficStats.getTotalRxBytes() + TrafficStats.getTotalTxBytes()
    }

    private fun getWiFiDataUsage(): Long {
        return getTotalDataUsage() - getMobileDataUsage()
    }

    @SuppressLint("DefaultLocale")
    private fun formatDataSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val exp = (ln(bytes.toDouble()) / ln(1024.0)).toInt()
        val unit = "KMGTPE"[exp - 1]
        return String.format(Locale.getDefault(), "%.1f %sB", bytes / 1024.0.pow(exp.toDouble()), unit)
    }

    private fun startFloatingSpeedService() {
        if (!Settings.canDrawOverlays(this)) {
            showToast("Please enable 'Display over other apps' permission in settings.")
            return
        }
        val serviceIntent = Intent(this, SpeedService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateDataTask)
    }
}
