# 离线 TTS 语音功能说明（外挂模型、自由切换音色 / 模型）

> 目标：让用户可以和 Emma（或替换后的角色）语音对话；TTS 模型与音色全部外挂到头显本地存储，APK 只携带加载代码与骨架，不打包语音权重。STT（语音识别）与 TTS（语音合成）同属离线语音链路，本文聚焦 TTS。

## 1. 对话链路中的 TTS 位置

```
麦克风语音 ── STT 离线识别 ── 文字输入
                                  │
文字输入 ── PromptComposer 组装 ── 本地 llama.cpp 推理 ── 回复文本
                                  │                        │
                                  │                        ├── 渲染到对话气泡
                                  │                        ├── 触发口型动画
                                  │                        └── OfflineTtsBridge 合成语音 → AudioTrack 播放
                                  └── User / Assistant 历史写入本地记忆
```

- TTS 输入：模型回复文本（气泡同一文本，避免说的和显示的不一致）。
- TTS 输出：PCM / 音频帧交给播放端；离线环境不请求云端语音接口。
- 口型动画复用原版已有事件；TTS 文本与口型时间轴对齐即可。

## 2. 外挂目录约定（部署到头显后）

```
/storage/emulated/0/DayDream/
├── models/                     # 对话大模型 GGUF（模型线，见 character-and-model-swap.md）
├── tokenizer/                  # 分词器
├── memory/                     # 对话历史、角色设定、长期记忆
└── voice/
    └── tts/
        ├── <tts-model-id>/
        │   ├── model.bin / model.onnx / model.gguf   # TTS 引擎权重（本地私有，不入库）
        │   ├── voices/                                # 音色参考 / 声线配置（本地私有）
        │   └── tts-config.json                        # 模型元数据骨架：采样率、音色、语速、语言
        └── ...
```

### 2.1 文件边界
- **不入库（.gitignore 已排除同类二进制）**：TTS 权重、音色参考音频、声线二进制、STT 模型二进制。
- **可入库**：TTS 桥接代码、目录扫描示例、对话流程串联代码、配置骨架 JSON、本文档。

## 3. TTS 模型 / 音色自由切换

### 3.1 切换流程（运行时，不改 APK 代码）
1. 把 TTS 模型目录或权重文件放入 `voice/tts/<model-id>/`；音色文件放入 `voices/`。
2. `OfflineTtsBridge.scanModels(...)` 扫描目录，UI 下拉选择目标模型与音色。
3. 加载前若旧 TTS 句柄存在，先 `nativeTtsRelease(handle)` 释放旧模型与音色内存，再 `nativeTtsInit(...)` 加载新句柄。
4. 合成调用 `nativeTtsSynthesize(handle, text, speed)`；输出采样率需与播放端 AudioTrack 配置一致。
5. 切换音色只换 `voiceRef`；切换引擎模型才替换模型目录 / 权重文件。

### 3.2 模型与音色分离
| 切换对象 | 替换内容 | 是否换权重文件 |
|---|---|---|
| 音色 / 声线 | `voices/` 内参考音色或声线参数，`voiceRef` 指向它 | 否 |
| TTS 引擎模型 | `tts-config.json` 对应模型目录或权重文件 | 是 |

### 3.3 与角色设定的关系
- 角色人设文本在 `characters/<id>/system-prompt.md`；音色引用记录在角色 profile 的 `voice_ref` 字段。
- 切换角色时，按 profile 加载对应音色；角色记忆目录隔离，不混用历史与偏好。
- 详见 `docs/character-and-model-swap.md` §2、§4。

## 4. 内存与性能约束（Quest 3，8GB 共享内存）
- TTS 权重、对话 GGUF、KV 缓存、STT 模型、应用纹理共享同一内存池；先真机实测驻留占用。
- TTS 合成与推理都在子线程；UI 线程只更新气泡文本、播放状态与口型触发。
- 内存不足时降级顺序：先关闭非必要音色预加载 → 缩短单次合成文本长度 → 降低 TTS 模型规模 → 必要时仅显示文字、暂停语音播放。
- 采样率、语速、音色参数在 `tts-config.json` 骨架中记录；运行时与 `runtime-config.json` 的 `speech` 段对齐。

## 5. 配置骨架衔接
`app/assets/runtime-config.json` 的 `speech` 段：
- `tts_provider`: `on-device`（离线本机合成）；云端 TTS 为可选扩展，默认关闭。
- `offline_only`: `true`；离线主链路不发起语音网络请求。
- `lip_sync_enabled`: 回复文本同时喂给口型动画触发点。

TTS 模型目录、音色路径、采样率等外置项由 `voice/tts/<model-id>/tts-config.json` 承载；仓库只放骨架示例，不放权重。

## 6. 示例代码清单
| 文件 | 职责 |
|---|---|
| `inference/examples/tts/OfflineTtsBridge.java` | TTS 模型扫描、句柄初始化 / 合成 / 释放骨架（native 方法声明） |
| `inference/examples/tts/TtsDialogueFlow.java` | 对话回合串联：提示词组装 → 推理回复 → TTS 合成 → 历史落盘 |

## 7. 对接现有文档
- `docs/integration.md` §2.2：TTS 模块边界与语音内存预算。
- `docs/native-integration.md`：JNI 句柄生命周期、线程约束、Smali 类 / 方法对接规则。
- `docs/character-and-model-swap.md`：角色音色引用、模型切换与记忆隔离。
- `docs/risk-and-legal.md`：语音模型许可、素材版权与本地私有边界。
- `reference/model-notes.md`：对话模型量化档位与内存记录；TTS 占用在真机实测后补充。

## 8. Smali 对接要点（回编译前核对）
1. 在 JADX 定位原版回复文本赋值点、语音播放触发点、口型动画事件。
2. 本地链路：扫描 GGUF → 加载推理上下文 → 组装提示词 → 生成回复；TTS 句柄独立加载 / 释放，与推理句柄分开管理。
3. Smali 中 native 方法名、参数类型需与 C 侧导出函数一致；合成调用放子线程。
4. 云端语音调用保持可选默认关闭；离线模式不发起语音网络请求。
