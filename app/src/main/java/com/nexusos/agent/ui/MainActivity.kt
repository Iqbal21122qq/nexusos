package com.nexusos.agent.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.nexusos.agent.core.root.RootShellManager
import com.nexusos.agent.core.vision.NexusAccessibilityService
import com.nexusos.agent.core.voice.VoiceForegroundService
import com.nexusos.agent.data.local.ApiKeyManager
import com.nexusos.agent.data.local.AuditLogDao
import com.nexusos.agent.data.local.MemoryDatabase
import com.nexusos.agent.data.remote.GeminiAgentClient
import com.nexusos.agent.domain.FreelanceAutoPilot
import com.nexusos.agent.domain.SocialCrmEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var apiKeyManager: ApiKeyManager
    private lateinit var rootShellManager: RootShellManager
    private lateinit var geminiClient: GeminiAgentClient
    private lateinit var memoryDatabase: MemoryDatabase
    private lateinit var auditLogDao: AuditLogDao
    private lateinit var freelanceAutoPilot: FreelanceAutoPilot
    private lateinit var socialCrmEngine: SocialCrmEngine

    private var activeTaskJob: Job? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        apiKeyManager = ApiKeyManager(this)
        rootShellManager = RootShellManager(cacheDir)
        geminiClient = GeminiAgentClient(apiKeyManager)
        memoryDatabase = MemoryDatabase(this)
        auditLogDao = AuditLogDao(memoryDatabase)
        freelanceAutoPilot = FreelanceAutoPilot(geminiClient, rootShellManager, memoryDatabase, auditLogDao)
        socialCrmEngine = SocialCrmEngine(freelanceAutoPilot)
        // Defer permission request to LaunchedEffect after composition is active

        setContent {
            MaterialTheme {
                var statusLog by remember { mutableStateOf("Ready. NexusOS Agent প্রস্তুত।") }
                var isRooted by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    requestRequiredPermissions()
                    isRooted = rootShellManager.checkRootAccess()
                    NexusAccessibilityService.instance?.onEmergencyKillCallback = {
                        activeTaskJob?.cancel()
                        rootShellManager.triggerEmergencyKillSwitch()
                        VoiceForegroundService.instance?.stopSpeaking()
                        statusLog = "⚠️ HARDWARE KILL-SWITCH TRIGGERED! সব অটোমেশন বন্ধ করা হয়েছে।"
                    }

                    // Listen for voice commands from VoiceForegroundService
                    VoiceForegroundService.voiceCommands.collect { spokenGoal ->
                        statusLog = "🎙️ ভয়েস কমান্ড: $spokenGoal\nপ্রসেসিং শুরু হচ্ছে..."
                        activeTaskJob?.cancel()
                        activeTaskJob = lifecycleScope.launch {
                            freelanceAutoPilot.executeAutonomousLoop(spokenGoal) { logUpdate ->
                                statusLog = logUpdate
                            }
                        }
                    }
                }

                SettingsScreen(
                    apiKeyManager = apiKeyManager,
                    isRootAvailable = isRooted,
                    isAccessibilityActive = NexusAccessibilityService.instance != null,
                    onOpenAccessibilitySettings = {
                        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    },
                    onToggleVoiceService = { enable ->
                        val intent = Intent(this@MainActivity, VoiceForegroundService::class.java)
                        if (enable) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                startForegroundService(intent)
                            } else {
                                startService(intent)
                            }
                        } else {
                            stopService(intent)
                        }
                    },
                    onTriggerTask = { goal ->
                        activeTaskJob?.cancel()
                        activeTaskJob = lifecycleScope.launch {
                            statusLog = "🚀 টাস্ক শুরু: $goal"
                            freelanceAutoPilot.executeAutonomousLoop(goal) { logUpdate ->
                                statusLog = logUpdate
                            }
                        }
                    },
                    onHuntFacebookLeads = { keyword ->
                        activeTaskJob?.cancel()
                        activeTaskJob = lifecycleScope.launch {
                            statusLog = "🎯 ফেসবুক লিড হান্টার শুরু ($keyword)..."
                            socialCrmEngine.huntFacebookGroupLeads(keyword) { logUpdate ->
                                statusLog = logUpdate
                            }
                        }
                    },
                    onReplyPageInbox = {
                        activeTaskJob?.cancel()
                        activeTaskJob = lifecycleScope.launch {
                            statusLog = "💬 ইনবক্স অটো-রেসপন্ডার সক্রিয়..."
                            socialCrmEngine.replyToPageInbox { logUpdate ->
                                statusLog = logUpdate
                            }
                        }
                    },
                    onEmergencyStop = {
                        activeTaskJob?.cancel()
                        rootShellManager.triggerEmergencyKillSwitch()
                        VoiceForegroundService.instance?.stopSpeaking()
                        NexusAccessibilityService.resetKillSwitch()
                        statusLog = "🛑 Emergency Stop চাপানো হয়েছে। সমস্ত রুট কমান্ড বাতিল করা হয়েছে।"
                    },
                    statusLog = statusLog
                )
            }
        }
    }

    private fun requestRequiredPermissions() {
        val permissions = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.RECORD_AUDIO)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (permissions.isNotEmpty()) {
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }
}
