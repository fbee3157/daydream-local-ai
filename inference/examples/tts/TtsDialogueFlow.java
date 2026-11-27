package com.daydream.speech;

import com.daydream.inference.NativeLlamaBridge;
import com.daydream.inference.PromptComposer;

/**
 * 对话 + TTS 串联示例：用户输入 → 提示词组装 → 本地推理回复 → TTS 合成播放。
 *
 * 线程约束：
 * - 推理、TTS 合成均在子线程执行；本类只给出调用顺序，线程派发由上层 UI / 调度层完成。
 * - 模型切换、TTS 模型切换时先释放旧句柄，避免双份权重驻留内存。
 *
 * 对话回合：
 * 1. STT（离线语音识别）把麦克风语音转成文字；文字输入可直接跳过 STT。
 * 2. PromptComposer 组装系统设定 + 记忆 + 历史 + 当前输入。
 * 3. NativeLlamaBridge 生成回复文本。
 * 4. 回复文本交给 OfflineTtsBridge 合成语音，播放；文本同时渲染到气泡并触发口型动画。
 * 5. 本轮 User / Assistant 历史写入记忆目录。
 */
public final class TtsDialogueFlow {

    private final NativeLlamaBridge llama;
    private final OfflineTtsBridge tts;
    private final PromptComposer composer;

    public TtsDialogueFlow(NativeLlamaBridge llama, OfflineTtsBridge tts, PromptComposer composer) {
        this.llama = llama;
        this.tts = tts;
        this.composer = composer;
    }

    /**
     * 一个对话回合。
     *
     * @param userInput  用户文字输入（或 STT 识别结果）
     * @param maxTokens  推理最大生成 token 数
     * @param speed      TTS 语速系数
     * @return 模型回复文本；语音由 TTS 侧异步播放
     */
    public String chatAndSpeak(String userInput, int maxTokens, float speed) {
        String prompt = composer.compose(userInput, /* maxChars = */ 4096);

        // 本地推理生成回复（子线程调用）
        String reply = llama.generate(prompt, maxTokens);

        // TTS 合成；文本为空时不合成，避免空音频
        if (reply != null && !reply.trim().isEmpty()) {
            tts.nativeTtsSynthesize(ttsHandle(), reply, speed);
        }

        // 记录历史：当前用户输入 + 本轮助手回复
        composer.addHistory(new PromptComposer.Turn("user", userInput));
        composer.addHistory(new PromptComposer.Turn("assistant", reply));
        return reply;
    }

    private long ttsHandle() {
        // 桥接读取已加载句柄；骨架阶段由 OfflineTtsBridge 内部持有
        return 0L;
    }
}
