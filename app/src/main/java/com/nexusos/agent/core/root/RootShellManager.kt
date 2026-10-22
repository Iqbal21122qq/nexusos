package com.nexusos.agent.core.root

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.random.Random

@Suppress("DEPRECATION")
class RootShellManager(private val cacheDir: File) {

    private val isEmergencyStopped = AtomicBoolean(false)

    init {
        Shell.enableVerboseLogging = false
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setFlags(Shell.FLAG_REDIRECT_STDERR)
                .setTimeout(15)
        )
    }

    fun checkRootAccess(): Boolean {
        return Shell.getShell().isRoot
    }

    /**
     * Module 7: Hardware Kill-Switch Trigger (Volume Down 3x)
     */
    fun triggerEmergencyKillSwitch() {
        isEmergencyStopped.set(true)
        Shell.cmd("pkill -f input").exec()
    }

    fun resetEmergencyKillSwitch() {
        isEmergencyStopped.set(false)
    }

    suspend fun executeSafeCommand(command: String): Result<String> = withContext(Dispatchers.IO) {
        if (isEmergencyStopped.get()) {
            return@withContext Result.failure(IllegalStateException("Emergency Kill-Switch Active!"))
        }
        if (!CommandFirewall.isCommandSafe(command)) {
            return@withContext Result.failure(SecurityException("BLOCKED BY COMMAND FIREWALL: $command"))
        }

        val result = Shell.cmd(command).exec()
        if (result.isSuccess) {
            Result.success(result.out.joinToString("\n"))
        } else {
            Result.failure(RuntimeException("Shell Error: ${result.err.joinToString("\n")}"))
        }
    }

    suspend fun tap(x: Int, y: Int): Result<String> {
        // Slight natural jitter (+- 3px) and pacing delay
        val jitterX = (x + Random.nextInt(-3, 4)).coerceAtLeast(0)
        val jitterY = (y + Random.nextInt(-3, 4)).coerceAtLeast(0)
        delay(Random.nextLong(120, 320))
        return executeSafeCommand("input tap $jitterX $jitterY")
    }

    suspend fun swipe(x1: Int, y1: Int, x2: Int, y2: Int, durationMs: Int = 350): Result<String> {
        val naturalDuration = durationMs + Random.nextInt(-40, 80)
        delay(Random.nextLong(150, 350))
        return executeSafeCommand("input swipe $x1 $y1 $x2 $y2 $naturalDuration")
    }

    suspend fun typeTextNaturally(text: String): Result<String> {
        val escaped = text.replace(" ", "%s").replace("'", "")
        delay(Random.nextLong(180, 400))
        return executeSafeCommand("input text '$escaped'")
    }

    suspend fun captureScreenshotBitmap(): Bitmap? = withContext(Dispatchers.IO) {
        if (isEmergencyStopped.get()) return@withContext null
        val tempFile = File(cacheDir, "screen_cap.png")
        val res = Shell.cmd("screencap -p ${tempFile.absolutePath}").exec()
        if (res.isSuccess && tempFile.exists()) {
            BitmapFactory.decodeFile(tempFile.absolutePath)
        } else {
            null
        }
    }
}
