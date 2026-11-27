# 工具链备注

## ADB
- `adb devices`：查看连接设备与授权状态。
- `adb shell pm list packages`：列出已安装包名。
- `adb shell pm path <包名>`：查询 APK 安装路径。
- `adb pull <远端路径> <本地路径>`：拉取 APK / 文件到头显外的电脑。

## Apktool
- `apktool d <apk> -o <目录>`：解包资源与 Smali。
- `apktool b <目录> -o <apk>`：回编译。

## JADX
- 图形化阅读反编译后的 Java / Kotlin 代码，定位对话、语音、网络请求类与方法。

## NDK / CMake
- 编译推理库原生 `.so`，目标架构 arm64-v8a（Quest 3）。
- 产物放入 APK 的 `lib/arm64-v8a/`。

## 签名工具
- `zipalign`：安装包 4 字节对齐。
- `apksigner`：APK 签名与签名校验。

## 推理框架备忘
- llama.cpp：GGUF，导入模型成本最低，默认首选。
- MNN-LLM：MNN 格式，移动端优化备选。
- ExecuTorch：PTE，PyTorch 导出链路，运行时体积最小。
- LiteRT-LM：Google 端侧框架，Gemma 适配较好，Quest XR 需真机验证。
