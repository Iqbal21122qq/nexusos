package com.nexusos.agent.core.vision

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class NexusAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "NexusAccessibility"
        var instance: NexusAccessibilityService? = null
            private set

        private val _killSwitchTriggered = MutableStateFlow(false)
        val killSwitchTriggered: StateFlow<Boolean> = _killSwitchTriggered

        fun resetKillSwitch() {
            _killSwitchTriggered.value = false
        }
    }

    // Hardware Kill-Switch Tracking (3 rapid Volume Down presses within 1.5 seconds)
    private val volumeDownTimestamps = mutableListOf<Long>()
    var onEmergencyKillCallback: (() -> Unit)? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "NexusAccessibilityService Connected.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Real-time window changes can be observed here if needed
    }

    override fun onInterrupt() {
        Log.w(TAG, "NexusAccessibilityService Interrupted.")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) instance = null
    }

    /**
     * Module 7: Hardware Kill-Switch (Press Volume Down 3 times rapidly)
     */
    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && event.action == KeyEvent.ACTION_DOWN) {
            val now = System.currentTimeMillis()
            volumeDownTimestamps.add(now)
            volumeDownTimestamps.removeAll { now - it > 1500L }

            if (volumeDownTimestamps.size >= 3) {
                volumeDownTimestamps.clear()
                _killSwitchTriggered.value = true
                onEmergencyKillCallback?.invoke()
                Log.e(TAG, "EMERGENCY KILL-SWITCH ACTIVATED BY USER!")
                return true // Consume event
            }
        }
        return super.onKeyEvent(event)
    }

    /**
     * Extracts a compact, token-efficient UI Hierarchy XML with exact center [x, y] coordinates
     */
    fun dumpCompactUiTree(): String {
        val rootNode = rootInActiveWindow ?: return "<screen error=\"no_active_window\"/>"
        val sb = StringBuilder()
        val pkgName = rootNode.packageName?.toString() ?: "unknown"
        sb.append("<screen package=\"").append(pkgName).append("\">\n")
        traverseNode(rootNode, sb, 1)
        sb.append("</screen>")
        return sb.toString()
    }

    fun getCurrentPackageName(): String {
        return rootInActiveWindow?.packageName?.toString() ?: ""
    }

    private fun traverseNode(node: AccessibilityNodeInfo?, sb: StringBuilder, depth: Int) {
        if (node == null || !node.isVisibleToUser) return

        val text = node.text?.toString()?.trim()?.replace("\"", "'") ?: ""
        val desc = node.contentDescription?.toString()?.trim()?.replace("\"", "'") ?: ""
        val viewId = node.viewIdResourceName?.substringAfterLast("/") ?: ""
        val clickable = node.isClickable
        val editable = node.isEditable

        val rect = Rect()
        node.getBoundsInScreen(rect)
        val centerX = rect.centerX()
        val centerY = rect.centerY()

        // Only include meaningful nodes to save Gemini API tokens
        val isMeaningful = text.isNotEmpty() || desc.isNotEmpty() || clickable || editable
        val indent = "  ".repeat(depth.coerceAtMost(10))

        if (isMeaningful && rect.width() > 0 && rect.height() > 0) {
            val tag = node.className?.toString()?.substringAfterLast(".") ?: "View"
            sb.append(indent).append("<").append(tag)
            if (viewId.isNotEmpty()) sb.append(" id=\"").append(viewId).append("\"")
            if (text.isNotEmpty()) sb.append(" text=\"").append(text.take(120)).append("\"")
            if (desc.isNotEmpty()) sb.append(" desc=\"").append(desc.take(80)).append("\"")
            if (clickable) sb.append(" clickable=\"true\"")
            if (editable) sb.append(" editable=\"true\"")
            sb.append(" center=\"[").append(centerX).append(",").append(centerY).append("]\"")
            sb.append(" />\n")
        }

        for (i in 0 until node.childCount) {
            traverseNode(node.getChild(i), sb, depth + 1)
        }
    }
}
