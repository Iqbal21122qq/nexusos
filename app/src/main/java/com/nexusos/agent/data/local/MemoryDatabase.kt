package com.nexusos.agent.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class MemoryDatabase(context: Context) :
    SQLiteOpenHelper(context, "nexus_rag_memory.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        // Table 1: Personal RAG Knowledge Base (Skills, Portfolio, Templates)
        db.execSQL(
            """
            CREATE TABLE rag_memory (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                category TEXT NOT NULL,
                tags TEXT NOT NULL,
                content TEXT NOT NULL
            )
            """.trimIndent()
        )

        // Table 2: Module 7 Action Audit Log
        db.execSQL(
            """
            CREATE TABLE audit_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                timestamp INTEGER NOT NULL,
                action_type TEXT NOT NULL,
                details TEXT NOT NULL
            )
            """.trimIndent()
        )

        // Seed Default Freelancing Profile for User
        insertDefaultProfile(db)
    }

    private fun insertDefaultProfile(db: SQLiteDatabase) {
        val defaults = listOf(
            Triple("PORTFOLIO", "web development react nodejs fullstack", "Full-Stack Web Developer skilled in HTML, CSS, JavaScript, React, and Backend APIs. GitHub: https://github.com/my-portfolio"),
            Triple("PROPOSAL_STYLE", "upwork fiverr bid cover letter", "Write concise, friendly proposals focusing on solving the client's problem first, offering a quick demo, and sharing relevant GitHub links."),
            Triple("FB_LEAD_FILTER", "facebook group post website app bug", "Look for posts asking for website creation, bug fixing, landing pages, e-commerce setup, or Android app development.")
        )
        for ((cat, tags, text) in defaults) {
            val cv = ContentValues().apply {
                put("category", cat)
                put("tags", tags)
                put("content", text)
            }
            db.insert("rag_memory", null, cv)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    fun saveMemory(category: String, tags: String, content: String) {
        val cv = ContentValues().apply {
            put("category", category)
            put("tags", tags.lowercase())
            put("content", content)
        }
        writableDatabase.insert("rag_memory", null, cv)
    }

    /**
     * Retrieves top relevant RAG memories matching the current goal or screen context
     */
    fun retrieveRelevantContext(query: String, limit: Int = 3): String {
        val queryWords = query.lowercase().split(Regex("\\W+")).filter { it.length > 2 }.toSet()
        val results = mutableListOf<Pair<Int, String>>()

        readableDatabase.rawQuery("SELECT category, tags, content FROM rag_memory", null).use { cursor ->
            while (cursor.moveToNext()) {
                val cat = cursor.getString(0)
                val tags = cursor.getString(1).lowercase()
                val content = cursor.getString(2)
                val score = queryWords.count { word -> tags.contains(word) || content.lowercase().contains(word) }
                results.add(score to "[$cat]: $content")
            }
        }

        return results.sortedByDescending { it.first }
            .take(limit)
            .joinToString("\n") { it.second }
    }
}
