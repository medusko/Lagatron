#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
APP_NAME="Lagatron"
BUNDLE_DIR="$SCRIPT_DIR/build/${APP_NAME}.app"
CONTENTS_DIR="$BUNDLE_DIR/Contents"
MACOS_DIR="$CONTENTS_DIR/MacOS"
RESOURCES_DIR="$CONTENTS_DIR/Resources"

echo "==> Compiling LagatronMac (Release)..."
cd "$SCRIPT_DIR"
swift build -c release

BIN_PATH="$(swift build -c release --show-bin-path)"
EXECUTABLE="$BIN_PATH/LagatronMac"

echo "==> Creating ${APP_NAME}.app bundle..."
rm -rf "$BUNDLE_DIR"
mkdir -p "$MACOS_DIR" "$RESOURCES_DIR"

cp "$EXECUTABLE" "$MACOS_DIR/$APP_NAME"
chmod +x "$MACOS_DIR/$APP_NAME"

# Copy SwiftPM resource bundle if generated
if [ -d "$BIN_PATH/LagatronMac_LagatronMac.bundle" ]; then
    cp -R "$BIN_PATH/LagatronMac_LagatronMac.bundle" "$RESOURCES_DIR/"
fi

# Also copy Poppins fonts directly into Contents/Resources
cp "$SCRIPT_DIR/Sources/LagatronMac/Resources/poppins_regular.ttf" "$RESOURCES_DIR/"
cp "$SCRIPT_DIR/Sources/LagatronMac/Resources/poppins_bold.ttf" "$RESOURCES_DIR/"

# Generate AppIcon.icns from Android ic_launcher-web.png if available
SRC_ICON="$REPO_ROOT/app/src/main/ic_launcher-web.png"
if [ -f "$SRC_ICON" ] && command -v sips >/dev/null 2>&1 && command -v iconutil >/dev/null 2>&1; then
    ICONSET_DIR="$SCRIPT_DIR/build/AppIcon.iconset"
    rm -rf "$ICONSET_DIR"
    mkdir -p "$ICONSET_DIR"
    sips -z 16 16     "$SRC_ICON" --out "$ICONSET_DIR/icon_16x16.png" >/dev/null 2>&1
    sips -z 32 32     "$SRC_ICON" --out "$ICONSET_DIR/icon_16x16@2x.png" >/dev/null 2>&1
    sips -z 32 32     "$SRC_ICON" --out "$ICONSET_DIR/icon_32x32.png" >/dev/null 2>&1
    sips -z 64 64     "$SRC_ICON" --out "$ICONSET_DIR/icon_32x32@2x.png" >/dev/null 2>&1
    sips -z 128 128   "$SRC_ICON" --out "$ICONSET_DIR/icon_128x128.png" >/dev/null 2>&1
    sips -z 256 256   "$SRC_ICON" --out "$ICONSET_DIR/icon_128x128@2x.png" >/dev/null 2>&1
    sips -z 256 256   "$SRC_ICON" --out "$ICONSET_DIR/icon_256x256.png" >/dev/null 2>&1
    sips -z 512 512   "$SRC_ICON" --out "$ICONSET_DIR/icon_256x256@2x.png" >/dev/null 2>&1
    sips -z 512 512   "$SRC_ICON" --out "$ICONSET_DIR/icon_512x512.png" >/dev/null 2>&1
    iconutil -c icns "$ICONSET_DIR" -o "$RESOURCES_DIR/AppIcon.icns" || true
    rm -rf "$ICONSET_DIR"
fi

# Create Info.plist
cat > "$CONTENTS_DIR/Info.plist" <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>CFBundleDevelopmentRegion</key>
    <string>en</string>
    <key>CFBundleExecutable</key>
    <string>${APP_NAME}</string>
    <key>CFBundleIconFile</key>
    <string>AppIcon</string>
    <key>CFBundleIdentifier</key>
    <string>com.medusko.lagatron.mac</string>
    <key>CFBundleInfoDictionaryVersion</key>
    <string>6.0</string>
    <key>CFBundleName</key>
    <string>${APP_NAME}</string>
    <key>CFBundlePackageType</key>
    <string>APPL</string>
    <key>CFBundleShortVersionString</key>
    <string>1.1-RC1</string>
    <key>CFBundleVersion</key>
    <string>3</string>
    <key>LSMinimumSystemVersion</key>
    <string>13.0</string>
    <key>LSUIElement</key>
    <true/>
    <key>NSHighResolutionCapable</key>
    <true/>
</dict>
</plist>
EOF

echo "==> Built macOS app bundle at: $BUNDLE_DIR"
