#!/usr/bin/env bash
# 回编译 -> 对齐 -> 签名
set -euo pipefail

DECODED_DIR="${1:?用法: ./repackage-sign.sh <解包目录> [密钥库] [别名]}"
KS="${2:-debug.keystore}"
ALIAS="${3:-debug}"

apktool b "${DECODED_DIR}" -o daydream_modified.apk
zipalign -f -v 4 daydream_modified.apk daydream_aligned.apk
apksigner sign --ks "${KS}" --ks-key-alias "${ALIAS}" daydream_aligned.apk
apksigner verify --verbose daydream_aligned.apk

echo "==> 签名完成: daydream_aligned.apk"
