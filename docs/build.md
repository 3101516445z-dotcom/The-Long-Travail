# 源码构建

准备 JDK 17 和 Node.js 18 或更新版本，在项目根目录通过仓库附带的 Gradle Wrapper 构建。Node.js 用于合并和校验全部五种语言文件，无需安装 npm 依赖。当前 Wrapper 固定为 Gradle 8.8，源码编译目标为 Java 17。

Gradle 默认调用 PATH 中的 `node`；需要指定程序时，可在构建命令后追加 `-PnodeExecutable="Node.js 可执行文件的绝对路径"`。语言源文件位于 `src/main/lang/`，构建时由 `tools/` 下的脚本合并并校验。

首次构建前，按[编译依赖说明](../tools/dependencies/README.md)准备可选联动 API。

Windows：

```text
.\gradlew.bat build jarJar
```

Linux / macOS：

```sh
./gradlew build jarJar
```

构建产物位于 `build/libs/`，安装时选择包含内嵌依赖的 `The_Long_Travail-<版本>.jar`。带 `-slim` 后缀的是精简包。Curios 需另行安装。

开发运行目录相对于工程根目录解析：`runClient` 使用 `../test-environments/minimal-client/`，`runServer` 使用 `../test-environments/minimal-server/`。

离线构建可在缓存齐全后追加 `--offline`。要使用本地车万女仆 API，可指定 `-PmaidApiJar=<JAR路径>`。依赖版本见 [build.gradle](../build.gradle) 和[可选依赖配置](../gradle/optional-compat.gradle)。

Gradle Wrapper 使用 Apache-2.0 许可证，第三方依赖沿用各自的许可证。
