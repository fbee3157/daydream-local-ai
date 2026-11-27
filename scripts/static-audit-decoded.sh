#!/usr/bin/env bash
# 对已解包的 DayDream APK 目录做离线静态审计（无需连接头显 / ADB）。
# 输出：清单报告（权限、导出组件、原生库 ABI、素材目录、关键词命中、待人工核对项）。
# 仅限本人合法持有的副本做个人研究；APK、解包产物与素材不入库、不分发。
#
# 用法：
#   ./scripts/static-audit-decoded.sh <解包目录> [报告输出路径]
set -euo pipefail

DECODED="${1:?用法: ./static-audit-decoded.sh <解包目录> [报告路径]}"
if [ ! -d "${DECODED}" ]; then
  echo "ERROR: 解包目录不存在: ${DECODED}"
  exit 1
fi

REPORT="${2:-$(dirname "${DECODED}")/static-audit-report.txt}"
: > "${REPORT}"

append() { echo "$1" >> "${REPORT}"; }

append "DayDream 解包静态审计报告"
append "解包目录: ${DECODED}"
append "生成时间: $(date '+%Y-%m-%d %H:%M:%S %z')"
append ""

# ---------- 1. AndroidManifest：权限与导出组件 ----------
MANIFEST="${DECODED}/AndroidManifest.xml"
append "### 1. 申请权限（uses-permission）"
if [ -f "${MANIFEST}" ]; then
  grep -oE 'uses-permission[^>]*android:name="[^"]+"' "${MANIFEST}" \
    | sed -E 's/.*android:name="([^"]+)"/\1/' | sort -u >> "${REPORT}" || append "(无权限声明)"
  append ""
  append "### 2. 导出组件标记（android:exported）"
  grep -oE 'android:exported="[^"]+"' "${MANIFEST}" | sort -u >> "${REPORT}" || append "(无 exported 标记)"
  append ""
else
  append "(未找到 AndroidManifest.xml)"
fi

# ---------- 3. 原生库与 ABI ----------
append "### 3. 原生库目录与 .so 文件"
if [ -d "${DECODED}/lib" ]; then
  find "${DECODED}/lib" -maxdepth 2 -type d | sort >> "${REPORT}"
  find "${DECODED}/lib" -type f -name '*.so' | sort >> "${REPORT}"
else
  append "(无 lib 目录：原版可能为纯托管代码 / 云端推理)"
fi
append ""

# ---------- 4. 素材与资源目录清单 ----------
append "### 4. 角色 / 服装 / 场景 / 语音素材目录（仅清单，版权素材本地私有）"
find "${DECODED}/assets" "${DECODED}/res" -maxdepth 4 -type d \
  \( -iname '*avatar*' -o -iname '*character*' -o -iname '*clothes*' -o -iname '*outfit*' \
     -o -iname '*scene*' -o -iname '*model*' -o -iname '*texture*' -o -iname '*voice*' -o -iname '*audio*' \) \
  2>/dev/null | sort >> "${REPORT}" || append "(无素材目录命中)"
append ""

# ---------- 5. 关键词命中：对话 / 语音 / 网络 / 场景 ----------
append "### 5. Smali / 资源 / 资产关键词命中文件"
grep -RIlE 'https?://|Retrofit|OkHttp|WebSocket|chat|dialog|message|voice|tts|stt|lip|scene|spawn|seed|avatar|outfit' \
  "${DECODED}/smali" "${DECODED}/assets" "${DECODED}/res" 2>/dev/null \
  | sort | head -n 300 >> "${REPORT}" || append "(无关键词命中)"
append ""

# ---------- 6. 待 JADX 人工核对项 ----------
append "### 6. 待人工核对项"
append "1. 对话输入框、发送按钮、气泡台词文本来源类与方法"
append "2. 语音录制 / 播放 / 口型动画触发点"
append "3. 云端请求类、域名、请求 / 响应结构（结合第 5 节命中文件）"
append "4. lib 下是否已有推理原生 .so；ABI 是否为 arm64-v8a（判断云端 / 本地推理）"
append "5. 角色外观、服装、场景素材所属资源目录与加载方式"
append "6. Manifest 中导出组件是否暴露了对话、场景或角色相关入口"

echo "==> 静态审计报告: ${REPORT}"
echo "    注意：解包产物与素材仅限本机个人研究；不要提交到 Git，不要对外分发。"
