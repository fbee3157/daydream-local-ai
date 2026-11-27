# 重打包、对齐、签名与回装

## 1. 回编译
```bash
apktool b daydream_decoded -o daydream_modified.apk
```

## 2. Zip 对齐
```bash
zipalign -f -v 4 daydream_modified.apk daydream_aligned.apk
```

## 3. 签名
先生成调试密钥库（若尚无），再用 apksigner 签名：
```bash
apksigner sign --ks <密钥库路径> --ks-key-alias <密钥别名> daydream_aligned.apk
```
校验签名：
```bash
apksigner verify --verbose daydream_aligned.apk
```

## 4. 回装 Quest 3
```bash
adb install -r daydream_aligned.apk
```
- 分包应用：按 split APK 安装方式依次安装 base 与各 config 分包。
- 重打包后签名变更，可能触发系统 / 应用签名校验；仅限个人研究侧载。

## 5. 部署后头显目录
```
/storage/emulated/0/DayDream/
├── models/
├── tokenizer/
├── memory/
└── config.json
```
模型权重不随 APK 打包；应用更新只替换代码与 `.so`。

## 6. 脚本参考（scripts/）
- `adb-extract.sh`：提取 APK / 分包。
- `apktool-decode.sh`：解包 APK。
- `ndk-build-inference.sh`：NDK 编译推理库。
- `repackage-sign.sh`：回编译、对齐、签名。
- `adb-install.sh`：回装到头显。
