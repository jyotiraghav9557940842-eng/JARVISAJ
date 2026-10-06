package com.jarvis.table

import android.accessibilityservice.AccessibilityService
import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.*
import android.speech.*
import android.speech.tts.TextToSpeech
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

class JarvisService : Service() {
    private var sr: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var awake = false
    private val h = Handler(Looper.getMainLooper())
    private val wake = Regex(
        "wake\\s*up\\s*(a\\.?\\s*j|aj|ej|age)|वेक\\s*अप\\s*(ए\\s*जे|एजे)|jarvis|जार्विस",
        RegexOption.IGNORE_CASE
    )
    private val PKG = mapOf(
        "youtube" to "com.google.android.youtube", "whatsapp" to "com.whatsapp",
        "chrome" to "com.android.chrome", "instagram" to "com.instagram.android",
        "maps" to "com.google.android.apps.maps", "gmail" to "com.google.android.gm",
        "settings" to "com.android.settings"
    )
    private val SYS = """You control an Android phone. Reply ONLY with a JSON array of steps, no other text.
Actions: {"action":"open_app","app":"youtube"} {"action":"click_text","text":"Search"} {"action":"type","text":"..."}
{"action":"enter"} {"action":"tap","x":0.5,"y":0.3} (x,y are screen fractions) {"action":"scroll_down"}
{"action":"back"} {"action":"home"} {"action":"speak","text":"..."}
Example for "youtube me claude ai search karke pehli video chalao":
[{"action":"open_app","app":"youtube"},{"action":"click_text","text":"Search"},{"action":"type","text":"claude ai"},{"action":"enter"},{"action":"tap","x":0.5,"y":0.3}]
The user speaks Hindi or English."""

    override fun onBind(i: Intent?): IBinder? = null

    override fun onStartCommand(i: Intent?, f: Int, id: Int): Int {
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(NotificationChannel("jt", "Jarvis", NotificationManager.IMPORTANCE_LOW))
        val n = Notification.Builder(this, "jt").setContentTitle("Jarvis is listening")
            .setContentText("Say: wake up AJ").setSmallIcon(android.R.drawable.ic_btn_speak_now).build()
        startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        tts = TextToSpeech(this) { tts?.language = Locale("hi", "IN") }
        listen()
        return START_STICKY
    }

    private fun say(t: String) { tts?.speak(t, TextToSpeech.QUEUE_FLUSH, null, null) }

    private fun listen() {
        sr?.destroy()
        sr = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onResults(r: Bundle?) {
                    handle(r?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: "")
                    h.postDelayed({ listen() }, 300)
                }
                override fun onError(e: Int) { h.postDelayed({ listen() }, 800) }
                override fun onReadyForSpeech(p: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(v: Float) {}
                override fun onBufferReceived(b: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onPartialResults(p: Bundle?) {}
                override fun onEvent(t: Int, p: Bundle?) {}
            })
            startListening(
                Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                    .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
            )
        }
    }

    private fun handle(t: String) {
        if (t.isBlank()) return
        if (!awake) {
            val m = wake.find(t) ?: return
            val rest = t.removeRange(m.range).trim()
            if (rest.length > 2) doCommand(rest)
            else { awake = true; say("Haan boliye"); h.postDelayed({ awake = false }, 10000) }
        } else { awake = false; doCommand(t) }
    }

    private fun doCommand(cmd: String) = Thread {
        try {
            val key = getSharedPreferences("jt", MODE_PRIVATE).getString("key", "") ?: ""
            val body = JSONObject().put("model", "claude-haiku-4-5-20251001").put("max_tokens", 600)
                .put("system", SYS)
                .put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", cmd)))
            val req = Request.Builder().url("https://api.anthropic.com/v1/messages")
                .header("x-api-key", key).header("anthropic-version", "2023-06-01")
                .post(body.toString().toRequestBody("application/json".toMediaType())).build()
            val res = OkHttpClient().newCall(req).execute().body!!.string()
            val txt = JSONObject(res).getJSONArray("content").getJSONObject(0).getString("text")
            val steps = JSONArray(txt.substring(txt.indexOf('['), txt.lastIndexOf(']') + 1))
            for (i in 0 until steps.length()) { exec(steps.getJSONObject(i)); Thread.sleep(1800) }
        } catch (e: Exception) { say("Error " + e.message) }
    }.start()

    private fun exec(s: JSONObject) {
        val a = JarvisAccess.instance
        when (s.getString("action")) {
            "open_app" -> {
                val p = PKG[s.getString("app").lowercase()] ?: s.getString("app")
                packageManager.getLaunchIntentForPackage(p)?.let {
                    startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }
            "type" -> a?.typeText(s.getString("text"))
            "enter" -> a?.enter()
            "click_text" -> a?.clickText(s.getString("text"))
            "tap" -> a?.tap(s.getDouble("x").toFloat(), s.getDouble("y").toFloat())
            "scroll_down" -> a?.scrollDown()
            "back" -> a?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
            "home" -> a?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
            "speak" -> say(s.getString("text"))
        }
    }

    override fun onDestroy() { sr?.destroy(); tts?.shutdown(); super.onDestroy() }
}
