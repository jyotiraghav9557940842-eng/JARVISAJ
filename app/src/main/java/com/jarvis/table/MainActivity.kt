package com.jarvis.table

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.*

class MainActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val sp = getSharedPreferences("jt", MODE_PRIVATE)
        val pad = (24 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
            setPadding(pad, pad * 2, pad, pad)
        }
        fun tv(t: String, s: Float) = TextView(this).apply { text = t; setTextColor(Color.WHITE); textSize = s }
        fun btn(t: String, f: () -> Unit) = Button(this).apply {
            text = t; setTextColor(Color.BLACK); setBackgroundColor(Color.WHITE); setOnClickListener { f() }
        }
        val key = EditText(this).apply {
            hint = "Claude API key"; setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE); setText(sp.getString("key", ""))
        }
        root.addView(tv("Jarvis Table", 32f))
        root.addView(key)
        root.addView(btn("1. Mic + notification permission") {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS), 1)
        })
        root.addView(btn("2. Accessibility: turn ON") { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) })
        root.addView(btn("3. Display over other apps") {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        })
        root.addView(btn("4. Battery: Unrestricted") { startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) })
        root.addView(btn("START Jarvis") {
            sp.edit().putString("key", key.text.toString().trim()).apply()
            startForegroundService(Intent(this, JarvisService::class.java))
        })
        root.addView(btn("STOP") { stopService(Intent(this, JarvisService::class.java)) })
        setContentView(ScrollView(this).apply { setBackgroundColor(Color.BLACK); addView(root) })
    }
}
