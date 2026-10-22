package com.nexusos.agent

import android.app.Application
import com.topjohnwu.superuser.Shell

class NexusApplication : Application() {
    companion object {
        lateinit var instance: NexusApplication
            private set
    }

    @Suppress("DEPRECATION")
    override fun onCreate() {
        super.onCreate()
        instance = this
        Shell.enableVerboseLogging = false
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setFlags(Shell.FLAG_REDIRECT_STDERR)
                .setTimeout(15)
        )
    }
}
