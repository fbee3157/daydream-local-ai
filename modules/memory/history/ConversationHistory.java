package com.daydream.memory;

import java.util.ArrayList;
import java.util.List;

/**
 * 短期会话历史骨架：保存当前会话的 User / Assistant 轮次，并按上限裁剪最早历史。
 *
 * 落盘格式、文件路径与长期记忆读写由记忆模块在真机实现；本类只负责内存内的轮次管理，
 * 与 PromptComposer 的历史输入衔接。历史文本本地私有，不进入 Git 仓库。
 */
public final class ConversationHistory {

    public static final class Turn {
        public final String role;
        public final String text;

        public Turn(String role, String text) {
            this.role = role;
            this.text = text == null ? "" : text;
        }
    }

    private final List<Turn> turns = new ArrayList<>();
    private final int maxTurns;

    public ConversationHistory(int maxTurns) {
        this.maxTurns = maxTurns > 0 ? maxTurns : 20;
    }

    /** 追加一轮对话；超出上限时丢弃最早轮次。 */
    public void add(String role, String text) {
        turns.add(new Turn(role, text));
        while (turns.size() > maxTurns) {
            turns.remove(0);
        }
    }

    /** 返回当前历史的只读副本，供 PromptComposer 组装上下文。 */
    public List<Turn> snapshot() {
        return new ArrayList<>(turns);
    }

    public void clear() {
        turns.clear();
    }

    public int size() {
        return turns.size();
    }
}
