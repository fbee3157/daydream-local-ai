#!/usr/bin/env bash
# NDK 编译推理库（arm64-v8a，Quest 3）。框架源码与 CMake 工程按所选引擎准备。
# 说明：本脚本为流程骨架；实际编译参数依 llama.cpp / MNN / ExecuTorch 官方 NDK 文档配置。
set -euo pipefail

ENGINE="${1:-llama.cpp}"
BUILD_DIR="${2:-build-${ENGINE}}"

echo "==> 推理引擎: ${ENGINE}"
echo "==> 构建目录: ${BUILD_DIR}"
echo "==> 目标 ABI: arm64-v8a (Quest 3 / XR2+ Gen2)"

# llama.cpp Android NDK 交叉编译（Quest 3 / arm64-v8a）。
# 参数逐项说明见 docs/native-integration.md 第 1 节；路径按本地源码与 NDK 实际位置替换。
#
# cmake -S "${LLAMA_SRC}" -B "${BUILD_DIR}" \
#   -DCMAKE_TOOLCHAIN_FILE="${ANDROID_NDK}/build/cmake/android.toolchain.cmake" \
#   -DANDROID_ABI=arm64-v8a \
#   -DANDROID_PLATFORM=android-29 \
#   -DCMAKE_BUILD_TYPE=Release \
#   -DLLAMA_BUILD_EXAMPLES=OFF \
#   -DLLAMA_BUILD_TESTS=OFF \
#   -DGGML_NATIVE=OFF
# cmake --build "${BUILD_DIR}" -j"$(nproc)"

echo "==> 编译产物 .so 放入 app/lib/arm64-v8a/"
