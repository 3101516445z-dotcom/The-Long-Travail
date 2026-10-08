# The Long Travail / 苦旅

由 **Phoebe_Real** 制作的 Minecraft Forge 模组。模组围绕不可轻易卸下的核心饰品“苦旅”展开：玩家需要背负六种恶意踏上旅程，探索随机生成的群系与结构条件，并将恶意永久反转为见证。

## 环境要求

- Minecraft 1.20.1
- Forge 47.4.20 或更高的 47.x 版本
- Java 17
- Curios 5.14.1 或更高的兼容版本（必需）

## 安装

1. 安装 Minecraft 1.20.1 对应的 Forge 与 Curios。
2. 将本模组 JAR 放入游戏实例的 `mods` 文件夹，联机时客户端与服务端均须安装，并使用相同的版本。
3. 启动游戏。

## 主要内容

- 佩戴核心饰品“苦旅”，背负六种恶意，可以在探索中逐一将它们反转为见证。
- 通过旅行日记查看旅程记录，用六种解密之石揭开各篇章的反转条件。
- 活用“归乡”、“全修”、“路引”调整“苦旅”的状态，以控制游玩难度。
- 收集六大类主题物品，体验多种多样的功能组合，考虑恶意与见证的取舍，提升游戏趣味性。
- 高度自定义的配置，可以按自己的玩法调整模组的实现和功能，兼容性高。

## 可选联动

| 模组 | 核对版本 | 联动内容 |
| --- | --- | --- |
| JEI | 15.20.0.132 | 花饰合成与选花、配花、洗花展示；阅读日记时隐藏 JEI 覆盖层。 |
| Caelus | 3.2.0+1.20.1 | 让 Caelus 识别**伊卡洛斯**提供的飞行能力，并遵守禁飞限制。 |
| Embeddium | 0.3.31+mc1.20.1 | 适配的**剑与灯**动态照明。 |
| Sounds | 2.2.1+1.20.1+forge | 按苦旅规则过滤效果变化通知音，不屏蔽普通喝药或装备音效。 |
| FTB Teams | 2001.3.2 | 兼容本模组的队友识别判定。 |
| Gravestone | 1.20.1-1.0.35 | 在墓碑保存和恢复物品时，校正**死者之书**及日记相关数据 |
| Modern UI | 3.12.0.1 | 适配名称和日记文字的字体渲染 |
| Enigmatic Legacy | 2.30.1 | 使用**死者苏生**时，避免消耗绑定的重生锚 |

这些联动模组均为可选，不安装也可以游玩苦旅。光影方面，**剑与灯**曾在 Embeddium 0.3.31、Oculus 1.8.0 与 Complementary Reimagined r5.8.1 的独立测试环境中验证，其他组合的显示效果可能不同。

## 配置与常见操作

配置位于 `config/the_long_travail/`，首次启动时自动生成。各主题设置放在 `aspects/` 下，战利品统一在 `general.toml` 中调整，详见 [配置说明](docs/configuration-controls.md)。

苦旅造成的僵硬和视觉剥夺不能靠负面效果免疫或净化解除，但可以喝牛奶解除。管理员也可使用以下命令清除自身状态：

```text
/the_long_travail stiff clear
/the_long_travail visual_deprivation clear
```

普通 `/effect` 命令只能添加或移除这两个状态的图标，不会改变实际限制。

## 源码构建

使用 Gradle Wrapper 构建正式包：

```text
gradlew.bat jarJar
```

首次构建需要联网，Gradle 会自动下载依赖；缓存齐全后可追加 `--offline`。成品位于 `build/libs/`，请选择不带 `-slim` 后缀的 JAR。

Caelus、Embeddium 和 JEI API 仅用于编译，不打包进成品，也不会自动加载到开发运行环境。MixinExtras 随正式包内嵌，Curios 需另行安装。具体版本及仓库地址见 `build.gradle`。

`tools/` 提供算法、资源检查和服务端回归测试。常用检查命令：

```text
gradlew.bat -I tools/audit.init.gradle auditStandalone
node tools/verify-diary-resources.cjs
```

服务端测试还需准备 EULA 文件，部分兼容测试需要额外的模组依赖。其他模组接入苦旅时，可参考 [状态查询 API](docs/state-api.md)。

## 许可证

本项目的源代码、资源与文档均采用 [Mozilla Public License 2.0](LICENSE) 许可。
