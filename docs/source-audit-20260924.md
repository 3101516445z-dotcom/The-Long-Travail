# 苦旅客户端与服务端源码审查

检查对象：重命名后的 `my_mods/The Long Travail`。本次仅审查，没有修改运行源码、编译或替换 JAR。核对了事件、物品、NBT、网络、Curios 管理、GUI、字体渲染和 Mixin；关键事件时序对照本地 Forge 1.20.1-47.4.20 字节码。未启动独立服务端，也未进行联机或 GPU 性能实测。

## 优先修复

### 1. 穷遐之见证首次强化效果会被覆盖【高，服务端逻辑确定】

位置：`src/main/java/com/thelongtravail/event/TravailEvents.java:314`。

`MobEffectEvent.Added` 回调中递归调用 `player.addEffect` 加入强化版。实际 Forge 调用顺序为：读取已有效果 → 发布 Added 事件 → 没有旧效果时将原始实例写入 activeEffects。因而首次获得某效果时，回调加入的高等级实例被外层随后写入的低等级实例覆盖。ThreadLocal 只能阻止无限递归，不能修正写入顺序。已有同类效果时走 update 分支，表现可能不同。

建议在效果正式合并前修改传入实例，或使用经过验证的合并后处理；避免在 Added 内对同一效果重入。至少验证首次获得、续时、覆盖低等级、原本已有高等级四种情况。

### 2. 铁砧免费操作的双端判断不一致【中，代码路径确定】

位置：`mixin/AnvilMenuMixin.java:35`、`:44`。

所有见证判断要求 ServerPlayer，客户端恒为 false。配置消耗上限为 0 时，服务端 mayPickup 允许领取，客户端仍走原版 cost > 0 判定，因此拒绝正常客户端取出操作。大于等于 40 的配置还涉及原版客户端“过于昂贵”显示，需要同步处理。默认 20 不能据此断言必然失效。

建议客户端使用已同步见证与配置进行显示/可取预测，服务端继续做最终校验；验证上限 0、20、40 及不同端配置。

### 3. 配置热重载访问玩家列表没有切回服务器线程【中，条件性并发风险】

位置：`network/TravailNetwork.java:60`。

本地 Forge ConfigWatcher.run 直接触发 Reloading 回调。当前回调立即遍历在线玩家并发送配置，未通过 server.execute 调度。文件监控线程触发重载且同时登录/退出时，有并发访问玩家列表的风险。不是每次重载必然崩溃。

建议把在线玩家访问、配置快照生成与广播放到服务器线程。

### 4. 共用物品提示方法直接调用 Minecraft 客户端类【中，专服兼容风险】

位置：`item/LongTravailItem.java:133`、`item/HomecomingItem.java:34` → `client/DiaryDialogue.java`。

DiaryDialogue.append 直接访问 Minecraft.getInstance、字体和窗口。普通服务端不绘制 tooltip，不能据此断言启动即崩溃；但服务器侧其他模组若调用通用物品描述 API，就会走入客户端专用代码。

建议共用方法只构造端无关 Component，测宽、分行和光泽标记移入客户端提示事件，保证服务端读取描述有安全退路。

## 性能与健壮性

### 5. 幻翼清理扫描整个维度【中，服务端性能风险】

位置：`event/TravailEvents.java:507`。

每位拥有无垠之见证的玩家到达检查周期，都会 getAllEntities 遍历当前维度全部已加载实体，再按类型和距离筛选。isDue 使用世界时间取模，多名玩家会集中在同一 tick 执行。

建议按玩家附近 AABB 查询 Phantom，再按球形距离过滤；可错开玩家检查 tick。实际卡顿程度取决于在线人数和已加载实体数量。

### 6. 数量配置允许百万级直接执行【中，异常配置下风险】

位置：`config/TravailConfig.java:120`、`:149`，以及事件中的随机效果循环。

额外饰品槽、单次随机效果数等允许 1,000,000。前者会造成巨大槽位分配/同步，后者可让一次攻击执行百万次随机效果处理。配置合法并不代表运行成本可承受；不是普通玩家通过网络即可利用的漏洞。

建议数量型配置使用符合玩法的上限，并对运行时输入保底限制。随机池解析也可在配置变更时缓存。

### 7. 客户端绘制重复工作较多【优化项，未做性能实测】

位置：`client/TravelDiaryScreen.java:165`、`:236`，`client/DiaryFontEffects.java:76`，`mixin/DiaryFontMixin.java:43`。

