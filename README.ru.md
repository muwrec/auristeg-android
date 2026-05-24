[![English](https://img.shields.io/badge/lang-en-green.svg)](README.md)
[![Rust](https://img.shields.io/badge/rust-2021-orange.svg)](https://www.rust-lang.org)

# AuriSteg Android

Визуализатор битовых плоскостей для Android — порт рендеринга [auristeg](https://github.com/muwrec/auristeg) на мобильную платформу через **Rust JNI** (`cdylib`).

## Архитектура

```
auristeg-android/
├── rust/
│   └── src/lib.rs       # JNI мост: loadImage, fillBitmap, extractBitPlane
├── app/
│   ├── src/main/java/   # UI (MainActivity), SAF picker, ImageView зум/пан
│   └── src/main/res/    # Layout, темы, адаптивная иконка
├── build-android.sh     # Скрипт сборки
└── build.gradle.kts     # Gradle конфиг (minSdk 34, targetSdk 35)
```

- **`rust/`** — Rust `cdylib`. `jni 0.21`, `image 0.25`, `auristeg-core` (git). JNI-функции для загрузки, доступа к пикселям (zero-copy через `AndroidBitmap_lockPixels`), извлечения битовых плоскостей.
- **`app/`** — Стандартный Android-модуль. SAF file picker, `ImageView` + `Matrix` (пинч-зум, пан).

## Сборка

```bash
git clone https://github.com/muwrec/auristeg-android
cd auristeg-android
./build-android.sh
# APK → app/build/outputs/apk/debug/app-debug.apk
```

Требования: Linux, JDK 21+, Android SDK + NDK r27, Rust + `aarch64-linux-android`.

## Использование

- **Open Image** — кнопка папки, выбрать PNG/JPG/BMP
- **Ch** — переключение канала R/G/B
- **Bit** — выбор бита (LSB-0 … LSB-7)
- **Щипок** — зум
- **Один палец** — pan

## Статус

Только визуализатор. Встраивание и извлечение сообщений позже.

## Лицензия

MIT
