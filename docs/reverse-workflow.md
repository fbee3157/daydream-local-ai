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

## 6. 一键审计脚本
`scripts/reverse-audit.sh` 串联连接检查、包名定位、APK/分包拉取、apktool 解包与只读扫描，输出审计清单：
```bash
# 不传包名：先列出候选包名再交互输入；也可直接传入完整包名
./scripts/reverse-audit.sh
./scripts/reverse-audit.sh <包名>
```
脚本产出 `~/QuestApk/audit-report.txt`：原生 `.so` 清单、Smali 目录、网络/对话/语音/场景关键词命中、素材目录命中，以及待 JADX 人工核对项。APK 与解包产物为本地私有，不入库、不分发。

### 6.1 离线解包审计（无需头显连接）
解包完成后，可在任意机器上对解包目录跑静态审计，不依赖 ADB：
```bash
./scripts/static-audit-decoded.sh <解包目录> [报告输出路径]
```
报告覆盖 Manifest 权限与导出组件、原生库 ABI 与 `.so`、素材目录清单、关键词命中文件，以及待 JADX 核对项。

## 7. 注意事项
- 原素材资源（模型、服装、场景）只复用不改动；不提取对外分发。
- Smali 修改需对应 JADX 中的类与方法；回编译前核对资源与清单一致性。
