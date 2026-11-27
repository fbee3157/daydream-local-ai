package com.daydream.speech;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * 离线 TTS 外挂模型骨架：回复文本 → 本机合成语音 → 播放。
 *
 * 设计约定（与 llama.cpp 推理线平行）：
 * - TTS 模型 / 音色文件外置到头显本地存储，不打入 APK、不进入 Git 仓库。
 * - 运行时扫描外置 TTS 模型目录，选中后加载；切换音色 / 模型时先释放旧句柄。
 * - 合成与播放必须在子线程；UI 线程只更新气泡文本与播放状态。
 * - 离线优先：默认不调用云端 TTS；云端仅作为可选扩展，默认关闭。
 *
 * 目录约定：
 * /storage/emulated/0/DayDream/voice/tts/
 *   ├── <tts-model-id>/
 *   │   ├── model.bin / model.onnx        # TTS 引擎权重（本地私有，不入库）
 *   │   ├── voices/                        # 音色参考 / 声线配置（本地私有）
 *   │   └── tts-config.json               # 模型元数据骨架（采样率、音色、语速等）
 *   └── ...
 */
public final class OfflineTtsBridge {

    public static final String[] MODEL_SUFFIXES = {".bin", ".onnx", ".gguf", ".pt"};

    private long ttsHandle = 0L;

    static {
        // 若 TTS 也走 NDK 原生库，库名与推理库分开加载；这里仅声明骨架。
        // System.loadLibrary("daydream-tts");
    }

    /**
     * 扫描外置 TTS 模型目录，返回模型子目录或权重文件列表（供 UI 选择）。
     *
     * @param ttsDir TTS 外置根目录；不存在或不可读时返回空列表
     */
    public static List<File> scanModels(File ttsDir) {
        List<File> result = new ArrayList<>();
        if (ttsDir == null || !ttsDir.isDirectory()) {
            return result;
        }
        File[] entries = ttsDir.listFiles();
        if (entries == null) {
            return result;
        }
        for (File f : entries) {
            if (f.isHidden()) continue;
            if (f.isDirectory()) {
                result.add(f); // 目录型模型：内含权重 + 音色 + 配置
            } else if (hasModelSuffix(f.getName())) {
                result.add(f); // 单文件权重型模型
            }
        }
        Collections.sort(result, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        return result;
    }

    private static boolean hasModelSuffix(String name) {
        String n = name.toLowerCase(Locale.ROOT);
        for (String suf : MODEL_SUFFIXES) {
            if (n.endsWith(suf)) return true;
        }
        return false;
    }

    /**
     * 加载 TTS 模型与音色。
     *
     * @param modelPath  TTS 模型目录或权重文件绝对路径
     * @param voiceRef   音色标识 / 参考音色文件路径；可为空（使用默认音色）
     * @param sampleRate 目标输出采样率（Hz），与播放端 AudioTrack 配置一致
     * @return TTS 句柄；0 表示加载失败
     */
    public native long nativeTtsInit(String modelPath, String voiceRef, int sampleRate);

    /**
     * 合成一段文本为语音 PCM / 音频帧（具体编码由原生引擎决定）。
     *
     * @param handle   nativeTtsInit 返回的句柄
     * @param text     待合成文本（模型回复气泡文本）
     * @param speed    语速系数（1.0 为默认）
     * @return 合成结果句柄或音频字节数组；失败返回空
     */
    public native Object nativeTtsSynthesize(long handle, String text, float speed);

    /** 释放 TTS 模型与音色内存；切换模型前必须先调用。 */
    public native void nativeTtsRelease(long handle);

    public void load(File modelDir, String voiceRef, int sampleRate) {
        ttsHandle = nativeTtsInit(modelDir == null ? null : modelDir.getAbsolutePath(),
                voiceRef, sampleRate);
        if (ttsHandle == 0L) {
            throw new IllegalStateException("TTS model init failed: check model path / voice / memory");
        }
    }

    public void release() {
        if (ttsHandle != 0L) {
            nativeTtsRelease(ttsHandle);
            ttsHandle = 0L;
        }
    }
}
