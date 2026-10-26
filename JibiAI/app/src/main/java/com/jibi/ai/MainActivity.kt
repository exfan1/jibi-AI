package com.jibi.ai

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Bundle
import android.provider.AlarmClock
import android.provider.Settings
import android.speech.*
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.widget.*
import android.text.InputType
import com.jibi.ai.actions.Action
import com.jibi.ai.core.Planner
import com.jibi.ai.services.JibiAccessibilityService
import com.jibi.ai.services.JibiNotificationListener
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private lateinit var input: EditText
    private lateinit var log: TextView
    private lateinit var tts: TextToSpeech
    private val planner = Planner()
    private var recognizer: SpeechRecognizer? = null
    private val prefs by lazy { getSharedPreferences("jibi", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        buildUi()
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 10)
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 24, 24, 18); setBackgroundColor(Color.rgb(246,247,251)) }
        val title = TextView(this).apply { text = "Jibi AI 0.5"; textSize = 28f; gravity = Gravity.CENTER; setPadding(0, 8, 0, 16) }
        log = TextView(this).apply { text = "آماده‌ام.\nمثلاً بگو: تلگرام را باز کن و روی جستجو بزن"; textSize = 16f; setPadding(12,12,12,12) }
        val scroll = ScrollView(this).apply { addView(log) }
        input = EditText(this).apply { hint = "فرمان را بنویس…" }
        val send = Button(this).apply { text = "اجرا"; setOnClickListener { execute(input.text.toString()) } }
        val mic = Button(this).apply { text = "🎙️ صحبت کن"; setOnClickListener { listen() } }
        val ai = Button(this).apply { text = "🧠 تنظیم موتور AI"; setOnClickListener { aiSettings() } }
        val access = Button(this).apply { text = "♿ فعال‌سازی کنترل گوشی"; setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) } }
        val notif = Button(this).apply { text = "🔔 فعال‌سازی اعلان‌ها"; setOnClickListener { startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")) } }
        root.addView(title); root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f)); root.addView(input)
        val test = Button(this).apply { text = "🧪 تست وضعیت Jibi"; setOnClickListener { selfTest() } }
        root.addView(send); root.addView(mic); root.addView(ai); root.addView(access); root.addView(notif); root.addView(test)
        setContentView(root)
    }

    private fun listen() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) return reply("تشخیص گفتار روی این دستگاه در دسترس نیست.")
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(this).also { r ->
            r.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) { log.text = "🎙️ گوش می‌دم…" }
                override fun onResults(results: Bundle?) {
                    val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                    input.setText(text); execute(text)
                }
                override fun onError(error: Int) { log.text = "تشخیص صدا انجام نشد ($error)" }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            r.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa-IR")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            })
        }
    }

    private fun execute(raw: String) {
        val command = raw.trim(); if (command.isBlank()) return
        log.text = "تو: $command\n\nJibi: در حال اجرا…"
        val actions = planner.plan(command)
        if (actions.isEmpty()) return reply("فرمانی دریافت نکردم.")
        runActions(actions)
    }

    private fun runActions(actions: List<Action>) {
        var delay = 0L
        actions.forEachIndexed { index, action ->
            window.decorView.postDelayed({
                val last = index == actions.lastIndex
                perform(action, last)
            }, delay)
            delay += if (action is Action.OpenApp || action is Action.ClickText || action is Action.TypeText) 1100L else 450L
        }
    }

    private fun perform(action: Action, last: Boolean) {
        when (action) {
            is Action.OpenApp -> { launch(action.packageName); if (last) reply("${action.label} را باز کردم.") }
            is Action.ClickText -> { val ok = JibiAccessibilityService.instance?.clickText(action.text) == true; if (last) reply(if (ok) "روی «${action.text}» زدم." else "«${action.text}» پیدا نشد.") }
            is Action.TypeText -> { val ok = JibiAccessibilityService.instance?.typeText(action.text) == true; if (last) reply(if (ok) "متن را وارد کردم." else "کادر قابل ویرایش پیدا نشد.") }
            is Action.Scroll -> { val ok = JibiAccessibilityService.instance?.scroll(action.down) == true; if (last) reply(if (ok) "اسکرول انجام شد." else "اسکرول ممکن نبود.") }
            Action.Home -> { JibiAccessibilityService.instance?.home(); if (last) reply("صفحه اصلی باز شد.") }
            Action.Back -> { JibiAccessibilityService.instance?.back(); if (last) reply("برگشتم.") }
            Action.Recents -> { JibiAccessibilityService.instance?.recents(); if (last) reply("برنامه‌های اخیر باز شد.") }
            Action.Notifications -> { JibiAccessibilityService.instance?.notifications(); if (last) reply("پنل اعلان‌ها باز شد.") }
            Action.Settings -> { startActivity(Intent(Settings.ACTION_SETTINGS)); if (last) reply("تنظیمات باز شد.") }
            Action.WifiSettings -> { startActivity(Intent(Settings.ACTION_WIFI_SETTINGS)); if (last) reply("تنظیمات وای‌فای باز شد.") }
            Action.BluetoothSettings -> { startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)); if (last) reply("تنظیمات بلوتوث باز شد.") }
            is Action.WebSearch -> { webSearch(action.query); if (last) reply("نتایج جستجو را باز کردم.") }
            is Action.MapsSearch -> { mapsSearch(action.query); if (last) reply("نقشه را باز کردم.") }
            is Action.Dial -> confirmSensitive("تماس", "صفحه تماس برای ${action.number ?: "شماره نامشخص"} باز شود؟") { dial(action.number); if (last) reply("صفحه تماس باز شد؛ تماس نهایی با خودت است.") }
            is Action.Sms -> confirmSensitive("پیام", "صفحه پیام باز شود؟\n\n${action.body}") { sms(action.body); if (last) reply("صفحه پیام آماده شد؛ ارسال نهایی با خودت است.") }
            is Action.Alarm -> { alarm(action.hour, action.minute); if (last) reply("آلارم را آماده کردم.") }
            Action.Camera -> { startActivity(Intent("android.media.action.IMAGE_CAPTURE")); if (last) reply("دوربین باز شد.") }
            Action.ToggleFlash -> toggleFlash(last)
            is Action.Remember -> { saveMemory(action.value); if (last) reply("یادداشت شد.") }
            Action.RecallMemory -> if (last) reply(memoryText())
            Action.ReadNotifications -> if (last) reply(notificationText())
            is Action.AskAI -> askAI(action.prompt)
        }
    }

    private fun saveMemory(value: String) { if (value.isBlank()) return; val s = prefs.getStringSet("memory", emptySet())!!.toMutableSet(); s.add(value); prefs.edit().putStringSet("memory", s).apply() }
    private fun notificationText(): String {
        val n = JibiNotificationListener.snapshot()
        return if (n.isEmpty()) "اعلان ذخیره‌شده‌ای ندارم. دسترسی اعلان را هم بررسی کن." else "آخرین اعلان‌ها:\n• " + n.take(8).joinToString("\n• ")
    }

    private fun memoryText(): String { val s = prefs.getStringSet("memory", emptySet())!!; return if (s.isEmpty()) "حافظه‌ام خالی است." else "چیزهایی که یادم هست:\n• " + s.joinToString("\n• ") }
    private fun launch(pkg: String) { try { startActivity(packageManager.getLaunchIntentForPackage(pkg) ?: throw Exception()) } catch (_: Exception) { reply("این برنامه روی گوشی نصب نیست.") } }
    private fun webSearch(q: String) { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + URLEncoder.encode(q, "UTF-8")))) }
    private fun mapsSearch(q: String) { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + URLEncoder.encode(q, "UTF-8")))) }
    private fun dial(number: String?) { startActivity(Intent(Intent.ACTION_DIAL).apply { data = Uri.parse("tel:${number.orEmpty()}") }) }
    private fun sms(body: String) { startActivity(Intent(Intent.ACTION_SENDTO).apply { data = Uri.parse("smsto:"); putExtra("sms_body", body) }) }
    private fun alarm(hour: Int, minute: Int) { startActivity(Intent(AlarmClock.ACTION_SET_ALARM).apply { putExtra(AlarmClock.EXTRA_HOUR, hour); putExtra(AlarmClock.EXTRA_MINUTES, minute); putExtra(AlarmClock.EXTRA_MESSAGE, "Jibi AI") }) }

    private fun toggleFlash(last: Boolean) {
        try {
            val cm = getSystemService(CAMERA_SERVICE) as CameraManager
            val id = cm.cameraIdList.firstOrNull { cm.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true }
                ?: return reply("فلش در دسترس نیست.")
            val on = !prefs.getBoolean("flash", false); cm.setTorchMode(id, on); prefs.edit().putBoolean("flash", on).apply()
            if (last) reply(if (on) "چراغ روشن شد." else "چراغ خاموش شد.")
        } catch (_: Exception) { if (last) reply("کنترل فلش انجام نشد.") }
    }

    private fun confirmSensitive(title: String, message: String, onYes: () -> Unit) {
        AlertDialog.Builder(this).setTitle(title).setMessage(message)
            .setPositiveButton("ادامه") { _, _ -> onYes() }
            .setNegativeButton("لغو", null).show()
    }

    private fun selfTest() {
        val audio = SpeechRecognizer.isRecognitionAvailable(this)
        val ttsOk = ::tts.isInitialized
        val accessibility = JibiAccessibilityService.instance != null
        val notifications = JibiNotificationListener.snapshot().isNotEmpty()
        val ai = prefs.getString("endpoint", "").orEmpty().isNotBlank() && prefs.getString("key", "").orEmpty().isNotBlank()
        val text = """تشخیص صدا: ${if (audio) "✅" else "❌"}
پاسخ صوتی: ${if (ttsOk) "✅" else "❌"}
کنترل Accessibility: ${if (accessibility) "✅ فعال" else "⚠️ فعال نشده"}
دسترسی اعلان: ${if (notifications) "✅ دریافت شده" else "⚠️ هنوز اعلانی دریافت نشده/دسترسی بررسی شود"}
موتور AI: ${if (ai) "✅ تنظیم شده" else "⚠️ تنظیم نشده"}

برای کنترل کامل گوشی، Accessibility را فعال کن. برای خواندن اعلان‌ها، Notification Access را فعال کن."""
        AlertDialog.Builder(this).setTitle("گزارش تست Jibi").setMessage(text).setPositiveButton("باشه", null).show()
    }

    private fun askAI(prompt: String) {
        val endpoint = prefs.getString("endpoint", "")!!.trim().trimEnd('/')
        val key = prefs.getString("key", "")!!.trim()
        val model = prefs.getString("model", "gpt-4o-mini")!!.trim()
        if (endpoint.isBlank() || key.isBlank()) return reply("برای فرمان‌های آزاد، موتور AI را در تنظیمات برنامه وصل کن.")
        Thread {
            try {
                val conn = URL("$endpoint/v1/chat/completions").openConnection() as HttpURLConnection
                conn.requestMethod = "POST"; conn.connectTimeout = 15000; conn.readTimeout = 30000; conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json"); conn.setRequestProperty("Authorization", "Bearer $key")
                val body = JSONObject().put("model", model).put("messages", JSONArray()
                    .put(JSONObject().put("role", "system").put("content", "تو Jibi AI هستی. فارسی و کوتاه جواب بده. برای عملیات حساس فقط راهنمایی کن و تأیید کاربر را لازم بدان."))
                    .put(JSONObject().put("role", "user").put("content", prompt)))
                conn.outputStream.use { it.write(body.toString().toByteArray()) }
                val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
                val response = stream.bufferedReader().readText()
                val answer = JSONObject(response).optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")?.optString("content").orEmpty().ifBlank { "پاسخ معتبری دریافت نشد." }
                runOnUiThread { reply(answer) }
            } catch (_: Exception) { runOnUiThread { reply("ارتباط با موتور AI برقرار نشد.") } }
        }.start()
    }

    private fun aiSettings() {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(28, 4, 28, 4) }
        val endpoint = EditText(this).apply { hint = "Endpoint (مثلاً https://... )"; setText(prefs.getString("endpoint", "")) }
        val key = EditText(this).apply { hint = "API Key"; setText(prefs.getString("key", "")); inputType = 0x81 }
        val model = EditText(this).apply { hint = "Model"; setText(prefs.getString("model", "gpt-4o-mini")) }
        box.addView(endpoint); box.addView(key); box.addView(model)
        AlertDialog.Builder(this).setTitle("اتصال موتور AI").setMessage("یک سرویس سازگار با OpenAI Chat Completions وارد کن. کلید فقط در تنظیمات محلی برنامه ذخیره می‌شود.")
            .setView(box).setPositiveButton("ذخیره") { _, _ -> prefs.edit().putString("endpoint", endpoint.text.toString()).putString("key", key.text.toString()).putString("model", model.text.toString()).apply(); reply("تنظیمات AI ذخیره شد.") }
            .setNegativeButton("لغو", null).show()
    }

    private fun reply(text: String) { log.text = log.text.toString() + "\n\nJibi: $text"; if (::tts.isInitialized) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jibi") }
    override fun onInit(status: Int) { if (status == TextToSpeech.SUCCESS) { if (tts.setLanguage(Locale("fa", "IR")) < 0) tts.language = Locale.US } }
    override fun onDestroy() { recognizer?.destroy(); tts.shutdown(); super.onDestroy() }
}
