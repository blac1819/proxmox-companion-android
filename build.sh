#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "============================================="
echo " Building Proxmox Companion Android App"
echo "============================================="

ANDROID_JAR="/data/user/0/com.agy/files/usr/share/android-sdk/platforms/android-34/android.jar"
KEYSTORE="$SCRIPT_DIR/debug.keystore"
BUILD_DIR="$SCRIPT_DIR/build"
DIST_DIR="$SCRIPT_DIR/dist"
OUTPUT_APK="$DIST_DIR/ProxmoxCompanion.apk"
SHARED_STORAGE_APK="/storage/emulated/0/Download/ProxmoxCompanion.apk"

# Check required binaries and files
for tool in aapt2 d8 apksigner javac zip; do
    if ! command -v "$tool" &> /dev/null; then
        echo "Error: Required tool '$tool' not found in PATH." >&2
        exit 1
    fi
done

if [ ! -f "$ANDROID_JAR" ]; then
    echo "Error: android.jar not found at $ANDROID_JAR" >&2
    exit 1
fi

if [ ! -f "$KEYSTORE" ]; then
    echo "Generating debug keystore..."
    keytool -genkeypair -v -keystore "$KEYSTORE" \
        -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 \
        -storepass android -keypass android -dname "CN=Android Debug,O=Android,C=US"
fi

# Clean previous build artifacts
echo "[1/6] Cleaning build directories..."
rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR/gen" "$BUILD_DIR/classes" "$BUILD_DIR/dex" "$DIST_DIR"

# Step 1: Compile Android resources
echo "[2/6] Compiling resources with aapt2..."
aapt2 compile --dir src/main/res -o "$BUILD_DIR/resources.zip"

# Step 2: Link Android resources and generate R.java
echo "[3/6] Linking resources and generating R.java..."
aapt2 link -I "$ANDROID_JAR" \
    --manifest src/main/AndroidManifest.xml \
    -o "$BUILD_DIR/base.apk" \
    --java "$BUILD_DIR/gen" \
    "$BUILD_DIR/resources.zip" \
    --auto-add-overlay

# Step 3: Compile Java sources
echo "[4/6] Compiling Java source files..."
find src/main/java "$BUILD_DIR/gen" -name "*.java" > "$BUILD_DIR/sources.txt"
javac -d "$BUILD_DIR/classes" \
    -cp "$ANDROID_JAR" \
    -source 1.8 -target 1.8 \
    -encoding UTF-8 \
    @"$BUILD_DIR/sources.txt"

# Step 4: Convert Java bytecode to Dalvik bytecode (D8)
echo "[5/6] Dexing with D8..."
find "$BUILD_DIR/classes" -name "*.class" > "$BUILD_DIR/classes.txt"
d8 --output "$BUILD_DIR/dex" \
    --lib "$ANDROID_JAR" \
    --min-api 26 \
    @"$BUILD_DIR/classes.txt"

# Add classes.dex into APK
cd "$BUILD_DIR/dex"
zip -uj "$BUILD_DIR/base.apk" classes.dex
cd "$SCRIPT_DIR"

# Step 5: Sign the APK
echo "[6/6] Signing APK with apksigner..."
apksigner sign \
    --ks "$KEYSTORE" \
    --ks-pass pass:android \
    --ks-key-alias androiddebugkey \
    --key-pass pass:android \
    --out "$OUTPUT_APK" \
    "$BUILD_DIR/base.apk"

# Verify signature
echo "Verifying APK signature..."
apksigner verify --verbose "$OUTPUT_APK"

# Copy to shared storage for easy installation
if [ -d "/storage/emulated/0/Download" ]; then
    cp -f "$OUTPUT_APK" "$SHARED_STORAGE_APK"
    echo "✓ APK also copied to: $SHARED_STORAGE_APK"
fi

echo "============================================="
echo " BUILD SUCCESSFUL!"
echo " Output: $OUTPUT_APK"
echo " Size: $(du -h "$OUTPUT_APK" | cut -f1)"
echo "============================================="
