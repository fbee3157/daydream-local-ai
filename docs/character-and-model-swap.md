# 角色（Emma）替换与大模型自由切换说明

> 目标：在不改 APK 主链路代码的前提下，后续可以自由更换对话角色（人设 / 外观 / 语音），也可以自由切换底层 GGUF 大模型。角色素材与模型权重均外置到本机存储，不打入安装包、不进入 Git 仓库。

## 1. 两条独立的“替换线”

| 替换线 | 替换对象 | 外置位置 | 触发方式 | 是否需要重编 APK |
|---|---|---|---|---|
| 角色线 | Emma 的人设文本、外观模型 / 服装、语音音色 | `device-storage/DayDream/characters/`、`voice/` | 启动加载或角色选择项切换 | 否（素材外置） |
| 模型线 | 对话用 GGUF 权重、分词器、聊天模板 | `device-storage/DayDream/models/`、`tokenizer/` | 扫描目录选中文件 → 重载上下文 | 否（同一 llama.cpp / GGUF 格式前提下） |

两条线互不绑定：换角色不必换模型；换模型也不必改角色设定。

## 2. 角色替换（Emma → 其他角色）

### 2.1 角色构成拆分
原版 Emma 的表现分三层，替换时分别处理：
1. **人设文本（可自由改写）**：系统提示词里的角色定位、语气、关系设定、行为边界；对应 `PromptComposer` 的 System 段。
2. **外观素材（版权受限）**：3D 角色模型、服装、发型、场景、动画贴图；属于原应用版权方，仅限本人合法持有的副本本地研究使用，不得提取分发。
3. **语音音色（离线 TTS）**：TTS 音色包或声线参数；与 STT / TTS 模型一起计入内存预算。

### 2.2 外置角色目录约定（部署到头显后）
```
/storage/emulated/0/DayDream/
├── characters/
│   └── <character-id>/
│       ├── profile.json        # 人设元数据：名字、称呼、语气、简介、系统提示词引用
│       ├── system-prompt.md     # 角色系统提示词文本（喂给 PromptComposer System 段）
│       ├── avatar/              # 外观素材（本地私有，不入库；版权素材不提交 Git）
│       └── voice/               # 音色参数 / TTS 声线配置（本地私有）
├── models/                     # GGUF 权重（模型线）
├── tokenizer/                  # 分词器（模型线）
└── memory/                     # 对话历史与长期记忆
```

### 2.3 人设文本替换流程
1. 在 `characters/<character-id>/system-prompt.md` 写新角色的系统提示词（性格、语气、称呼、禁忌、合规边界）。
2. `profile.json` 记录角色名、展示名、提示词文件路径、音色引用。
3. 启动或切换角色时，应用读取该角色提示词注入 `PromptComposer` 的 System 段；历史记忆按角色隔离目录存放，避免不同角色串人设。
4. 外观素材若要替换：仅在本地解包工程内引用本人有权使用的素材；原版 Emma 模型、服装、贴图不得提取后对外分享或二次发布（见 `docs/risk-and-legal.md` §2）。

### 2.4 角色与记忆隔离
- 每个角色独立记忆子目录；切换角色时加载对应历史与偏好，不混用。
- 长期记忆要点仍走 `PromptComposer.addMemoryNote(...)`，只是按角色分别落盘。

## 3. 大模型自由切换（GGUF）

### 3.1 可切换范围
- **同引擎内自由换**：llama.cpp 支持的 GGUF 量化文件（Gemma 3 4B、Qwen 4B 及其他兼容 GGUF 模型），放入 `models/` 目录即可被 `GgufModelScanner` 扫描列出。
- **格式前提**：权重必须是 GGUF；其他框架格式（MNN、PTE、ONNX）需先转换，或另接对应推理后端。
- **分词器**：新版 GGUF 常内嵌分词信息；外置分词器目录仅在模型需要单独 tokenizer 文件时使用。

### 3.2 切换步骤（运行时，不改代码）
1. 把新 GGUF 文件复制到头显 `/storage/emulated/0/DayDream/models/`。
2. 应用扫描目录，下拉选择目标权重；选中路径传给 `NativeLlamaBridge.nativeInit(...)`。
3. 切换前先 `nativeRelease(handle)` 释放旧上下文与权重内存，再加载新模型，避免双份权重驻留。
4. 新模型若使用不同聊天模板（ChatML / Gemma / Qwen 模板），在外层提示词组装阶段套用对应模板标记；`PromptComposer` 负责通用内容，模板包装在模型适配层完成。

### 3.3 模型选择与内存约束（Quest 3，8GB 共享内存）
- 4B 量级 Q4_K_M 权重约 2.5–3GB，运行内存约 3GB 上下；STT / TTS、应用纹理、KV 缓存共享同一内存池。
- 内存不足时降级顺序：降量化档位（Q4 → Q3）→ 缩短上下文长度 → 减小模型规模 → 保留云端可选回退。
- 量化档位、权重体积、内存记录见 `reference/model-notes.md`。

### 3.4 模型元数据（可选配置骨架）
每个模型可在 `models/<model-id>.json` 记录展示名、GGUF 文件名、聊天模板、上下文长度建议、量化档位；应用读取后填充推理参数。仓库只放配置骨架示例，不放权重文件。

## 4. 角色 × 模型组合矩阵

| 组合 | 角色人设 | 底层模型 | 说明 |
|---|---|---|---|
| 默认 | Emma（原版人设文本） | Gemma 3 4B GGUF | 初始基线 |
| 换角色 | 自定义角色提示词 + 本地素材 | 同一 GGUF | 只改角色线，模型不动 |
| 换模型 | Emma 人设不变 | 其他 GGUF（Qwen 4B 等） | 只改模型线，角色不动 |
| 全换 | 新角色 | 新 GGUF | 两条线分别加载，记忆按角色隔离 |

## 5. 合规与入库边界（再次明确）
- **不入库**：原版 Emma 模型 / 服装 / 场景 / 贴图、GGUF 权重、分词器二进制、TTS 音色模型、签名密钥、本地私密配置；`.gitignore` 已排除。
- **可入库**：角色配置骨架（profile.json / system-prompt.md 示例）、模型扫描与切换代码、提示词组装代码、本文档与流程说明。
- 素材提取仅限本人合法持有的原版副本；修改后的安装包不上架商店、不对外分发。
- 模型生成内容保持健康合规；角色互动避免不当设定。

## 6. 与现有代码 / 文档衔接
- `inference/examples/model-scan/GgufModelScanner.java`：GGUF 目录扫描与选择。
- `inference/examples/prompt/PromptComposer.java`：系统提示词、记忆、历史组装；角色提示词注入 System 段。
- `inference/examples/jni/NativeLlamaBridge.java`：上下文句柄加载 / 释放，模型切换时先释放旧句柄。
- `docs/native-integration.md`：JNI 生命周期、线程约束、Smali 对接点。
- `docs/risk-and-legal.md`：版权素材、模型许可、隐私与分发边界。
