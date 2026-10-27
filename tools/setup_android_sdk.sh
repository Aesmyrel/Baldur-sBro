#!/usr/bin/env bash
# Installe le SDK Android en ligne de commande dans .android-sdk/ (non versionné).
# Usage : bash tools/setup_android_sdk.sh   puis   export ANDROID_HOME=$PWD/.android-sdk
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SDK="$ROOT/.android-sdk"
TOOLS_ZIP="commandlinetools-linux-16111833_latest.zip"   # cmdline-tools 23.0, relevé le 27/10/2026

mkdir -p "$SDK/cmdline-tools"
if [ ! -x "$SDK/cmdline-tools/latest/bin/sdkmanager" ]; then
  tmp="$(mktemp -d)"
  curl -sSfL -o "$tmp/tools.zip" "https://dl.google.com/android/repository/$TOOLS_ZIP"
  unzip -q "$tmp/tools.zip" -d "$tmp"
  rm -rf "$SDK/cmdline-tools/latest"
  mv "$tmp/cmdline-tools" "$SDK/cmdline-tools/latest"
  rm -rf "$tmp"
fi

SDKMANAGER="$SDK/cmdline-tools/latest/bin/sdkmanager"
yes | "$SDKMANAGER" --sdk_root="$SDK" --licenses >/dev/null
"$SDKMANAGER" --sdk_root="$SDK" "platform-tools" "platforms;android-37.0" "build-tools;37.0.0"
echo "SDK prêt : export ANDROID_HOME=$SDK"
