package com.nexusos.agent.data.local

import android.content.ContentValues
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AuditLogDao(private val memoryDb: MemoryDatabase) {

    private val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    fun logAction(actionType: String, details: String) {
        val cv = ContentValues().apply {
            put("timestamp", System.currentTimeMillis())
            put("action_type", actionType)
            put("details", details)
        }
        memoryDb.writableDatabase.insert("audit_logs", null, cv)
    }

    fun getRecentLogs(limit: Int = 15): String {
        val logs = mutableListOf<String>()
        memoryDb.readableDatabase.rawQuery(
            "SELECT timestamp, action_type, details FROM audit_logs ORDER BY id DESC LIMIT $limit",
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val time = formatter.format(Date(cursor.getLong(0)))
                val type = cursor.getString(1)
                val details = cursor.getString(2)
                logs.add("[$time] $type: $details")
            }
        }
        return logs.joinToString("\n")
    }
}
