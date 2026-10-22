package com.nexusos.agent.domain

class SocialCrmEngine(private val autoPilot: FreelanceAutoPilot) {

    suspend fun huntFacebookGroupLeads(skillKeyword: String, onUpdate: (String) -> Unit) {
        val specializedPrompt = """
            Open or inspect the active Facebook Group feed.
            Scroll through posts naturally. Ignore memes or spam.
            Find any post where someone is looking to hire for: $skillKeyword.
            When found, stop scrolling, draft a helpful comment or inbox reply based on RAG_MEMORY, and set requiresConfirmation=true before sending.
        """.trimIndent()
        autoPilot.executeAutonomousLoop(specializedPrompt, maxSteps = 10, onStatusUpdate = onUpdate)
    }

    suspend fun replyToPageInbox(onUpdate: (String) -> Unit) {
        val inboxPrompt = """
            Inspect the current Facebook / Messenger / WhatsApp conversation on screen.
            Read the client's message and any image on screen.
            Type a polite, professional reply matching RAG_MEMORY and pause for confirmation before pressing send.
        """.trimIndent()
        autoPilot.executeAutonomousLoop(inboxPrompt, maxSteps = 6, onStatusUpdate = onUpdate)
    }
}
