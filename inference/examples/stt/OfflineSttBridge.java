package com.daydream.speech;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * 离线 STT 外挂模型骨架：麦克风音频 → 本机识别为文字 → 送入对话输入。
 *
 * 与 TTS 桥接平行：模型 / 音色文件外置到本机存储，不打入 APK、不进入 Git 仓库。
 * 识别与音频采集在子线程；UI 线程只更新输入框文本与识别状态。
 *
 * 目录约定：
 * /storage/emulated/0/DayDream/voice/stt/
 *   └── <stt-model-id>/
 *       ├── model.bin / model.onnx        # STT 引擎权重（本地私有，不入库）
 *       └── stt-config.json                # 采样率、语言、模型元数据骨架
 */
public final class OfflineSttBridge {

    public static final String[] MODEL_SUFFIXES = {".bin", ".onnx", ".gguf", ".pt"};

    private long sttHandle = 0L;

    /** 扫描外置 STT 模型目录，返回模型目录或权重文件列表（供 UI 选择）。 */
    public static List<File> scanModels(File sttDir) {
        List<File> result = new ArrayList<>();
        if (sttDir == null || !sttDir.isDirectory()) {
            return result;
        }
        File[] entries = sttDir.listFiles();
        if (entries == null) {
            return result;
        }
        for (File f : entries) {
            if (f.isHidden()) continue;
            if (f.isDirectory()) {
                result.add(f);
            } else if (hasModelSuffix(f.getName())) {
                result.add(f);
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
     * 加载 STT 模型。
     *
     * @param modelPath  STT 模型目录或权重文件绝对路径
     * @param sampleRate 输入音频采样率（Hz），与采集端 AudioRecord 配置一致
     * @param language   识别语言标识（如 zh-CN）
     * @return STT 句柄；0 表示加载失败
     */
    public native long nativeSttInit(String modelPath, int sampleRate, String language);

    /**
     * 识别一段音频缓冲为文本。
     *
     * @param handle   nativeSttInit 返回的句柄
     * @param audioPcm 单声道 PCM 缓冲；编码与采样率由引擎约定
     * @return 识别文本；失败返回空字符串
     */
    public native String nativeSttRecognize(long handle, byte[] audioPcm);

    /** 释放 STT 模型内存；切换模型前必须先调用。 */
    public native void nativeSttRelease(long handle);

    public void load(File modelDir, int sampleRate, String language) {
        sttHandle = nativeSttInit(modelDir == null ? null : modelDir.getAbsolutePath(),
                sampleRate, language);
        if (sttHandle == 0L) {
            throw new IllegalStateException("STT model init failed: check model path / memory");
        }
    }

    public String recognize(byte[] audioPcm) {
        if (sttHandle == 0L) {
            throw new IllegalStateException("STT context not loaded");
        }
        return nativeSttRecognize(sttHandle, audioPcm);
    }

    public void release() {
        if (sttHandle != 0L) {
            nativeSttRelease(sttHandle);
            sttHandle = 0L;
        }
    }
}
