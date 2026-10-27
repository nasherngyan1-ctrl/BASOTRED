# OKX Sniper AI — سیستەمی زیرەکی سیگناڵ و شیکاری چارت

ئەپێکی تایبەت بە بۆرسەی **OKX** بۆ پشکنینی خۆکاری هەموو دراوەکان هەموو 5 خولەک جارێک، هەژمارکردنی قازانجی **Spot** و **Futures** بە دۆلار، و شیکاری وێنەی چارت بە زیرەکی دەستکردی **Google Gemini**.

## چۆنیەتی دابەزاندنی فایلی APK لە GitHub

هەموو ڕێکخستنەکانی **GitHub Actions Workflow**، **Android SDK 36.1**، **Gradle 9.3.1** و **Keystore** لەناو فایلی `.github/workflows/build-apk.yml` ئامادە کراون:

1. پڕۆژەکە بنێرە بۆ **GitHub** (Push to GitHub).
2. لەناو پەڕەی GitHubـەکەت بڕۆ بۆ بەشی **Actions** یان بەشی **Releases**.
3. دوای تەواوبوونی کارەکە (نزیکەی 2 بۆ 3 خولەک):
   - لە بەشی **Releases** دەتوانیت ڕاستەوخۆ فایلی **`OKX-Sniper-AI.apk`** دابەزێنیتە ناو مۆبایلەکەت.
   - یان لەناو **Actions -> Build Android APK -> Artifacts** فایلی **`OKX-Sniper-AI-APK`** دابەزێنیت.
