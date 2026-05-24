#!/usr/bin/env bash
set -euo pipefail

ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
NDK_HOME="${ANDROID_NDK_HOME:-$ANDROID_HOME/ndk/27.0.12077973}"
RUST_TARGET="aarch64-linux-android"
API_LEVEL=26

ROOT="$(cd "$(dirname "$0")" && pwd)"

echo "=== 1. Build Rust library ==="
cd "$ROOT/rust"

if ! rustup target list --installed | grep -q "$RUST_TARGET"; then
    rustup target add "$RUST_TARGET"
fi

LINKER="$NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin/${RUST_TARGET}${API_LEVEL}-clang"
export "CARGO_TARGET_AARCH64_LINUX_ANDROID_LINKER=$LINKER"
cargo build --target "$RUST_TARGET" --release
cd "$ROOT"

echo "=== 2. Copy .so to jniLibs ==="
mkdir -p app/src/main/jniLibs/arm64-v8a
cp "rust/target/$RUST_TARGET/release/libauristeg_bridge.so" \
   app/src/main/jniLibs/arm64-v8a/

echo "=== 3. Ensure Gradle wrapper ==="
if [ ! -f gradlew ]; then
    echo "Downloading Gradle wrapper..."
    GRADLE_VER=8.9
    ZIP="/tmp/gradle-${GRADLE_VER}-bin.zip"
    curl -sL "https://services.gradle.org/distributions/gradle-${GRADLE_VER}-bin.zip" -o "$ZIP"
    cd /tmp
    unzip -qo "$ZIP"
    cd "$ROOT"
    /tmp/gradle-${GRADLE_VER}/bin/gradle wrapper --gradle-version "$GRADLE_VER"
    rm -rf "/tmp/gradle-${GRADLE_VER}" "$ZIP"
    chmod +x gradlew
    ./gradlew --stop 2>/dev/null || true
    rm -rf "$HOME/.gradle/wrapper/dists/gradle-${GRADLE_VER}-bin*" 2>/dev/null || true
fi

echo "=== 4. Build APK ==="
export ANDROID_HOME
./gradlew assembleDebug

echo "=== Done ==="
echo "APK: $ROOT/app/build/outputs/apk/debug/app-debug.apk"
