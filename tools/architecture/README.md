# 架构与静态检查

| 工具 | 用途 |
|---|---|
| [EventSubscriberNames.java](EventSubscriberNames.java) | Forge 事件订阅代理名称检查，接入 Gradle check |
| [check-boundaries.mjs](check-boundaries.mjs) | 可选兼容依赖与 Mixin 路径检查 |
| [check-boundaries.test.mjs](check-boundaries.test.mjs) | 边界规则反例验证 |

从仓库根目录运行：

```text
node tools/architecture/check-boundaries.mjs
node tools/architecture/check-boundaries.test.mjs
```

检查依赖 Node.js、主源码和 Mixin 清单；结果写入标准输出，不修改生产文件。事件订阅检查使用 Gradle 编译产物，由 `verifyEventSubscriberNames` 执行。

[固定 API 准备](../dependencies/README.md) · [资源生成](../resources/README.md)
