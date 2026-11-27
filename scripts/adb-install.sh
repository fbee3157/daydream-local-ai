#!/usr/bin/env bash
# 回装到 Quest 3；分包应用需按 split APK 顺序安装
set -euo pipefail

APK="${1:?用法: ./adb-install.sh <apk> [split.apk ...]}"
shift || true

adb install -r "${APK}"
for split in "$@"; do
  adb install -r "${split}"
done

echo "==> 安装完成。模型 / 分词器 / 记忆文件放置于 /storage/emulated/0/DayDream/"
