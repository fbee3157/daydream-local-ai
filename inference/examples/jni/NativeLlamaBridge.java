package com.daydream.inference;

/**
 * JNI 桥接骨架：Java/Kotlin 层调用 NDK 编译出的 llama.cpp 原生库。
 *
 * 约定：
 * - 原生库名 libdaydream-llama.so，Java 侧 System.loadLibrary("daydream-llama")。
 * - 方法签名与 C 侧 JNI 函数一一对应；C 侧不持有 Java 对象引用的全局副本，避免内存泄漏。
 * - 推理调用必须在子线程执行，禁止在 UI 线程加载权重或生成回复。
 *
 * 对应 Smali：本类编译后会出现在 smali/com/daydream/inference/NativeLlamaBridge.smali，
 * Smali 对接时按类名、方法名、参数类型核对，不要改动 native 方法名与 C 侧导出函数名。
 */
public final class NativeLlamaBridge {

    static {
        // 加载原生库；文件名不带 lib 前缀与 .so 后缀
        System.loadLibrary("daydream-llama");
    }

    private long handle = 0L;

    /**
     * 加载 GGUF 权重与分词器，初始化推理上下文。
     *
     * @param modelPath   外置 GGUF 文件绝对路径
     * @param tokenizerPath 分词器目录或文件绝对路径（llama.cpp 新版可内嵌在 GGUF 中，可为空）
     * @param contextSize 上下文长度（与 runtime-config.json 的 context_length 对齐）
     * @param threads     推理线程数
     * @param maxMemoryMb 内存上限（MB），仅用于 C 侧日志与兜底，不强制限额
     * @return 上下文句柄；0 表示加载失败
     */
    public native long nativeInit(String modelPath, String tokenizerPath, int contextSize,
                                  int threads, int maxMemoryMb);

    /**
     * 输入已组装好的完整提示词，同步生成回复文本。
     *
     * @param handle  nativeInit 返回的句柄
     * @param prompt  完整提示词（系统提示 + 历史 + 当前输入）
     * @param maxTokens 本次生成最大 token 数
     * @return 模型回复文本；失败返回空字符串
     */
    public native String nativeGenerate(long handle, String prompt, int maxTokens);

    /** 释放上下文与权重内存；句柄置空，避免重复释放。 */
    public native void nativeRelease(long handle);

    /** Java 侧封装：加载失败抛异常，调用方在子线程捕获。 */
    public void load(String modelPath, String tokenizerPath, int contextSize,
                     int threads, int maxMemoryMb) {
        handle = nativeInit(modelPath, tokenizerPath, contextSize, threads, maxMemoryMb);
        if (handle == 0L) {
            throw new IllegalStateException("llama.cpp context init failed: check GGUF path / memory");
        }
    }

    public String generate(String prompt, int maxTokens) {
        if (handle == 0L) {
            throw new IllegalStateException("native context not loaded");
        }
        return nativeGenerate(handle, prompt, maxTokens);
    }

    public void release() {
        if (handle != 0L) {
            nativeRelease(handle);
            handle = 0L;
        }
    }
}
