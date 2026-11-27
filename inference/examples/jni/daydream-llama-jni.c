// JNI 原生侧骨架：对接 NativeLlamaBridge.java，底层调用 llama.cpp C API。
// 编译产物 libdaydream-llama.so，放入 APK 的 lib/arm64-v8a/。
//
// 说明：本文件是接口骨架；真正接入时把 TODO 替换为 llama.cpp 官方示例
// (examples/simple/simple.cpp) 中的模型加载、分词、采样、解码调用。
// 不在这里硬编码模型路径；路径全部由 Java 侧从 runtime-config.json 传入。

#include <jni.h>
#include <stdlib.h>
#include <string.h>

// llama.cpp C API 头文件（NDK 编译时通过 include 路径指向 llama.cpp 源码目录）
// #include "llama.h"

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jlong JNICALL
Java_com_daydream_inference_NativeLlamaBridge_nativeInit(
        JNIEnv *env, jobject thiz,
        jstring model_path, jstring tokenizer_path,
        jint context_size, jint threads, jint max_memory_mb) {
    // TODO: 解析 model_path / tokenizer_path 为 UTF-8 C 字符串；
    // TODO: llama_load_model_from_file() 加载 GGUF；
    // TODO: llama_new_context_with_model() 按 context_size、threads 建上下文；
    // TODO: 返回上下文指针（句柄）；失败返回 0。
    (void) env; (void) thiz; (void) model_path; (void) tokenizer_path;
    (void) context_size; (void) threads; (void) max_memory_mb;
    return 0;
}

JNIEXPORT jstring JNICALL
Java_com_daydream_inference_NativeLlamaBridge_nativeGenerate(
        JNIEnv *env, jobject thiz,
        jlong handle, jstring prompt, jint max_tokens) {
    // TODO: 转 UTF-8 prompt；tokenize -> eval 提示词 token；
    // TODO: 循环采样、解码新 token，直到 EOS 或 max_tokens；
    // TODO: 拼接回复字符串并 NewStringUTF 返回。
    // 注意：NewStringUTF 只接受 Modified UTF-8；非 ASCII 文本需确保编码正确。
    (void) env; (void) thiz; (void) handle; (void) prompt; (void) max_tokens;
    return (*env)->NewStringUTF(env, "");
}

JNIEXPORT void JNICALL
Java_com_daydream_inference_NativeLlamaBridge_nativeRelease(
        JNIEnv *env, jobject thiz, jlong handle) {
    // TODO: llama_free_context() / llama_free_model()；释放句柄指向资源。
    (void) env; (void) thiz; (void) handle;
}

#ifdef __cplusplus
}
#endif
