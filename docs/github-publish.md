# GitHub 仓库初始化与发布步骤

## 1. 仓库可公开 / 不可公开内容划分

### 1.1 可提交（公开仓库）
- `docs/` 流程文档；
- `scripts/` 构建、提取、签名、回装脚本；
- `app/assets/runtime-config.json` 配置骨架（不含真实密钥）；
- `inference/examples/`、`modules/` 接入示例代码与模块说明；
- `reference/` 工具链与模型备忘；
- `.gitignore`、`README.md`、`LICENSE`。

### 1.2 禁止提交（本地私有，已在 .gitignore 排除）
- 原版 APK、base.apk、split_config.*.apk；
- 原版角色模型、服装、场景、纹理、音频等素材；
- GGUF 权重、分词器二进制、离线 STT / TTS 模型二进制；
- 签名密钥库、含 API Key 或私有路径的本地配置；
- NDK / Apktool 编译产物 `.so`、临时构建目录。

## 2. 本地 Git 初始化（Mac 终端）

```bash
cd ~/Doubao/chats/38456930251162114/daydream-local-ai

# 初始化仓库
git init

# 确认 .gitignore 已排除私有文件
cat .gitignore

# 暂存文档、脚本、示例与配置
git add README.md LICENSE .gitignore docs/ scripts/ reference/ app/assets/runtime-config.json inference/examples/ modules/

# 首次提交
git commit -m "init: Quest 3 llama.cpp 离线 AI 改造项目文档、脚本与配置骨架"
```

## 3. 关联 GitHub 远程仓库并推送

1. 在 GitHub 网页创建空仓库（不勾选自动生成 README / LICENSE）。
2. 关联远程地址：
```bash
git remote add origin https://github.com/<用户名>/<仓库名>.git
```
3. 推送主分支：
```bash
git branch -M main
git push -u origin main
```

## 4. Release 发布规范
- Release 仅打包代码、脚本、文档与配置；**不附带原版 APK、素材、模型权重**。
- 版本号按语义化版本（如 v0.1.0）；Release Notes 写明本机 llama.cpp 推理、离线语音、权重自由导入等能力。
- 仓库 README 中注明：原版应用与素材版权归原作者，本仓库代码仅用于个人研究参考。

## 5. 模型权重导入说明（仓库文档内说明，不提交权重）
- 部署后把 GGUF 权重放入头显 `/storage/emulated/0/DayDream/models/`。
- 分词器放入 `/storage/emulated/0/DayDream/tokenizer/`。
- 应用扫描模型目录读取 GGUF；替换权重文件即可切换模型，无需重编 APK（同一 GGUF 格式前提下）。

## 6. 合规复查清单（推送前）
- [ ] 无原版 APK / 分包文件被暂存；
- [ ] 无角色模型、服装、场景素材文件；
- [ ] 无 GGUF / 离线语音模型二进制；
- [ ] 配置中无真实 API Key、无私有绝对路径泄露；
- [ ] 无签名密钥库文件；
- [ ] README 已注明版权归属与个人研究用途。
