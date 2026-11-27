# 运行配置字段对照（runtime-config.json）

> 配置骨架位于 `app/assets/runtime-config.json`；本文逐段说明字段用途、取值与对应代码 / 文档。新增配置项时先同步本表，再更新示例代码读取逻辑。

## 1. local（本地推理）
| 字段 | 类型 | 取值 / 默认 | 说明 | 衔接 |
|---|---|---|---|---|
| `engine` | string | `llama.cpp` | 推理引擎；定稿 llama.cpp | `docs/native-integration.md` |
| `model_dir` | string | `/storage/emulated/0/DayDream/models` | GGUF 权重外置目录 | `GgufModelScanner.java` |
| `model_file` | string | 空 | 选中的 GGUF 文件名；空时由扫描列表选择 | `GgufModelScanner.java` |
| `tokenizer_path` | string | `/storage/emulated/0/DayDream/tokenizer` | 分词器目录；新版 GGUF 可内嵌分词信息 | `docs/native-integration.md` §2 |
| `context_length` | int | `2048` | 上下文 token 上限；KV 缓存随长度增长 | `reference/model-notes.md` |
| `threads` | int | `4` | 推理线程数 | `NativeLlamaBridge.nativeInit` |
| `max_memory_mb` | int | `3072` | 推理内存上限参考值；真机实测后调整 | `docs/project-status.md` §2.1 |
| `model_format` | string | `GGUF` | 权重格式；其他框架格式需转换 | `docs/character-and-model-swap.md` §3.1 |

## 2. cloud（云端可选扩展，默认关闭）
| 字段 | 类型 | 取值 / 默认 | 说明 |
|---|---|---|---|
| `enabled` | bool | `false` | 云端接口默认不启用；离线主链路不依赖 |
| `api_endpoint` | string | 空 | 云端对话地址；仓库骨架不填真实地址 |
| `api_key` | string | 空 | 云端密钥占位；不提交真实密钥 |
| `fallback_when_local_missing` | bool | `false` | 本地模型缺失时是否回退云端；按配置决定，不强制 |

## 3. speech（语音：STT + TTS）
| 字段 | 类型 | 取值 / 默认 | 说明 | 衔接 |
|---|---|---|---|---|
| `stt_provider` | string | `on-device` | 离线识别；不调用云端语音 | `OfflineSttBridge.java` |
| `stt_model_dir` | string | `/storage/emulated/0/DayDream/voice/stt` | STT 模型外置目录 | `modules/speech/stt/stt-config.example.json` |
| `tts_provider` | string | `on-device` | 离线合成 | `OfflineTtsBridge.java` |
| `offline_only` | bool | `true` | 语音链路离线优先；不发起语音网络请求 | `docs/tts-offline-guide.md` §5 |
| `lip_sync_enabled` | bool | `true` | 回复文本同时触发口型动画 | `docs/integration.md` §2.2 |

## 4. memory（记忆）
| 字段 | 类型 | 取值 / 默认 | 说明 | 衔接 |
|---|---|---|---|---|
| `storage_dir` | string | `/storage/emulated/0/DayDream/memory` | 对话历史、长期记忆本地目录 | `ConversationHistory.java` |
| `enable_long_term` | bool | `true` | 长期偏好 / 角色设定本地落盘 | `docs/integration.md` §3.2 |
| `enable_vector_retrieval` | bool | `false` | 可选向量检索；关闭时按顺序拼接历史 | `modules/memory/retrieval/retrieval.example.json` |

## 5. search（可选联网搜索，默认关闭）
| 字段 | 类型 | 取值 / 默认 | 说明 |
|---|---|---|---|
| `enabled` | bool | `false` | 离线主链路不发起搜索请求 |
| `require_retrieval_rule` | string | `model-or-rule` | 检索判断规则；Router 与规则共同决定 |
| `include_result_context` | bool | `true` | 检索摘要是否拼入提示词上下文 |

## 6. scene（远端场景种子入场，可选）
| 字段 | 类型 | 取值 / 默认 | 说明 | 衔接 |
|---|---|---|---|---|
| `remote_enabled` | bool | `false` | 场景服务默认关闭；本地离线对话不受影响 | `docs/scene-server-bridge.md` §5 |
| `server_endpoint` | string | 空 | 场景服务器地址；骨架不填真实地址 | `SceneEntryClient.java` |
| `scene_cache_dir` | string | `/storage/emulated/0/DayDream/scenes` | 场景资源本地缓存目录 | `docs/scene-server-bridge.md` §3 |
| `character_on_spawn` | bool | `true` | 场景加载完成后是否把本地角色放到出生点 | `SceneEntryClient.spawnCharacter` |

## 7. 校验清单（接入新代码前）
- [ ] 配置字段类型与上表一致；布尔值不写成字符串。
- [ ] 外置目录路径与 `docs/character-and-model-swap.md`、`docs/tts-offline-guide.md`、`docs/scene-server-bridge.md` 的目录约定一致。
- [ ] 真实密钥、私有绝对路径不写入仓库配置；本地私有副本由 `.gitignore` 排除。
- [ ] 模型权重、STT/TTS 二进制、场景素材、签名密钥保持排除，不进入提交。
