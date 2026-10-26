package com.jibi.ai.core

import com.jibi.ai.actions.Action
import java.util.Locale

class Planner {
    private val apps = mapOf(
        "تلگرام" to ("org.telegram.messenger" to "تلگرام"),
        "واتساپ" to ("com.whatsapp" to "واتساپ"),
        "یوتیوب" to ("com.google.android.youtube" to "یوتیوب"),
        "کروم" to ("com.android.chrome" to "کروم")
    )

    fun plan(raw: String): List<Action> {
        val input = raw.trim()
        if (input.isBlank()) return emptyList()
        val parts = input.split(Regex("\\s*(?:و سپس|سپس|بعدش|بعد از آن|بعد)\\s*"), limit = 12)
            .map { it.trim() }.filter { it.isNotBlank() }
        return parts.flatMap { parseOne(it) }
    }

    private fun parseOne(cmd: String): List<Action> {
        val l = cmd.lowercase(Locale("fa"))
        if (l.startsWith("یادت باشه") || l.startsWith("یاد بگیر"))
            return listOf(Action.Remember(cmd.replaceFirst(Regex("^یادت باشه|^یاد بگیر"), "").trim()))
        if (l.contains("چی یادت") || l == "حافظه") return listOf(Action.RecallMemory)
        if (l.contains("اعلان هام") || l.contains("اعلان‌ها") || l.contains("اعلان های من") || l.contains("نوتیفیکیشن")) return listOf(Action.ReadNotifications)
        if (l.contains("صفحه اصلی") || l.contains("هوم")) return listOf(Action.Home)
        if (l.contains("برگرد") || l.contains("بازگشت")) return listOf(Action.Back)
        if (l.contains("برنامه های اخیر") || l.contains("برنامه‌های اخیر")) return listOf(Action.Recents)
        if (l.contains("اعلان های گوشی") || l.contains("اعلان‌های گوشی")) return listOf(Action.Notifications)
        if (l.contains("تنظیمات وای فای") || l.contains("وای فای") || l.contains("wifi")) return listOf(Action.WifiSettings)
        if (l.contains("تنظیمات بلوتوث") || l.contains("بلوتوث")) return listOf(Action.BluetoothSettings)
        if (l == "تنظیمات" || l.contains("تنظیمات گوشی")) return listOf(Action.Settings)
        apps.entries.firstOrNull { l.contains(it.key) }?.let { return listOf(Action.OpenApp(it.value.first, it.value.second)) }
        if (l.startsWith("بزن روی ") || l.startsWith("روی ")) {
            val t = cmd.substringAfter("روی ").removePrefix("بزن ").trim()
            return if (t.isBlank()) listOf(Action.AskAI(cmd)) else listOf(Action.ClickText(t))
        }
        if (l.contains("اسکرول پایین") || l.contains("پایین برو")) return listOf(Action.Scroll(true))
        if (l.contains("اسکرول بالا") || l.contains("بالا برو")) return listOf(Action.Scroll(false))
        Regex("(?:متن را بنویس|تایپ کن)\\s+(.+)", RegexOption.IGNORE_CASE).find(cmd)?.let { return listOf(Action.TypeText(it.groupValues[1])) }
        if (l.contains("دوربین")) return listOf(Action.Camera)
        if (l.contains("چراغ") || l.contains("فلاش")) return listOf(Action.ToggleFlash)
        if (l.contains("جستجو") || l.contains("گوگل")) return listOf(Action.WebSearch(cmd.substringAfter("جستجو", cmd.substringAfter("گوگل", cmd)).trim()))
        if (l.contains("نقشه") || l.contains("مسیر")) return listOf(Action.MapsSearch(cmd))
        if (l.contains("آلارم")) {
            val m = Regex("(\\d{1,2})(?::(\\d{1,2}))?").find(cmd)
            val h = m?.groupValues?.getOrNull(1)?.toIntOrNull()
            val min = m?.groupValues?.getOrNull(2)?.toIntOrNull() ?: 0
            if (h != null) return listOf(Action.Alarm(h.coerceIn(0,23), min.coerceIn(0,59)))
        }
        if (l.contains("تماس") || l.contains("زنگ بزن")) {
            val n = Regex("[+]?\\d[\\d\\s-]{5,}").find(cmd)?.value?.replace(" ", "")
            return listOf(Action.Dial(n))
        }
        if (l.contains("پیام") || l.contains("اس ام اس")) return listOf(Action.Sms(cmd.substringAfter("پیام", "").trim()))
        return listOf(Action.AskAI(cmd))
    }
}
