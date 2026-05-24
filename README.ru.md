[Read this in English](README.md)

# AuriSteg Android

Визуализатор битовых плоскостей для Android — порт рендеринга [auristeg](https://github.com/muwrec/auristeg) на мобильную платформу через Rust JNI.

**Статус:** только визуализатор. Открыть изображение, посмотреть каналы R/G/B на каждом бите (LSB-0 … LSB-7), приближение/сдвиг двумя пальцами. Встраивание и извлечение сообщений позже.

| | |
|---|---|
| **Рендеринг** | Java `ImageView` + `Matrix` (зум/пан) |
| **Обработка** | Rust `cdylib` через JNI (`image` crate + `auristeg-core`) |
| **Файлы** | Android SAF (`ACTION_OPEN_DOCUMENT`) |
| **Min API** | 34 (Android 14) |

## Сборка

### Требования

- Linux x86\_64
- JDK 21+
- Android SDK / NDK r27 (`$HOME/Android/Sdk`)
- Rust + `aarch64-linux-android` target
- `curl` + `unzip` (для загрузки Gradle wrapper)

### Автоматически

```bash
./build-android.sh
# APK → app/build/outputs/apk/debug/app-debug.apk
```

### Вручную (по шагам)

```bash
# 1. Собрать Rust .so
cd rust
CARGO_TARGET_AARCH64_LINUX_ANDROID_LINKER="$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android26-clang" \
  cargo build --target aarch64-linux-android --release
cd ..

# 2. Скопировать в jniLibs
mkdir -p app/src/main/jniLibs/arm64-v8a
cp rust/target/aarch64-linux-android/release/libauristeg_bridge.so \
   app/src/main/jniLibs/arm64-v8a/

# 3. Собрать APK
export ANDROID_HOME="$HOME/Android/Sdk"
./gradlew assembleDebug
```


