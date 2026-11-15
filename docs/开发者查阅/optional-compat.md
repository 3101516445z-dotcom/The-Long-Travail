# 可选依赖接入

## 依赖与加载

第三方专用实现位于 `src/main/java/com/thelongtravail/compat/`，按目标模组分包；专用注入位于 `compat/mixin/`，由[可选 Mixin 配置](../../src/main/resources/mixins.the_long_travail.compat.json)注册。

公共入口使用 Java、Minecraft 或 Forge 类型。确认目标模组存在后，再加载引用第三方 API 的实现类。客户端实现通过客户端入口装配，公共初始化保持物理端隔离。

编译依赖由 [optional-compat.gradle](../../gradle/optional-compat.gradle) 及[依赖准备说明](../../tools/dependencies/README.md)管理。`compileOnly` 依赖需要在开发运行环境另行提供。

## 注入契约

`OptionalCompatPlugin` 根据目标模组、类与方法布局决定是否应用可选 Mixin。新增注入应明确目标版本、完整方法描述符和调用位置，并在现有插件中加入对应检查。

目标缺席或契约不匹配时，同一适配的注入和运行入口应采用一致的启用状态。引用第三方类型的字段、方法签名和静态初始化同样需要加载隔离。

## 状态与生命周期

适配状态归属于实际实体、物品或会话。卸装、实体退出及服务器停止时撤销本来源的属性和临时记录；延迟回调保留发起时的对象或身份，并在执行前复核。

查询玩家主题资格使用[状态 API](state-api.md)，接入外部库存使用[携带 API](carrying-api.md)，装备恢复使用[饰品恢复入口](accessory-equipment.md)。通用机制留在公共层，第三方库存操作、事件和类型转换由适配实现负责。
