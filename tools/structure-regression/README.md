# 构建与静态检查工具

| 工具 | 用途 |
|---|---|
| [EventSubscriberNames.java](EventSubscriberNames.java) | 检查 Forge 事件订阅代理名称冲突，接入 Gradle check |
| [API 准备说明](direct-compat/README.md) | 按固定版本与哈希准备编译依赖 |
| [check-boundaries.mjs](check-boundaries.mjs) | 检查可选兼容的依赖边界与 Mixin 路径 |
| [check-boundaries.test.mjs](check-boundaries.test.mjs) | 检查边界规则的反例 |
| [extract-page-overlay.py](preface-preview/extract-page-overlay.py) | 从正式日记背景生成书签遮罩，支持 --check |

在仓库根目录运行静态检查：

```text
node tools/structure-regression/check-boundaries.mjs
node tools/structure-regression/check-boundaries.test.mjs
```

遮罩生成使用 Python 和 Pillow，读取现有 Java 布局常量及正式背景图片。
