#!/usr/bin/env bash
# DayDream – AI Companion 本机逆向审计流程（仅限合法持有的副本，个人研究使用）。
# 流程：ADB 连接检查 → 包名定位 → APK/分包拉取 → apktool 解包 → 只读扫描审计清单。
# APK、解包目录、素材、模型权重均为本地私有产物；.gitignore 已排除，不提交仓库。
#
# 用法（Mac / Linux，已连接 Quest 3 并授权调试）：
#   ./scripts/reverse-audit.sh [包名]
# 不传包名时先列出含 dream 的候选包名，再交互式输入。
set -euo pipefail

WORK_DIR="${HOME}/QuestApk"
DECODE_DIR="${WORK_DIR}/daydream_decoded"
AUDIT_REPORT="${WORK_DIR}/audit-report.txt"

# ---------- 1. 工具与连接检查 ----------
require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "ERROR: 缺少命令 $1，请先安装（见 docs/reverse-workflow.md §1）"
    exit 1
  fi
}

echo "==> [1/6] 检查逆向工具链"
require_cmd adb
command -v apktool >/dev/null 2>&1 || echo "WARN: apktool 未安装；解包步骤会跳过（brew install apktool）"
command -v jadx >/dev/null 2>&1 || echo "WARN: jadx 未安装；Java/Smali 阅读需手动用 JADX 打开 APK"

echo "==> [2/6] 检查 ADB 设备"
adb devices -l
DEVICE_COUNT="$(adb devices | tail -n +2 | grep -v '^$' | grep -c 'device$' || true)"
if [ "${DEVICE_COUNT}" -eq 0 ]; then
  echo "ERROR: 未检测到已授权的 Quest 3 设备。请连接头显、开启开发者模式并在头显上授权调试。"
  exit 1
fi

# ---------- 2. 包名定位 ----------
PKG="${1:-}"
if [ -z "${PKG}" ]; then
  echo "==> 候选包名（关键词 dream，以真机实际输出为准）"
  adb shell pm list packages | grep -i dream || echo "(无匹配，可手动传入完整包名)"
  read -r -p "输入目标包名: " PKG
fi
[ -z "${PKG}" ] && { echo "ERROR: 包名为空"; exit 1; }
echo "==> 目标包名: ${PKG}"

# ---------- 3. 拉取 APK 与分包 ----------
mkdir -p "${WORK_DIR}"
echo "==> [3/6] 查询 APK 安装路径并拉取到 ${WORK_DIR}"
adb shell pm path "${PKG}" | sed 's/^package://' | while read -r line; do
  [ -z "${line}" ] && continue
  fname="$(basename "${line}")"
  echo "    pull ${line} -> ${fname}"
  adb pull "${line}" "${WORK_DIR}/${fname}"
done
ls -lh "${WORK_DIR}"

# ---------- 4. apktool 解包 ----------
BASE_APK="$(find "${WORK_DIR}" -maxdepth 1 -name 'base.apk' -o -name '*.apk' | head -n 1 || true)"
if [ -z "${BASE_APK}" ]; then
  echo "ERROR: 未找到 APK 文件，停止解包。"
  exit 1
fi

if command -v apktool >/dev/null 2>&1; then
  echo "==> [4/6] apktool 解包: ${BASE_APK} -> ${DECODE_DIR}"
  apktool d -f "${BASE_APK}" -o "${DECODE_DIR}"
else
  echo "==> [4/6] 跳过解包（apktool 未安装）；可后续手动执行: apktool d ${BASE_APK} -o ${DECODE_DIR}"
fi

# ---------- 5. 只读扫描审计清单 ----------
echo "==> [5/6] 生成只读审计清单: ${AUDIT_REPORT}"
: > "${AUDIT_REPORT}"

append() { echo "$1" >> "${AUDIT_REPORT}"; }

append "DayDream 逆向审计清单"
append "包名: ${PKG}"
append "生成时间: $(date '+%Y-%m-%d %H:%M:%S %z')"
append ""

if [ -d "${DECODE_DIR}" ]; then
  append "### 原生库（lib 目录）"
  find "${DECODE_DIR}/lib" -type f -name '*.so' 2>/dev/null | sort >> "${AUDIT_REPORT}" || append "(无 lib/*.so)"
  append ""

  append "### Smali 类目录（前两层）"
  find "${DECODE_DIR}/smali" -maxdepth 3 -type d 2>/dev/null | sort | head -n 200 >> "${AUDIT_REPORT}" || append "(无 smali 目录)"
  append ""

  append "### 网络 / 对话 / 语音 / 场景关键词命中（Smali 与资源）"
  grep -RIlE 'https?://|Retrofit|OkHttp|WebSocket|chat|dialog|message|voice|tts|stt|lip|scene|spawn|seed' \
    "${DECODE_DIR}/smali" "${DECODE_DIR}/assets" "${DECODE_DIR}/res" 2>/dev/null \
    | sort | head -n 300 >> "${AUDIT_REPORT}" || append "(无关键词命中)"
  append ""

  append "### 素材目录（仅列清单；版权素材本地私有，禁止入库 / 分发）"
  find "${DECODE_DIR}/assets" "${DECODE_DIR}/res" -maxdepth 4 -type d \
    \( -iname '*avatar*' -o -iname '*character*' -o -iname '*clothes*' -o -iname '*outfit*' \
       -o -iname '*scene*' -o -iname '*model*' -o -iname '*texture*' -o -iname '*voice*' \) \
    2>/dev/null | sort >> "${AUDIT_REPORT}" || append "(无素材目录命中)"
  append ""
else
  append "解包目录不存在；安装 apktool 后重新运行本脚本，或手动解包后查看 Smali / res / assets。"
fi

append "### 待 JADX 人工核对项"
append "1. 对话输入框、发送按钮、气泡台词文本来源类与方法"
append "2. 语音录制 / 播放 / 口型动画触发点"
append "3. 云端请求类、域名、请求 / 响应结构"
append "4. lib/arm64-v8a 是否已有推理原生 .so（判断云端 / 本地推理）"
append "5. 角色外观、服装、场景素材所属资源目录"

# ---------- 6. 完成提示 ----------
echo "==> [6/6] 完成。审计清单: ${AUDIT_REPORT}"
echo "    注意：APK、解包产物与素材仅限本机个人研究；不要提交到 Git，不要对外分发。"
