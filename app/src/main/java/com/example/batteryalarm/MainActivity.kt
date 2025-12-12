package com.example.batteryalarm

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.batteryalarm.ui.theme.BatteryAlarmTheme
import androidx.activity.compose.rememberLauncherForActivityResult
import android.net.Uri
import com.example.batteryalarm.receivers.BatteryReceiver
import androidx.compose.ui.platform.LocalContext

class MainActivity : ComponentActivity() {

    private var currentMediaPlayer: MediaPlayer? = null
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        volumeControlStream = AudioManager.STREAM_ALARM
        prefs = getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)

        requestPermissions()

        if (intent?.action == "STOP_ALARM") {
            stopAlarm()
        }

        setContent {
            BatteryAlarmTheme {
                BatteryAlarmScreen(
                    onStartMonitoring = { startMonitoring() },
                    onStopMonitoring = { stopMonitoring() },
                    onVolumeChanged = { volume -> saveVolume(volume) },
                    onToneSelected = { uri, title -> saveTone(uri, title) },
                    onStopAlarm = { stopAlarm() },
                    prefs = prefs
                )
            }
        }
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.POST_NOTIFICATIONS,
                    Manifest.permission.READ_MEDIA_AUDIO
                ),
                1001
            )
        } else {
            @Suppress("DEPRECATION")
            requestPermissions(
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                1003
            )
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestPermissions(
                arrayOf(Manifest.permission.SCHEDULE_EXACT_ALARM),
                1002
            )
        }
    }

    private fun startMonitoring() {
        val intent = Intent(this, com.example.batteryalarm.services.BatteryMonitorService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        prefs.edit().putBoolean("is_monitoring", true).apply()
    }

    private fun stopMonitoring() {
        stopService(Intent(this, com.example.batteryalarm.services.BatteryMonitorService::class.java))
        prefs.edit().putBoolean("is_monitoring", false).apply()
    }

    private fun stopAlarm() {
        currentMediaPlayer?.stop()
        currentMediaPlayer?.release()
        currentMediaPlayer = null
        BatteryReceiver.stopAlarm(this)
    }

    private fun saveVolume(volume: Float) {
        prefs.edit().putFloat("alarm_volume", volume).apply()
        BatteryReceiver.applyVolume(volume)
    }

    private fun saveTone(uri: Uri, title: String?) {
        prefs.edit()
            .putString("selected_tone", uri.toString())
            .putString("selected_tone_title", title ?: "Custom tone")
            .apply()
    }
}

@Composable
fun BatteryAlarmScreen(
    onStartMonitoring: () -> Unit,
    onStopMonitoring: () -> Unit,
    onVolumeChanged: (Float) -> Unit,
    onToneSelected: (Uri, String?) -> Unit,
    onStopAlarm: () -> Unit,
    prefs: SharedPreferences
) {
    var isMonitoring by remember { 
        mutableStateOf(prefs.getBoolean("is_monitoring", false))
    }
    var selectedVolume by remember { 
        mutableFloatStateOf(prefs.getFloat("alarm_volume", 0.8f))
    }
    var selectedToneTitle by remember { 
        mutableStateOf(prefs.getString("selected_tone_title", "Default alarm tone") ?: "Default alarm tone")
    }

    val context = LocalContext.current
    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val intent = result.data
        val uri: Uri? = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
                intent?.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                    ?: intent?.data
            else -> intent?.data ?: @Suppress("DEPRECATION") intent?.getParcelableExtra(Intent.EXTRA_STREAM)
        }
        if (uri != null) {
            // Persist permission so the service can read it later
            try {
                val flags = intent?.flags ?: 0
                val persisted = flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                if (persisted != 0) {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                    )
                }
            } catch (_: Exception) { }

            val title = RingtoneManager.getRingtone(context, uri)?.getTitle(context)
                ?: uri.lastPathSegment
            selectedToneTitle = title ?: "Custom tone"
            onToneSelected(uri, title)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header
            Text(
                "Battery Alarm Monitor",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F1F1F)
            )

            // Status Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                elevation = CardDefaults.cardElevation(4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Monitoring Status",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Gray
                    )

                    Text(
                        if (isMonitoring) "🟢 Monitoring Active" else "🔴 Monitoring Stopped",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isMonitoring) Color(0xFF4CAF50) else Color.Red
                    )

                    Button(
                        onClick = {
                            isMonitoring = !isMonitoring
                            if (isMonitoring) onStartMonitoring() else onStopMonitoring()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isMonitoring) Color(0xFFFF6B6B) else Color(0xFF4CAF50)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            if (isMonitoring) "Stop Monitoring" else "Start Monitoring",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Volume Control Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Alarm Volume",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Gray
                    )

                    Slider(
                        value = selectedVolume,
                        onValueChange = {
                            selectedVolume = it
                            onVolumeChanged(it)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        valueRange = 0f..1f
                    )

                    Text(
                        "${(selectedVolume * 100).toInt()}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF4CAF50)
                    )

                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                addCategory(Intent.CATEGORY_OPENABLE)
                                type = "audio/*"
                                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("audio/*"))
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, prefs.getString("selected_tone", null)?.let { Uri.parse(it) })
                            }
                            ringtonePickerLauncher.launch(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text("Choose Alarm Tone", fontWeight = FontWeight.Bold)
                            Text(selectedToneTitle, fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
            }

            // Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(2.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4FF))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "📱 How it works:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F1F1F)
                    )
                    Text(
                        "• Start monitoring your battery\n• App runs in background\n• Alarm sounds at 100% charge\n• Tap 'Stop Alarm' to dismiss",
                        fontSize = 11.sp,
                        color = Color(0xFF555555)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Stop Alarm Button
            Button(
                onClick = onStopAlarm,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B6B)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Stop Alarm", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
