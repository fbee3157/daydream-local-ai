# 逆向提取、解包与代码定位流程

## 1. 前置工具
- ADB（Android SDK Platform Tools）
- Apktool（解包 / 回编译资源与 Smali）
- JADX（阅读 Java / Kotlin 业务逻辑）

## 2. 提取 APK（Mac + Quest 3）

### 2.1 连接校验
```bash
adb devices
```
- `device`：连接正常。
- `unauthorized`：回到头显重新授权，拔线重连。
- 空列表：更换数据线 / USB-C 口，确认头显未待机。

### 2.2 定位包名
```bash
adb shell pm list packages | grep -i dream
```
记录返回的包名（以实际输出为准）。

### 2.3 查询路径并拉取
```bash
adb shell pm path <包名>
mkdir -p ~/QuestApk && cd ~/QuestApk
adb pull <base.apk 路径> ./base.apk
# 分包：对每个 split_config.*.apk 逐个 adb pull
```

## 3. 解包
```bash
apktool d base.apk -o daydream_decoded
```
产出：`smali/`、`res/`、`AndroidManifest.xml`、`lib/` 等目录。

## 4. 代码定位清单（JADX）
1. 对话输入框与消息发送逻辑。
2. Emma 对话气泡、台词文本来源类与方法。
3. 语音录制、播放、口型动画触发点。
4. 网络请求层：云端对话接口域名、请求 / 响应结构。
5. 是否存在原生推理库（`lib/` 下 `.so`），判断原版为云端还是本地推理。

## 5. 判断原版对话链路
- **云端**：网络请求类访问外部 API；应用内无推理 `.so`。本地模型通过挂钩响应或替换对话调用接入。
- **本地**：`lib/` 内含推理原生库；对话逻辑直接调用本地推理。
- 原版后端是否仍在线：真机试聊或检查解包后的网络配置。

## 6. 注意事项
- 原素材资源（模型、服装、场景）只复用不改动；不提取对外分发。
- Smali 修改需对应 JADX 中的类与方法；回编译前核对资源与清单一致性。