日记每帧重新分段、测宽、构造文字列表；反转条件显示还会按条目重新解析配置池。名称外光晕额外绘制 2×8 次文字，每次文字绘制又创建渲染包装映射。普通文本也经过 Font Mixin 的标记检测。

建议缓存页面布局及条件名称映射，在语言/字体资源、页面、NBT、解密状态和配置变化时失效。光晕可保留现有外观，先测绘制成本再调整采样；不能把 16 次文字提交直接等同于 16 次 GPU draw call。

### 8. 退出世界未清空服务端配置缓存【低，客户端生命周期】

位置：`client/TravailClientEvents.java:68`、`network/TooltipConfigSync.java:13`。

退出时重置 JourneySync，但 clientValues/clientPools 没有 reset。再次登录收到完整配置后会覆盖，所以不是必然永久串服；但退出后或新服配置到达前仍持有上一服务器数值。

建议退出时一并重置配置缓存，并统一清理输入状态。

### 9. 读取见证状态会修改空 NBT【低，共用数据层】

位置：`data/LongTravailData.java:201`。

root 使用 getOrCreateTag，hasWitness 等读取路径也会创建空标签，包括客户端打开未初始化苦旅预览时。读取不应改变物品状态，这可能增加物品比较/同步差异。

建议区分只读 root 与写入 root，读取缺失数据时返回空值而不写回。

### 10. 飞行速度和吸收生命恢复会覆盖其他状态变化【中，模组交互风险】

位置：`event/TravailEvents.java:472`，`helper/TrueDamage.java:16`。

飞行减速缓存一次原速度后持续强写，其他模组期间改变飞行速度时会被覆盖，退出减速后也恢复旧快照。流体伤害则暂时清空吸收生命，hurt 返回后恢复原值；hurt 中若移除吸收效果、触发图腾或由其他模组改变吸收生命，旧快照可能覆盖新状态。

建议单独设计与其他来源兼容的速度修饰策略；吸收绕过应避免不区分 hurt 内状态变化地恢复旧值。此项未做组合模组实测。

## 确认可清理的旧实现

- `LongTravailItem.aspectTitle`、`aspectGaze`：运行源码没有调用；aspectColor、colored 和 12 个恶意/见证颜色常量只被旧标题链引用，可一起清理。
- `TravelDiaryScreen.drawWrapped`：无调用。VISIBLE_LINES 仍用于 PageUp/PageDown，不能一并当作废弃常量删除。
- `TravailCurios.appliedExtraSlots`：无调用；setAppliedExtraSlots 仍不断写入 LongTravailAppliedExtraSlots，但当前算法直接读取 Curios modifier，旧字段没有运行读取者。可停止写入并删除死方法，不必强制清洗玩家旧 NBT。
- `assets/the_long_travail/textures/gui/travel_diary.png`：旧 GUI 背景无运行引用，约 1.34 MiB；当前使用 diary_base 和 diary_bookmark。不要误删同名物品/槽位纹理。
- `Introduction.txt:60` 的 J 键说明，以及 `:125` 起的旅人纹章说明已过时。
- DiaryPageProse 保留占位符再替换成“旅人”，属于可简化实现，不是缺陷；若未来恢复可变称呼，也可保留。
- 构建缓存 regression-classpath.txt 仍有重命名前绝对路径。它是生成文件，不是游戏源码缺陷；下次运行相关手工回归测试前应重新生成。

## 检查中未发现明显错误的部分

- 客户端事件订阅指定 Dist.CLIENT，绘制/JEI/Sounds Mixin 放在 client 列表；没有看到普通注册流程无条件加载 Screen 或 Shader。
- 网络包限制 PLAY_TO_CLIENT，处理通过 enqueueWork；未看到客户端发包直接篡改解密或见证的入口。
- 石头解密与归乡的物品操作由 ServerPlayer 分支执行；解密存入玩家持久数据，支持玩家 Clone 和登录/重生/跨维同步。
- 当前饰品槽管理会在加成值不变时提前返回；未再看到旧版每秒撤销重加槽位的逻辑。归乡的返还路径先清空槽位，再放入背包/掉落。
- JEI 与 Sounds 兼容使用 Pseudo，可缺省安装；但现有模组升级后方法/字段改名仍可能因 required/defaultRequire=1 导致加载失败。这是版本兼容风险，不能把兼容注入当废弃代码随意删除。

优先顺序：穷遐效果时序 → 铁砧双端一致 → 热重载线程与专服 tooltip 隔离 → 性能/配置限制 → 清理旧代码资源。结论为源码审查及本地依赖字节码核对，不能替代客户端、独立服务端联机回归。
