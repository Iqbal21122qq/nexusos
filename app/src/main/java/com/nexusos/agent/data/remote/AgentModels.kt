package com.nexusos.agent.data.remote

import kotlinx.serialization.Serializable

@Serializable
enum class ActionType {
    TAP, SWIPE, TYPE, SHELL, ASK_USER, COMPLETE
}

@Serializable
data class AgentDecision(
    val action: ActionType,
    val coordinates: List<Int> = emptyList(), // [x, y] or [x1, y1, x2, y2]
    val text: String = "",
    val reasoning: String = "",
    val requiresConfirmation: Boolean = false,
    val spokenResponseBn: String = "" // বাংলায় বা বাংলিশে ইউজারকে যা বলবে
)
