package com.nexusos.agent.domain

import com.nexusos.agent.core.root.CommandFirewall
import com.nexusos.agent.core.root.RootShellManager
import com.nexusos.agent.core.vision.NexusAccessibilityService
import com.nexusos.agent.core.voice.VoiceForegroundService
import com.nexusos.agent.data.local.AuditLogDao
import com.nexusos.agent.data.local.MemoryDatabase
import com.nexusos.agent.data.remote.ActionType
import com.nexusos.agent.data.remote.GeminiAgentClient
import kotlinx.coroutines.delay

class FreelanceAutoPilot(
    private val geminiClient: GeminiAgentClient,
    private val rootShell: RootShellManager,
    private val memoryDb: MemoryDatabase,
    private val auditLogger: AuditLogDao
) {
    /**
     * Executes multi-step autonomous loop for Freelancing, Upwork/Fiverr bidding, or general tasks
     */
    suspend fun executeAutonomousLoop(
        userGoal: String,
        maxSteps: Int = 12,
        onStatusUpdate: (String) -> Unit
    ) {
        rootShell.resetEmergencyKillSwitch()
        val ragContext = memoryDb.retrieveRelevantContext(userGoal)

        for (step in 1..maxSteps) {
            if (NexusAccessibilityService.killSwitchTriggered.value) {
                onStatusUpdate("🛑 Kill-Switch সক্রিয়! লুপ বন্ধ হয়েছে।")
                return
            }

            val uiXml = NexusAccessibilityService.instance?.dumpCompactUiTree() ?: "<screen/>"
            val currentPkg = NexusAccessibilityService.instance?.getCurrentPackageName().orEmpty()
            val screenshot = rootShell.captureScreenshotBitmap()
            val base64Img = screenshot?.let { geminiClient.encodeBitmapToBase64(it) }

            val result = geminiClient.analyzeScreenAndDecide(
                userGoal = "Step $step/$maxSteps for Goal: $userGoal",
                uiXmlHierarchy = uiXml,
                screenshotBase64 = base64Img,
                ragContext = ragContext
            )

            val decision = result.getOrElse { err ->
                auditLogger.logAction("ERROR", err.message ?: "API Error")
                onStatusUpdate("❌ API Error: ${err.message}\n${auditLogger.getRecentLogs()}")
                return
            }

            auditLogger.logAction(decision.action.name, decision.reasoning)
            onStatusUpdate("🔄 ধাপ $step: ${decision.action} - ${decision.reasoning}\n\n${auditLogger.getRecentLogs()}")

            if (decision.spokenResponseBn.isNotBlank()) {
                VoiceForegroundService.instance?.speak(decision.spokenResponseBn)
            }

            // Module 7: Human-in-the-Loop Safety Check before sensitive actions
            if (CommandFirewall.requiresBiometricApproval(currentPkg, decision.requiresConfirmation)) {
                VoiceForegroundService.instance?.speak("বস, এই মেসেজ বা অ্যাকশনটি কি আমি কনফার্ম করব? অনুমতি দিন।")
                auditLogger.logAction("WAIT_APPROVAL", "Paused for user confirmation before final send.")
                onStatusUpdate("⏸️ আপনার অনুমতির অপেক্ষায়: ${decision.reasoning}\n\n${auditLogger.getRecentLogs()}")
                return
            }

            when (decision.action) {
                ActionType.TAP -> {
                    if (decision.coordinates.size >= 2) {
                        rootShell.tap(decision.coordinates[0], decision.coordinates[1])
                    }
                }
                ActionType.SWIPE -> {
                    val c = decision.coordinates
                    if (c.size >= 4) {
                        rootShell.swipe(c[0], c[1], c[2], c[3])
                    } else {
                        // Default human scroll down
                        rootShell.swipe(540, 1450, 540, 480)
                    }
                }
                ActionType.TYPE -> {
                    if (decision.coordinates.size >= 2) {
                        rootShell.tap(decision.coordinates[0], decision.coordinates[1])
                        delay(400)
                    }
                    rootShell.typeTextNaturally(decision.text)
                }
                ActionType.SHELL -> {
                    rootShell.executeSafeCommand(decision.text)
                }
                ActionType.ASK_USER, ActionType.COMPLETE -> {
                    auditLogger.logAction("COMPLETE", "Task finished at step $step")
                    onStatusUpdate("✅ টাস্ক সম্পন্ন!\n\n${auditLogger.getRecentLogs()}")
                    return
                }
            }
            delay(1800) // Wait for UI transition
        }
    }
}
