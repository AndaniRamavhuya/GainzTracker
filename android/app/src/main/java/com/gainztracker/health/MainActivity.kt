package com.gainztracker.health

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.Instant
import java.time.temporal.ChronoUnit

class MainActivity : ComponentActivity() {
    private lateinit var webView: WebView
    private lateinit var status: TextView
    private var healthClient: HealthConnectClient? = null

    private val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class),
    )

    private val permissionLauncher = registerForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { syncHealth() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        healthClient = runCatching { HealthConnectClient.getOrCreate(this) }.getOrNull()
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        status = TextView(this).apply { text = "Connect Samsung Health through Health Connect"; setPadding(24, 18, 24, 10) }
        val sync = Button(this).apply { text = "Connect / Sync Samsung Health"; setOnClickListener { requestOrSync() } }
        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = WebViewClient()
            loadUrl("https://andaniramavhuya.github.io/GainzTracker/")
        }
        root.addView(status)
        root.addView(sync)
        root.addView(webView, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    private fun requestOrSync() {
        val client = healthClient ?: run { status.text = "Install or enable Health Connect on this Samsung device."; return }
        lifecycleScope.launch {
            val granted = client.permissionController.getGrantedPermissions()
            if (!granted.containsAll(permissions)) permissionLauncher.launch(permissions) else syncHealth()
        }
    }

    private fun syncHealth() {
        val client = healthClient ?: return
        lifecycleScope.launch {
            try {
                val end = Instant.now()
                val start = end.minus(30, ChronoUnit.DAYS)
                val range = TimeRangeFilter.between(start, end)
                val steps = client.aggregate(
                    androidx.health.connect.client.request.AggregateRequest(
                        metrics = setOf(StepsRecord.COUNT_TOTAL), timeRangeFilter = range
                    )
                )[StepsRecord.COUNT_TOTAL] ?: 0L
                val calories = client.readRecords(
                    ReadRecordsRequest(ActiveCaloriesBurnedRecord::class, range)
                ).records.sumOf { it.energy.inKilocalories.toInt() }
                val workouts = client.readRecords(
                    ReadRecordsRequest(ExerciseSessionRecord::class, range)
                ).records.size
                val latestWeight = client.readRecords(
                    ReadRecordsRequest(WeightRecord::class, range)
                ).records.maxByOrNull { it.time }?.weight?.inKilograms
                val payload = JSONObject().apply {
                    put("source", "Samsung Health via Health Connect")
                    put("syncedAt", end.toString())
                    put("steps", steps)
                    put("activeCalories", calories)
                    put("workouts", workouts)
                    if (latestWeight != null) put("latestWeight", latestWeight)
                }
                val js = "localStorage.setItem('gainz-health', ${JSONObject.quote(payload.toString())}); window.dispatchEvent(new CustomEvent('gainz-health-sync', {detail: $payload}));"
                webView.evaluateJavascript(js, null)
                status.text = "Synced Samsung Health through Health Connect"
            } catch (error: Exception) {
                status.text = "Health sync failed: ${error.message ?: "check Health Connect permissions"}"
            }
        }
    }
}
