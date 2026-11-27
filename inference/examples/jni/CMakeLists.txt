# APK 内原生库 CMake 骨架：libdaydream-llama.so
# 外层 NDK 工具链由 scripts/ndk-build-inference.sh 传入；
# 本文件只负责把 JNI 桥接源文件编译成目标 ABI 的动态库。

cmake_minimum_required(VERSION 3.22)
project(daydream_llama LANGUAGES C)

# llama.cpp 源码根目录（由上层 CMake -DLLAMA_CPP_DIR=... 或脚本环境变量传入）
if(NOT DEFINED LLAMA_CPP_DIR)
    set(LLAMA_CPP_DIR "${CMAKE_SOURCE_DIR}/../../llama.cpp")
endif()

add_library(daydream-llama SHARED
    daydream-llama-jni.c
)

target_include_directories(daydream-llama PRIVATE
    "${LLAMA_CPP_DIR}/include"
)

# 链接 llama.cpp 静态库（NDK 编译产出 libllama.a；库名按实际 target 调整）
# target_link_libraries(daydream-llama PRIVATE llama)

# Android 平台：日志库 __android_log_print 按需链接
find_library(log-lib log)
# target_link_libraries(daydream-llama PRIVATE ${log-lib})

set_target_properties(daydream-llama PROPERTIES
    C_STANDARD 11
    POSITION_INDEPENDENT_CODE ON
)
