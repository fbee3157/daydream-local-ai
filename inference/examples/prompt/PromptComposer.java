package com.daydream.inference;

import java.util.ArrayList;
import java.util.List;

/**
 * 提示词组装示例：系统设定 + 长期记忆要点 + 会话历史 + 当前输入。
 *
 * 组装顺序固定，避免历史上下文把系统提示挤出窗口；截断策略从最早的历史消息丢弃。
 * 不同模型的聊天模板（ChatML / Gemma / Qwen 模板）在 buildModelTemplatePrompt()
 * 阶段再包一层；本类先完成通用内容拼接。
 *
 * 历史条目结构约定：role（system/user/assistant）+ text 文本。
 */
public final class PromptComposer {

    public static final class Turn {
        public final String role;
        public final String text;

        public Turn(String role, String text) {
            this.role = role;
            this.text = text == null ? "" : text;
        }
    }

    private final String systemPrompt;
    private final List<String> memoryNotes = new ArrayList<>();
    private final List<Turn> history = new ArrayList<>();

    public PromptComposer(String systemPrompt) {
        this.systemPrompt = systemPrompt == null ? "" : systemPrompt;
    }

    /** 追加长期记忆要点（用户偏好、角色设定、关键事实），每行一条。 */
    public void addMemoryNote(String note) {
        if (note != null && !note.trim().isEmpty()) {
            memoryNotes.add(note.trim());
        }
    }

    /** 追加一轮会话历史（上一条用户消息与助手回复）。 */
    public void addHistory(Turn turn) {
        if (turn != null && !turn.text.isEmpty()) {
            history.add(turn);
        }
    }

    /**
     * 组装最终提示词。
     *
     * @param userInput 当前用户输入；为空时抛异常
     * @param maxChars  上下文字符上限；超出时从最早历史开始裁剪
     * @return 可直接传给 nativeGenerate 的完整提示词
     */
    public String compose(String userInput, int maxChars) {
        if (userInput == null || userInput.trim().isEmpty()) {
            throw new IllegalArgumentException("userInput is empty");
        }

        StringBuilder sb = new StringBuilder();
        if (!systemPrompt.isEmpty()) {
            sb.append("### System\n").append(systemPrompt).append('\n');
        }
        if (!memoryNotes.isEmpty()) {
            sb.append("### Memory\n");
            for (String note : memoryNotes) {
                sb.append("- ").append(note).append('\n');
            }
        }

        // 历史按容量裁剪：先保留最近的轮次
        List<Turn> kept = fitHistory(maxChars, sb.length());
        sb.append("### Conversation\n");
        for (Turn t : kept) {
            sb.append(roleLabel(t.role)).append(": ").append(t.text).append('\n');
        }
        sb.append("User: ").append(userInput.trim()).append('\n');
        sb.append("Assistant: ");
        return sb.toString();
    }

    private List<Turn> fitHistory(int maxChars, int baseLen) {
        List<Turn> kept = new ArrayList<>();
        int used = baseLen;
        for (int i = history.size() - 1; i >= 0; i--) {
            Turn t = history.get(i);
            int turnLen = roleLabel(t.role).length() + 2 + t.text.length();
            if (used + turnLen > maxChars && !kept.isEmpty()) {
                break;
            }
            kept.add(0, t);
            used += turnLen;
        }
        return kept;
    }

    private static String roleLabel(String role) {
        if ("assistant".equalsIgnoreCase(role)) return "Assistant";
        if ("system".equalsIgnoreCase(role)) return "System";
        return "User";
    }
}
