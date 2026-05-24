[Read this in Русский](README.ru.md)

# AuriSteg Android

Bit plane visualizer for Android — port of [auristeg](https://github.com/muwrec/auristeg) rendering stack to mobile via Rust JNI.

**Status:** visualizer only. Open an image, inspect R/G/B channels at each bit depth (LSB-0 … LSB-7), pinch-to-zoom and pan. Embedding/extraction coming later.

| | |
|---|---|
| **Rendering** | Java `ImageView` + `Matrix` (zoom/pan) |
| **Processing** | Rust `cdylib` via JNI (`image` crate + `auristeg-core`) |
| **File access** | Android SAF (`ACTION_OPEN_DOCUMENT`) |
| **Min API** | 34 (Android 14) |

## Build

### Requirements

- Linux x86\_64
- JDK 21+
- Android SDK / NDK r27 (`$HOME/Android/Sdk`)
- Rust with `aarch64-linux-android` target
- `curl` + `unzip` (for Gradle wrapper bootstrap)

### One-shot

```bash
./build-android.sh
# APK → app/build/outputs/apk/debug/app-debug.apk
```

### Manual steps

```bash
# 1. Build Rust .so
cd rust
CARGO_TARGET_AARCH64_LINUX_ANDROID_LINKER="$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android26-clang" \
  cargo build --target aarch64-linux-android --release
cd ..

# 2. Copy to jniLibs
mkdir -p app/src/main/jniLibs/arm64-v8a
cp rust/target/aarch64-linux-android/release/libauristeg_bridge.so \
   app/src/main/jniLibs/arm64-v8a/

# 3. Build APK
export ANDROID_HOME="$HOME/Android/Sdk"
./gradlew assembleDebug
```

## JNI API

| Method | Returns | Description |
|---|---|---|
| `loadImage(byte[])` | `long` | Decode image, returns handle |
| `getWidth(long)` / `getHeight(long)` | `int` | Image dimensions |
| `fillBitmap(long, Bitmap)` | `int` | Write RGBA directly into Bitmap pixels (zero-copy) |
| `extractBitPlane(long, channel, bit)` | `byte[]` | Grayscale bit plane (0 or 255) |
| `freeImage(long)` | `void` | Release image from cache |

## License

MIT
