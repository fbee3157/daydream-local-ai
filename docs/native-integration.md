# llama.cpp NDK 编译、JNI 骨架、GGUF 扫描、提示词组装与 Smali 对接点

> 本文把接入示例代码串到编译与回装流程上。示例代码位于 `inference/examples/`，只含自研骨架；llama.cpp 源码、GGUF 权重、分词器二进制均不入库。

## 1. NDK 交叉编译：CMake 参数逐项说明

### 1.1 前置环境
- Android NDK（建议 r26 或以上），`ANDROID_NDK` 指向 NDK 根目录。
- CMake 3.22+；llama.cpp 源码克隆到本地（仓库内只保留外链说明，不提交源码）。
- 目标设备 Quest 3：ABI `arm64-v8a`，Android 平台 API 29（Quest 3 系统底层）。

### 1.2 工具链与 ABI 参数（必选）
| CMake 参数 | 取值 | 作用 |
|---|---|---|
| `CMAKE_TOOLCHAIN_FILE` | `${ANDROID_NDK}/build/cmake/android.toolchain.cmake` | 引入 Android NDK 交叉编译工具链（clang、sysroot、目标链接器） |
| `ANDROID_ABI` | `arm64-v8a` | 目标 CPU 架构；Quest 3 为 ARM64，不要打入 armeabi-v7a |
| `ANDROID_PLATFORM` | `android-29` | 最低 Android API；决定可用系统头文件与链接库版本 |
| `CMAKE_BUILD_TYPE` | `Release` | 开启优化；调试期可用 `RelWithDebInfo` 保留符号 |

### 1.3 llama.cpp 功能裁剪参数（按需关闭，缩小 .so 体积）
| CMake 参数 | 建议取值 | 作用 |
|---|---|---|
| `LLAMA_STATIC` | `OFF`（移动端按需） | 控制 llama 库静态/动态链接；以 APK 内只放必要 .so 为准 |
| `LLAMA_BUILD_EXAMPLES` | `OFF` | 不编译命令行示例，只留库 |
| `LLAMA_BUILD_TESTS` | `OFF` | 不编译单元测试 |
| `LLAMA_OPENMP` | `OFF`（或按真机实测） | OpenMP 在移动设备上线程调度需实测；关闭可减少运行时线程开销 |
| `LLAMA_AVX2` / 桌面指令集 | 关闭（ARM 目标不适用） | x86 SIMD 在 ARM 上无效，避免误开 |
| `GGML_NATIVE` | `OFF` | 不按编译机 CPU 开启专属指令集，保证 ARM64 通用可运行 |

### 1.4 典型编译命令（脚本骨架见 `scripts/ndk-build-inference.sh`）
```bash
# 环境变量按本地实际路径替换
export ANDROID_NDK="$HOME/Android/Sdk/ndk/26.3.11579264"
export LLAMA_SRC="$HOME/projects/llama.cpp"

cmake -S "${LLAMA_SRC}" -B build-quest3 \
  -DCMAKE_TOOLCHAIN_FILE="${ANDROID_NDK}/build/cmake/android.toolchain.cmake" \
  -DANDROID_ABI=arm64-v8a \
  -DANDROID_PLATFORM=android-29 \
  -DCMAKE_BUILD_TYPE=Release \
  -DLLAMA_BUILD_EXAMPLES=OFF \
  -DLLAMA_BUILD_TESTS=OFF \
  -DGGML_NATIVE=OFF

cmake --build build-quest3 -j"$(nproc)"
```

### 1.5 产物落点
- 推理静态/动态库与 JNI 桥接库 `libdaydream-llama.so` 复制到解包目录 `app/lib/arm64-v8a/`。
- `.so` 属于构建产物，已被 `.gitignore` 排除，不进入 Git 提交。

## 2. JNI 加载原生库示例骨架

### 2.1 文件对应关系
| 角色 | 文件 |
|---|---|
| Java 接口封装 | `inference/examples/jni/NativeLlamaBridge.java` |
| C/JNI 导出实现 | `inference/examples/jni/daydream-llama-jni.c` |
| 原生库 CMake | `inference/examples/jni/CMakeLists.txt` |

### 2.2 加载流程
1. `System.loadLibrary("daydream-llama")` 加载 `libdaydream-llama.so`（文件名规则：`lib` + 库名 + `.so`）。
2. `nativeInit(...)` 传入 GGUF 路径、分词器路径、上下文长度、线程数、内存上限，返回上下文句柄。
3. `nativeGenerate(handle, prompt, maxTokens)` 同步生成回复；**调用必须在子线程**，UI 线程只负责展示。
4. `nativeRelease(handle)` 退出或模型切换时释放权重与上下文内存。

