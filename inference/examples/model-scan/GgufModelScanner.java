package com.daydream.inference;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * GGUF 目录扫描示例：扫描外置模型目录，列出可用 GGUF 权重文件。
 *
 * 目录约定（对应 device-storage/DayDream/models/）：
 * /storage/emulated/0/DayDream/models/
 *   ├── gemma-3-4b-it-Q4_K_M.gguf
 *   ├── qwen4b-q4_k_m.gguf
 *   └── ...
 *
 * 约束：
 * - 只识别 .gguf 后缀；不解析模型内部张量，不读取权重内容。
 * - 不递归进入隐藏目录；忽略 .gitkeep 与临时文件。
 * - 扫描在本地线程执行；UI 层拿到列表后再展示下拉选择。
 */
public final class GgufModelScanner {

    public static final String GGUF_SUFFIX = ".gguf";

    /**
     * 扫描模型目录，返回按文件名排序的 GGUF 文件列表。
     *
     * @param modelDir 外置模型目录绝对路径；目录不存在或不可读时返回空列表
     * @return 可选择的 GGUF 文件（含绝对路径）
     */
    public static List<File> scan(File modelDir) {
        List<File> result = new ArrayList<>();
        if (modelDir == null || !modelDir.isDirectory()) {
            return result;
        }
        File[] files = modelDir.listFiles();
        if (files == null) {
            return result;
        }
        for (File f : files) {
            if (f.isHidden() || f.isDirectory()) {
                continue;
            }
            String name = f.getName().toLowerCase(Locale.ROOT);
            if (name.endsWith(GGUF_SUFFIX)) {
                result.add(f);
            }
        }
        Collections.sort(result, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        return result;
    }

    /** 取文件名（去掉后缀）作为下拉展示名。 */
    public static String displayName(File gguf) {
        String n = gguf.getName();
        int dot = n.lastIndexOf('.');
        return dot > 0 ? n.substring(0, dot) : n;
    }
}
