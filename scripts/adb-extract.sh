#!/usr/bin/env bash
# 提取 DayDream 原版 APK / 分包（Mac + Quest 3 + ADB）
set -euo pipefail

echo "==> 检查 ADB 设备连接"
adb devices

echo "==> 列出含 dream 的包名（按需修改关键词）"
adb shell pm list packages | grep -i dream || true

read -r -p "输入包名: " PKG
adb shell pm path "${PKG}"

mkdir -p "${HOME}/QuestApk"
cd "${HOME}/QuestApk"

# 对 pm path 返回的每一行分别拉取；分包逐行保存
adb shell pm path "${PKG}" | sed 's/^package://' | while read -r line; do
  [ -z "${line}" ] && continue
  fname="$(basename "${line}")"
  echo "==> 拉取 ${line} -> ${fname}"
  adb pull "${line}" "./${fname}"
done

ls -lh "${HOME}/QuestApk"
