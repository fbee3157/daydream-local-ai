#!/usr/bin/env bash
# Apktool 解包基础 APK
set -euo pipefail

APK="${1:?用法: ./apktool-decode.sh <base.apk> [输出目录]}"
OUT="${2:-daydream_decoded}"

apktool d "${APK}" -o "${OUT}"
echo "==> 解包完成: ${OUT}"
ls -la "${OUT}"
