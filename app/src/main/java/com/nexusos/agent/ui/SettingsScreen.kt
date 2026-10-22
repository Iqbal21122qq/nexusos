package com.nexusos.agent.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.nexusos.agent.data.local.ApiKeyManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    apiKeyManager: ApiKeyManager,
    isRootAvailable: Boolean,
    isAccessibilityActive: Boolean,
    onOpenAccessibilitySettings: () -> Unit,
    onToggleVoiceService: (Boolean) -> Unit,
    onTriggerTask: (String) -> Unit,
    onEmergencyStop: () -> Unit,
    onHuntFacebookLeads: (String) -> Unit = {},
    onReplyPageInbox: () -> Unit = {},
    statusLog: String
) {
    var apiKey by remember { mutableStateOf(apiKeyManager.getApiKey()) }
    var modelName by remember { mutableStateOf(apiKeyManager.getPrimaryModel()) }
    var rpmLimit by remember { mutableStateOf(apiKeyManager.getRpmLimit().toString()) }
    var manualPrompt by remember { mutableStateOf("") }
    var voiceActive by remember { mutableStateOf(false) }
    var saveMessage by remember { mutableStateOf("") }
    var showApiKey by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.SmartToy,
                            contentDescription = "Agent Icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "NexusOS Agent",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onEmergencyStop,
                        modifier = Modifier.testTag("action_kill_switch_top")
                    ) {
                        Icon(
                            Icons.Default.PowerSettingsNew,
                            contentDescription = "Emergency Kill Switch",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // System Status Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_system_status"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Device & Service Telemetry",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Root Shell (su):")
                        SuggestionChip(
                            onClick = {},
                            label = { Text(if (isRootAvailable) "Granted" else "Not Detected") },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = if (isRootAvailable) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Screen Vision (Accessibility):")
                        SuggestionChip(
                            onClick = {
                                if (!isAccessibilityActive) onOpenAccessibilitySettings()
                            },
                            label = { Text(if (isAccessibilityActive) "Active" else "Enable Needed") },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = if (isAccessibilityActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer
                            )
                        )
                    }

                    if (!isAccessibilityActive) {
                        Button(
                            onClick = onOpenAccessibilitySettings,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_enable_accessibility")
                        ) {
                            Icon(Icons.Default.SettingsAccessibility, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Open Accessibility Settings")
                        }
                    }
                }
            }

            // Dynamic Gemini API & Model Configuration
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_gemini_config")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Gemini Engine Configuration",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text("Google AI Studio API Key") },
                        visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showApiKey = !showApiKey }) {
                                Icon(
                                    imageVector = if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle API Key visibility"
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_api_key"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = modelName,
                        onValueChange = { modelName = it },
                        label = { Text("Model Name (e.g., gemini-2.5-flash)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_model_name"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = rpmLimit,
                        onValueChange = { rpmLimit = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Free Tier Rate Limit (RPM: 1-60)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_rpm_limit"),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            apiKeyManager.saveApiKey(apiKey)
                            apiKeyManager.setPrimaryModel(modelName)
                            apiKeyManager.setRpmLimit(rpmLimit.toIntOrNull() ?: 10)
                            saveMessage = "API Key ও Model সফলভাবে সেভ হয়েছে!"
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_save_config")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Save Configuration")
                    }

                    if (saveMessage.isNotEmpty()) {
                        Text(
                            saveMessage,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Quick Automation Presets
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_automation_presets")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Autonomous Workflows",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Quickly launch preconfigured autonomous tasks for freelancing and CRM:",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                manualPrompt = "আপওয়ার্ক বা ফাইভার অ্যাপ খুলে ওয়েব ডেভেলপমেন্টের নতুন জব পোস্ট খুঁজে প্রস্তাব ড্রাফট করো"
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Freelance Bid")
                        }
                        OutlinedButton(
                            onClick = {
                                manualPrompt = "ফেসবুক গ্রুপে গিয়ে ওয়েব ডেভেলপমেন্ট কাজের নতুন পোস্ট খুঁজে লিড বের করো"
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("FB Leads")
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            manualPrompt = "স্ক্রিনের চ্যাট বা ইনবক্স মেসেজ পড়ে প্রোফাইল অনুযায়ী উত্তর ড্রাফট করো"
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Inbox Auto-Responder")
                    }
                }
            }

            // Agent Command & Voice Controls
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_agent_control")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Autonomous Control Center",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = {
                                voiceActive = !voiceActive
                                onToggleVoiceService(voiceActive)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_toggle_voice")
                        ) {
                            Icon(
                                if (voiceActive) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = null
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(if (voiceActive) "Stop Voice" else "Voice Agent")
                        }

                        Button(
                            onClick = onEmergencyStop,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_emergency_kill")
                        ) {
                            Icon(Icons.Default.StopCircle, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("KILL SWITCH")
                        }
                    }

                    OutlinedTextField(
                        value = manualPrompt,
                        onValueChange = { manualPrompt = it },
                        label = { Text("কমান্ড বা লক্ষ্য লিখুন...") },
                        placeholder = { Text("যেমন: ফেসবুক গ্রুপে ওয়েব ডেভেলপমেন্টের কাজ খোঁজো") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_agent_prompt"),
                        minLines = 3
                    )

                    Button(
                        onClick = {
                            if (manualPrompt.isNotBlank()) {
                                onTriggerTask(manualPrompt)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_execute_task")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Execute Autonomous Task")
                    }
                }
            }

            // Live Audit Log Viewer
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_audit_log")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Live Agent Audit Log",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(Icons.Default.Terminal, contentDescription = null)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = statusLog.ifBlank { "এজেন্ট কোনো কাজ শুরু করলে এখানে রিয়েল-টাইম ডিসিশন ও অডিট লগ দেখা যাবে..." },
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }
    }
}
