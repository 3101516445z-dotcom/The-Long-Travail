# 固定 API 编译依赖

可选兼容通过固定版本 API 编译，依赖清单与准备脚本共同约束输入。

## 编译依赖

`direct-compat-api.json` 记录固定 API JAR 的 SHA-256；依赖不提交、不内嵌，不自动成为运行依赖。准备脚本只读取指定目录内的 JAR，支持从万法皆通 all.jar 提取内部模组。

```powershell
python tools/dependencies/prepare-direct-compat.py --search <模组目录> --search <额外API目录>
.\gradlew.bat build --offline --no-daemon
```

缺少的固定 API 会集中列出，脚本不会用其他版本静默替代。RSI 固定使用 1.5.3 API，旧版适配已移除。可用 `-PcompatApiDirectory=<目录>` 指定非默认目录。此目录中的文件名须为锁文件键名加 `-local.jar`。
