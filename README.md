[![Русский](https://img.shields.io/badge/lang-ru-blue.svg)](README.ru.md)
[![Rust](https://img.shields.io/badge/rust-2021-orange.svg)](https://www.rust-lang.org)

# AuriSteg Android

Bit plane visualizer for Android — port of the [auristeg](https://github.com/muwrec/auristeg) rendering stack to mobile via **Rust JNI** (`cdylib`).

## Architecture

```
auristeg-android/
├── rust/
│   └── src/lib.rs       # JNI bridge: loadImage, fillBitmap, extractBitPlane
├── app/
│   ├── src/main/java/   # UI (MainActivity), SAF picker, ImageView zoom/pan
│   └── src/main/res/    # Layout, themes, adaptive icon
├── build-android.sh     # One-shot build script
└── build.gradle.kts     # Android Gradle config (minSdk 34, targetSdk 35)
```

- **`rust/`** — Rust `cdylib`. Uses `jni 0.21`, `image 0.25`, and `auristeg-core` (git dependency). Exports JNI functions for image loading, RGBA pixel access (zero-copy via `AndroidBitmap_lockPixels`), and bit-plane extraction.
- **`app/`** — Standard Android app module. Java UI with `ActivityResultContracts` SAF file picker, `ImageView` + `Matrix` gestures (pinch zoom, pan).

## Build

```bash
git clone https://github.com/muwrec/auristeg-android
cd auristeg-android
./build-android.sh
# APK → app/build/outputs/apk/debug/app-debug.apk
```

Requirements: Linux, JDK 21+, Android SDK + NDK r27, Rust with `aarch64-linux-android` target.

## Usage

- **Open Image** — tap the folder button, pick a PNG/JPG/BMP
- **Ch** — switch R/G/B channel
- **Bit** — select bit plane (LSB-0 … LSB-7)
- **Pinch** — zoom
- **Drag (one finger)** — pan

## Status

Visualizer only. Embedding and extraction will follow.

## License

MIT
