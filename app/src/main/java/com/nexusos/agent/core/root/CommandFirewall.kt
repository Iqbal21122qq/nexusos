package com.nexusos.agent.core.root

object CommandFirewall {
    private val BLOCKED_PATTERNS = listOf(
        Regex("""\brm\s+(-[rRf]+\s+)*(/|/system|/vendor|/boot|/efs|/data)"""),
        Regex("""\bdd\b"""),
        Regex("""\bmkfs\b"""),
        Regex("""\bchmod\s+777\s+/"""),
        Regex("""\bmount\b.*remount.*(/system|/vendor|/boot)"""),
        Regex(""">\s*/dev/block/""")
    )

    private val SENSITIVE_PACKAGES = listOf(
        "com.bKash.customerapp",
        "com.konasl.nagad",
        "com.google.android.apps.walletnfcrel"
    )

    fun isCommandSafe(command: String): Boolean {
        val normalized = command.trim().lowercase()
        return BLOCKED_PATTERNS.none { it.containsMatchIn(normalized) }
    }

    fun requiresBiometricApproval(foregroundPackage: String, decisionRequiresConfirm: Boolean): Boolean {
        if (decisionRequiresConfirm) return true
        return SENSITIVE_PACKAGES.any { foregroundPackage.contains(it, ignoreCase = true) }
    }
}
