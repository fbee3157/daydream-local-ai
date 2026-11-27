# 项目现状、缺口与下一步清单

> 本文汇总仓库当前已落地内容、未完成骨架、待真机验证项与执行顺序，作为后续完善的索引。代码与文档为自研骨架；原版 APK、素材、模型权重不入库。

## 1. 已完成模块

| 模块 | 落地内容 | 对应文件 |
|---|---|---|
| 工程骨架 | README、LICENSE、`.gitignore`、配置骨架、目录结构 | `README.md`、`LICENSE`、`app/assets/runtime-config.json` |
| NDK / JNI | CMake 参数说明、JNI 桥接 Java + C 骨架、原生库 CMake | `docs/native-integration.md`、`inference/examples/jni/` |
| 模型扫描 | GGUF 目录扫描、过滤、排序、空目录处理 | `inference/examples/model-scan/GgufModelScanner.java` |
| 提示词组装 | 系统设定 → 记忆 → 历史裁剪 → 当前输入；模板分层 | `inference/examples/prompt/PromptComposer.java` |
| 角色 / 模型切换 | 角色人设外置、GGUF 自由切换、组合矩阵、记忆隔离 | `docs/character-and-model-swap.md`、`modules/memory/profile/` |
| 离线 TTS | TTS 模型 / 音色外挂、扫描加载释放、对话流程串联 | `docs/tts-offline-guide.md`、`inference/examples/tts/`、`modules/speech/tts/` |
| 远端场景 | 种子请求清单、资源缓存、角色 spawn 入场、离线回退 | `docs/scene-server-bridge.md`、`modules/scene/` |
| 逆向审计 | ADB 连接检查、拉取、解包、只读扫描报告脚本 | `scripts/reverse-audit.sh`、`docs/reverse-workflow.md` |
| 打包回装 | 回编译、对齐、签名、回装命令参考 | `docs/packaging.md`、`scripts/repackage-sign.sh`、`scripts/adb-install.sh` |
| 合规备忘 | 使用边界、版权素材、模型许可、隐私、技术风险 | `docs/risk-and-legal.md`、`reference/model-notes.md`、`reference/tool-notes.md` |

## 2. 未完成骨架（待接入真实实现）

### 2.1 推理原生层
- `daydream-llama-jni.c` 中 `nativeInit / nativeGenerate / nativeRelease` 的 llama.cpp C API 调用仍是 TODO：模型加载、上下文创建、tokenize、采样、解码、释放。
- 原生库 CMake 与 llama.cpp 静态库链接关系待按真实 NDK 构建结果补齐。
- 真机内存实测：权重 + KV 缓存 + STT/TTS + 应用纹理的驻留占用与峰值。

### 2.2 STT 离线语音
- 识别桥接 Java 骨架：`inference/examples/stt/OfflineSttBridge.java`（模型扫描、句柄初始化 / 识别 / 释放）。
- 待接入：采集端 AudioRecord 缓冲对接、STT 引擎 C API、识别结果送入 `PromptComposer` 输入链路；与 TTS 共享内存预算。

### 2.3 记忆模块
- **短期历史**：轮次管理骨架 `modules/memory/history/ConversationHistory.java`；待补落盘、上下文裁剪、会话结束归档。
- **长期记忆**：偏好 / 角色设定本地读写；按角色隔离目录。
- **向量检索（可选）**：历史片段索引、召回、注入提示词；未启用时按顺序拼接历史。

### 2.4 联网搜索（可选，默认关闭）
- Router 判断是否需要检索；结果摘要拼入提示词；离线模式不发起网络请求。

### 2.5 场景服务客户端
- `SceneEntryClient` 的真实 HTTP 请求、清单解析、哈希校验、资源下载缓存、角色 spawn 对接场景引擎方法。
- 服务器侧生成 / 种子复现逻辑不在本仓库构建，仅约定契约（见 `docs/scene-server-bridge.md` §4）。

### 2.6 Smali 对接（依赖逆向审计报告）
- 对话输入 / 发送 / 气泡文本、语音播放与口型事件、网络请求类、原生库存在性核对。
- 本地推理替换云端调用点；线程派发与句柄生命周期在 Smali 侧落地。

## 3. 配置一致性核对项
- `runtime-config.json` 的 `local / speech / memory / search / scene` 段与各示例代码读取字段一致；新增配置项时同步更新骨架示例。
- 外置目录：`models / tokenizer / memory / voice/tts / scenes / characters` 与文档目录约定一致。
- 所有二进制权重、音色、场景素材、签名密钥保持 `.gitignore` 排除。

## 4. 建议执行顺序
1. 真机运行 `scripts/reverse-audit.sh`，拿到 Smali / 网络层 / 原生库审计清单。
2. 按报告确认原版是云端还是本地推理，确定 JNI 接入替换点。
3. 补齐 llama.cpp JNI 真实调用；NDK 编译产出 `.so` 放入 `lib/arm64-v8a/`。
4. 接入 GGUF 扫描与提示词组装；先跑通纯文字本地对话。
5. 补 STT 骨架，再接 TTS 语音播放与口型事件。
6. 记忆历史 / 长期记忆落盘；按需开启向量检索。
7. 场景种子入场客户端对接；场景服务保持可选、默认关闭。
8. 打包、对齐、签名、回装真机验证；记录内存与兼容性问题到 `reference/model-notes.md`。

## 5. 仓库边界提醒
- 可入库：自研代码、脚本、文档、配置骨架、示例 JSON / Markdown。
- 不入库：原版 APK 与分包、角色 / 服装 / 场景素材、GGUF 与语音模型二进制、签名密钥、含真实密钥或私有路径的配置副本。
