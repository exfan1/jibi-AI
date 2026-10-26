# Jibi AI 0.5 — Cloud Build

نسخه‌ی آماده برای GitHub Actions و ساخت APK بدون لپ‌تاپ.

## ساخت APK از روی گوشی

1. یک Repository به نام `JibiAI` در GitHub بساز.
2. پوشه‌ی `JibiAI` داخل این پروژه را در ریشه‌ی Repository آپلود کن.
3. به تب **Actions** برو.
4. Workflow با نام **Android APK** را انتخاب کن.
5. روی **Run workflow** بزن.
6. پس از اتمام، در صفحه‌ی اجرای Workflow بخش **Artifacts** فایل `jibi-ai-debug-apk` را دانلود کن.

Workflow از JDK 17 و Gradle 8.9 استفاده می‌کند و `assembleDebug` را اجرا می‌کند.

## نکات امنیتی

- هیچ API key داخل سورس قرار نده.
- Repository را اگر نمی‌خواهی سورس عمومی باشد Private بساز.
- کلید AI فقط در تنظیمات محلی اپ وارد می‌شود.

## قابلیت‌های 0.5

- فرمان صوتی فارسی و TTS
- Planner چندمرحله‌ای
- Accessibility برای Home/Back/Recent/Notifications/Click/Type/Scroll
- Notification Listener
- تماس و SMS با تأیید کاربر
- دوربین، فلش، آلارم، وب و نقشه
- حافظه محلی
- اتصال اختیاری به API سازگار با OpenAI
- Self Test
