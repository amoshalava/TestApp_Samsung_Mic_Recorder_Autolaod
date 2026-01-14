package com.samsung.micrecorder

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.samsung.micrecorder.data.TranscriptionHistory
import java.text.SimpleDateFormat
import java.util.*

/**
 * Main activity for the Samsung Test Autoload Mic Record App.
 * Displays transcription history and handles elevated permission requests.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    // Permission launcher for RECORD_AUDIO
    private val recordAudioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        checkAndUpdatePermissions()
    }

    // Permission launcher for POST_NOTIFICATIONS (Android 13+)
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        checkAndUpdatePermissions()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check permissions on launch
        checkAndUpdatePermissions()

        // Start the foreground service if permissions are granted
        if (areAllPermissionsGranted()) {
            startMicRecorderService()
        }

        setContent {
            MicRecorderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF000000) // Black background
                ) {
                    MainScreen(
                        viewModel = viewModel,
                        onRequestPermissions = { requestPermissions() },
                        onRequestOverlay = { requestOverlayPermission() },
                        onRequestBatteryExemption = { requestBatteryExemption() }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Update permission status when returning to the app
        checkAndUpdatePermissions()
        
        // Try starting service again if everything was just granted
        if (areAllPermissionsGranted()) {
            startMicRecorderService()
        }
    }

    /**
     * Check if all required permissions (including special ones) are granted.
     */
    private fun areAllPermissionsGranted(): Boolean {
        val recordAudioGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        val notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        val overlayGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }

        return recordAudioGranted && notificationGranted && overlayGranted
    }

    /**
     * Check permissions and update ViewModel state.
     */
    private fun checkAndUpdatePermissions() {
        val allGranted = areAllPermissionsGranted()
        viewModel.setPermissionsGranted(allGranted)
        viewModel.setMicActive(allGranted)
    }

    /**
     * Request standard runtime permissions.
     */
    private fun requestPermissions() {
        val recordAudioGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!recordAudioGranted) {
            recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notificationGranted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!notificationGranted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
    }

    /**
     * Navigate user to 'Appear on top' settings.
     */
    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }

    /**
     * Directly request to ignore battery optimizations via system dialog.
     */
    private fun requestBatteryExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                // This triggers a direct system dialog asking the user to allow the app to ignore battery optimizations
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            } catch (e: Exception) {
                // Fallback to the general settings if the direct request fails
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                startActivity(intent)
            }
        }
    }

    /**
     * Start the foreground microphone recorder service.
     */
    private fun startMicRecorderService() {
        val serviceIntent = Intent(this, MicRecorderService::class.java)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        } catch (e: Exception) {
            // Log error but don't crash the activity
        }
    }
}

@Composable
fun MicRecorderTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF03A9F4),
            background = Color(0xFF000000),
            surface = Color(0xFF1E1E1E),
            onPrimary = Color.White,
            onBackground = Color.White,
            onSurface = Color.White
        ),
        content = content
    )
}

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onRequestPermissions: () -> Unit,
    onRequestOverlay: () -> Unit,
    onRequestBatteryExemption: () -> Unit
) {
    val historyList by viewModel.historyFlow.collectAsState(initial = emptyList())
    val isMicActive by viewModel.isMicActive.collectAsState()
    val arePermissionsGranted by viewModel.areAllPermissionsGranted.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        AppHeader()

        Spacer(modifier = Modifier.height(24.dp))

        StatusSection(
            isMicActive = isMicActive,
            arePermissionsGranted = arePermissionsGranted,
            onRequestPermissions = onRequestPermissions
        )

        Spacer(modifier = Modifier.height(16.dp))
        
        ElevatedPermissionsSection(
            onRequestOverlay = onRequestOverlay,
            onRequestBatteryExemption = onRequestBatteryExemption
        )

        Spacer(modifier = Modifier.height(24.dp))

        HistorySection(historyList = historyList)
    }
}

@Composable
fun ElevatedPermissionsSection(
    onRequestOverlay: () -> Unit,
    onRequestBatteryExemption: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val isOverlayGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        Settings.canDrawOverlays(context)
    } else true
    
    val powerManager = context.getSystemService(android.content.Context.POWER_SERVICE) as PowerManager
    val isBatteryOptimized = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        !powerManager.isIgnoringBatteryOptimizations(context.packageName)
    } else false

    if (!isOverlayGranted || isBatteryOptimized) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2C)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    "Autoload Setup Required",
                    color = Color(0xFFFFC107),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                if (!isOverlayGranted) {
                    PermissionRow(
                        title = "Appear on top (Required for Autoload)",
                        onClick = onRequestOverlay
                    )
                }
                
                if (isBatteryOptimized) {
                    Spacer(modifier = Modifier.height(8.dp))
                    PermissionRow(
                        title = "Disable Battery Optimization",
                        onClick = onRequestBatteryExemption
                    )
                }
            }
        }
    }
}

@Composable
fun PermissionRow(title: String, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(1f))
        TextButton(onClick = onClick) {
            Text("Fix", color = Color(0xFF03A9F4))
        }
    }
}

@Composable
fun AppHeader() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(R.string.app_name),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.app_version),
            fontSize = 14.sp,
            color = Color(0xFF9E9E9E)
        )
    }
}

@Composable
fun StatusSection(
    isMicActive: Boolean,
    arePermissionsGranted: Boolean,
    onRequestPermissions: () -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = if (isMicActive) Icons.Default.Mic else Icons.Default.MicOff,
                contentDescription = null,
                tint = if (isMicActive) Color(0xFF03A9F4) else Color(0xFF757575),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isMicActive)
                    stringResource(R.string.status_listening)
                else
                    stringResource(R.string.status_mic_inactive),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = if (arePermissionsGranted) Icons.Default.Check else Icons.Default.Warning,
                contentDescription = null,
                tint = if (arePermissionsGranted) Color(0xFF4CAF50) else Color(0xFFFFC107),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (arePermissionsGranted)
                    stringResource(R.string.status_permissions_granted)
                else
                    stringResource(R.string.status_permissions_required),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )

            if (!arePermissionsGranted) {
                Button(
                    onClick = onRequestPermissions,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF03A9F4)
                    )
                ) {
                    Text("Grant")
                }
            }
        }
    }
}

@Composable
fun HistorySection(historyList: List<TranscriptionHistory>) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "History",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (historyList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.history_empty),
                    fontSize = 16.sp,
                    color = Color(0xFF9E9E9E)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(historyList) { item ->
                    HistoryItem(item)
                }
            }
        }
    }
}

@Composable
fun HistoryItem(item: TranscriptionHistory) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1E1E1E))
            .padding(12.dp)
    ) {
        Text(
            text = item.text,
            fontSize = 16.sp,
            color = if (item.isError) Color(0xFFF44336) else Color.White
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = formatTimestamp(item.timestamp),
            fontSize = 12.sp,
            color = Color(0xFF9E9E9E)
        )
    }
}

fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
