package com.glyph.launcher

import android.app.Application
import android.os.StrictMode
import dagger.hilt.android.HiltAndroidApp
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

@HiltAndroidApp
class GlyphApplication : Application() {
    override fun onCreate() {
        // 🚨 Crash Logger: If the app crashes on startup, save the error to a file!
        // You can find it at: Android/data/com.glyph.launcher/files/crash_log.txt
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val file = File(getExternalFilesDir(null), "crash_log.txt")
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                file.writeText("CRASH DETECTED:\n$sw")
            } catch (e: Exception) { 
                e.printStackTrace() 
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }

        super.onCreate()

        // Allow file:// URIs in intents.
        // Emulators (RetroArch, DuckStation, Redream, NetherSX2, etc.)
        // ALL expect file:// URIs — they don't understand content:// URIs.
        // This is the standard approach used by Android game launchers
        // like Daijisho, Lemuroid, etc.
        StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder().build())
    }
}