### 2.3 JNI 对接约束
- C 侧导出函数名必须与 Java 类包名对应：`Java_com_daydream_inference_NativeLlamaBridge_<method>`。
- 不在原生层缓存 Java 对象全局引用；需要回调时通过 `NewGlobalRef` / `DeleteGlobalRef` 成对管理。
- 字符串跨层用 UTF-8；回复文本含非 ASCII 时，确认 `NewStringUTF` 的 Modified UTF-8 编码正确。
- 错误信息回传 Java 抛 `IllegalStateException`，原生层不弹 Android 对话框。

## 3. GGUF 目录扫描示例

代码：`inference/examples/model-scan/GgufModelScanner.java`。

### 3.1 扫描规则
- 目录：外置 `/storage/emulated/0/DayDream/models/`（对应 `device-storage/DayDream/models/`）。
- 只识别后缀 `.gguf`；忽略隐藏文件、子目录、`.gitkeep` 与临时文件。
- 返回按文件名排序的绝对路径列表，供 UI 下拉选择；选中后把路径传给 `nativeInit`。

### 3.2 缺失处理
- 目录不存在 / 不可读：返回空列表，UI 提示先授予存储权限并放入 GGUF 文件。
- 不读取权重张量内容；模型完整性由 llama.cpp 加载阶段校验。

## 4. 提示词组装示例

代码：`inference/examples/prompt/PromptComposer.java`。

### 4.1 组装顺序（固定）
1. **System 系统设定**：角色定位、行为约束、合规边界。
2. **Memory 长期记忆**：用户偏好、角色设定、关键事实，每条一行。
3. **Conversation 会话历史**：最近若干轮 User / Assistant 对话；容量超限时从最早历史裁剪。
4. **当前输入**：用户本轮消息；末尾补 `Assistant: ` 引导续写。

### 4.2 模板分层
- `PromptComposer` 先拼通用内容；接入具体模型（Gemma / Qwen / ChatML）时，在外层再套对应聊天模板的特殊标记与轮次分隔符。
- 上下文长度与 `runtime-config.json` 的 `context_length`、`max_memory_mb` 对齐；历史裁剪在字符预算内完成，避免把系统提示挤出窗口。

## 5. Smali 对接点说明（回编译前在反编译工程内核对）

### 5.1 定位顺序（JADX 先读逻辑，Smali 再改字节码）
1. **对话输入与发送**：输入框监听、发送按钮点击方法；消息文本从哪里进入对话链路。
2. **回复文本来源**：气泡文案对应的类、字段、getter/setter；定位助手台词最终赋值位置。
3. **语音链路**：录音开始/结束、播放触发、口型动画事件挂载点。
4. **网络层**：云端请求类、域名、请求/响应结构；判断原版是否已有本地推理 `.so`。
5. **原生库存在性**：解包目录 `lib/arm64-v8a/` 下是否已有推理 `.so`。

### 5.2 Smali 类与方法核对规则
- Java 类编译到 Smali 后路径为 `smali/<包路径>/<类名>.smali`；方法签名保留参数类型与返回值。
- `native` 方法在 Smali 中带 `native` 标志；方法名、参数类型不能与 C 侧导出函数错位。
- 接入本地推理时，把原版“发网络请求取回复”的调用点，替换为调用 `NativeLlamaBridge`：先扫描 GGUF → 加载上下文 → `PromptComposer` 组装提示词 → 生成回复 → 回写给气泡文本字段。
- 线程：Smali 侧新启线程执行推理；不要在主线程加载模型或生成文本。

### 5.3 回编译前后检查清单
- [ ] `lib/arm64-v8a/` 内 `.so` 与 ABI 一致（arm64-v8a）。
- [ ] JNI 库名与 `System.loadLibrary` 参数一致；无缺名 `UnsatisfiedLinkError`。
- [ ] Smali 调用的类名、方法名、参数类型与 Java 骨架一致。
- [ ] 提示词包含系统设定、记忆、历史；当前输入未丢。
- [ ] GGUF、分词器、记忆目录路径来自外置存储，不硬编码进 APK。
- [ ] 网络云端调用保持可选默认关闭；离线主链路不发起请求。

## 6. 与现有脚本 / 文档的衔接
- `scripts/ndk-build-inference.sh`：CMake 命令骨架；参数以本文第 1 节为准补齐。
- `docs/integration.md`：推理模块加载流程、语音与记忆模块边界。
- `docs/reverse-workflow.md`：JADX / Smali 定位原版对话链路的前置流程。
- `docs/packaging.md`：`.so` 进入 `lib/arm64-v8a/` 后再回编译、对齐、签名、回装。
