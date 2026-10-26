// Architecture inventory only; never executes game code or modifies Java sources.
import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import { fileURLToPath } from 'node:url';
import { inputSnapshot, validateLinks } from './checks.mjs';
const here = path.dirname(fileURLToPath(import.meta.url));
const doc = path.resolve(here, '..');
const root = path.resolve(doc, '../../..');
const source = path.join(root, 'src/main/java');
const check = process.argv.includes('--check');
const hash = s => crypto.createHash('sha256').update(s).digest('hex');
const slash = s => s.split(path.sep).join('/');
function walk(dir) {
  return fs.readdirSync(dir, {withFileTypes:true}).flatMap(e => e.isDirectory()
    ? walk(path.join(dir,e.name)) : [path.join(dir,e.name)]).sort();
}
const groups = {
  common:['公共基础','通用基础'], data:['旅程与运行数据','核心机制（包级暂定）'],
  api:['公开查询与写入入口','公共接口'], helper:['装备与战斗协作','协调机制（包级暂定）'],
  combat:['战斗属性与重放','核心机制'], config:['配置管理','基础设施'],
  network:['网络同步','平台接入'], registry:['注册与装配','平台接入'],
  event:['公共事件','流程协调'], mixin:['原版注入桥接','平台接入'],
  compat:['外部模组适配','兼容接入'], client:['客户端展示与输入','客户端接入'],
  menu:['容器菜单','公共端容器接入'],
  item:['物品入口','玩法入口'], entity:['自定义实体','玩法实现'], effect:['效果类型','玩法入口'],
  loot:['奖励与来源','公共机制'], growth:['生长机制','公共机制'], diagnostics:['运行诊断','基础设施'],
  flourishing:['繁茂','具体玩法'], abyss:['归墟','具体玩法'], farreach:['穷遐','具体玩法'],
  valley:['幽谷','具体玩法'], underworld:['冥府','具体玩法'], boundless:['无垠','具体玩法']
};
const overrides = {
  ColoringItem:['施色与名称显示','物品提示与服务端菜单入口'],
  NameColoring:['施色与名称显示','物品栈样式记录与名称组件变换'],
  ColoringMenu:['施色与名称显示','公共端槽位、选项同步与服务端领取提交'],
  ColoringScreen:['施色与名称显示','客户端按钮、标签排版与容器绘制'],
  ColoringNameMixin:['施色与名称显示','完整物品名称返回后的公共端注入'],
  AffixNameStyle:['客户端与兼容','神化完整名称样式及还原品质颜色'],
  ImmortalBudItem:['繁茂物品入口','永生芽物品属性、主题名称与说明'],
  ImmortalBud:['繁茂','独立实例标记、赋予与掉落物保护资格'],
  ImmortalBudRecipe:['繁茂','动态无序赋予、目标复制与剩余物规则'],
  ImmortalBudDamageMixin:['原版注入桥接','Item 的 Forge 耐久写入转发与增伤拦截'],
  ImmortalBudHurtMixin:['原版注入桥接','ItemStack.hurt 前置拦截正数耐久损耗'],
  ImmortalBudDropMixin:['原版注入桥接','ItemEntity 火爆伤害拦截与自然寿命维护'],
  TravailClientEvents:['客户端与兼容','客户端装配、阅读交互与提示协调'],
  CatalogSession:['客户端与兼容','图鉴页面会话与布局入口'],
  UnderworldClient:['客户端与兼容','苏生资格展示与请求按钮'],
  RainClient:['客户端与兼容','施雨输入和本地缓存清理'],
  UnderworldPacket:['客户端与兼容','死亡资格同步和苏生请求协议'],
  RainCastPacket:['客户端与兼容','施雨请求连接与实例复核'],
  OptionalCompatPlugin:['客户端与兼容','可选Mixin加载前契约分流'],
  OptionalCompatState:['客户端与兼容','单次启动兼容门控状态'],
  FlourishingAccessoryItem:['繁茂物品入口','春日一隅与喜欢的心情构造分支、交互和装备回调'],
  FarReachAccessoryItem:['穷遐物品入口','黄金时代与伊卡洛斯构造分支和开关'],
  UnderworldAccessoryItem:['冥府物品入口','一千年后佩戴与死者之书携带入口'],
  BoundlessAccessoryItem:['无垠物品入口','逍遥游与星之声佩戴入口'],
  AbyssItem:['归墟物品入口','完美流体与午夜拉菲名称、说明和佩戴入口'],
  PhosphophylliteItem:['幽谷物品入口','磷叶石佩戴与卸下对账入口'],
  UnderworldLedger:['复活与终结','消耗、见证与死亡事实持久化'],
  UnderworldStorage:['复活与终结','玩家、掉落物和墓碑校正协调'],
  GravestoneAccess:['复活与终结','墓碑反射访问与运行时契约'],
  GravestoneStorage:['复活与终结','墓碑加载队列、校正与位置索引扫描'],
  BookBlockEntityMixin:['复活与终结','墓碑设置世界后的延迟校正入口'],
  BookGravestoneMixin:['复活与终结','墓碑设置死亡记录后的直接校正入口'],
  UnderworldMaintenance:['复活与终结','玩家账本分步维护调度'],
  NbtReconcileCursor:['复活与终结','带挂接校验的分步NBT遍历'],
  BookRevival:['复活与终结','死亡资格、落点请求与苏生协调'],
  RevivalPosition:['复活与终结','基于方块碰撞的最近落点搜索'],
  AzraelExecution:['复活与终结','分阶段处决与死亡结果判定'],
  FinalExecution:['复活与终结','死亡实例锁与最终终结协调'],
  ExecutionCleanup:['复活与终结','非玩家存储脱离与移除通知'],
  ExecutionWatch:['复活与终结','旧实例回填观察与清理重试'],
  ExecutionStorage:['复活与终结','终结存储、实例保护与复核桥接'],
  ExecutionServerLevelMixin:['复活与终结','终结存储、实例保护与复核桥接'],
  ExecutionManagerMixin:['复活与终结','终结存储、实例保护与复核桥接'],
  ExecutionLookupMixin:['复活与终结','终结存储、实例保护与复核桥接'],
  ExecutionTickListMixin:['复活与终结','终结存储、实例保护与复核桥接'],
  ExecutionSectionsMixin:['复活与终结','终结存储、实例保护与复核桥接'],
  ExecutionSectionMixin:['复活与终结','终结存储、实例保护与复核桥接'],
  ExecutionMultiMapMixin:['复活与终结','终结存储、实例保护与复核桥接'],
  ExecutionChunkMapMixin:['复活与终结','终结存储、实例保护与复核桥接'],
  ExecutionTrackedAccessor:['复活与终结','终结存储、实例保护与复核桥接'],
  ExecutionRawEntity:['复活与终结','终结存储、实例保护与复核桥接'],
  ExecutionTransientMixin:['复活与终结','终结存储、实例保护与复核桥接'],
  ExecutionClientLevelMixin:['复活与终结','终结存储、实例保护与复核桥接'],
  ExecutionTrackingMixin:['复活与终结','终结存储、实例保护与复核桥接'],
  ExecutionPersistenceMixin:['复活与终结','终结存储、实例保护与复核桥接'],
  ExecutionRenderMixin:['复活与终结','终结存储、实例保护与复核桥接'],
  ExecutionReceipts:['复活与终结','终结存储、实例保护与复核桥接'],
  ExecutionLease:['复活与终结','有限观察期限'],
  ExecutionLoot:['复活与终结','实际掉落观察与一次补偿'],
  ExecutionLootTable:['复活与终结','公共战利品收集器委托'],
  BookServerPlayerMixin:['复活与终结','伤害死亡作用域和重生锚桥接'],
  BookPlayerListMixin:['复活与终结','苏生维度落点与绑定点桥接'],
  AzraelDeathMixin:['复活与终结','Forge死亡取消结果桥接'],
  PerfectFluidRecipe:['外部库存复制','配方匹配、预览与原件返还'],
  PerfectFluidCopies:['外部库存复制','保护规则、单件复制与净化'],
  FluidCrafting:['外部库存复制','领取准备、提交与菜单恢复'],
  FluidCopyGraph:['外部库存复制','身份图、遍历与写入回滚协调'],
  FluidBackpackStore:['外部库存复制','精妙背包外部内容适配'],
  FluidRsStore:['外部库存复制','RS标准与共振盘内容适配'],
  FluidUnifiedStore:['外部库存复制','统一盘内容、文件及代理适配'],
  FluidAeStore:['外部库存复制','AE2资源键与数量格式适配'],
  FluidBeyondStore:['外部库存复制','物质球资源表无损转换'],
  PerfectFluidBackpacks:['外部库存复制','旧入口委托'],
  FluidCraftingClickMixin:['外部库存复制','结果槽点击领取桥接'],
  FluidCraftingQuickMoveMixin:['外部库存复制','结果槽快速移动桥接'],
  TimeStopManager:['时停与恢复','场域、重叠组与到期协调'],
  TimeStopExemptions:['时停与恢复','队友快照与有界归属查询'],
  TimeStopApi:['时停与恢复','公开只读冻结查询'],
  FrozenEntityClock:['时停与恢复','实体冻结计时与同刻去重'],
  FrozenBlockTasks:['时停与恢复','计划刻和通知转存恢复'],
  FrozenBlockEvents:['时停与恢复','带阻挡位置的方块事件存储'],
  IndexedFrozenQueue:['时停与恢复','区块索引、稳定次序与异常保留'],
  FrozenCoverage:['时停与恢复','场域覆盖版本与区块变化'],
  StarVoiceState:['时停与恢复','玩家星之声触发与冷却'],
  BoundlessEvents:['无垠生命周期','时停、梦境与服务器事件协调'],
  GoldenAgeActions:['六主题玩法','黄金时代登记、额度与转换提交'],
  GoldenAgeCompatDefaults:['六主题玩法','黄金时代兼容黑名单一次性初始化'],
  GoldenAgeInteractionMixin:['六主题玩法','黄金时代挖掘交互桥接'],
  GoldenAgeItemMixin:['六主题玩法','金块实体转换与合并限制'],
  GoldenAgeMenuDropMixin:['六主题玩法','菜单手动丢弃作用域'],
  GoldenAgePlayerDropMixin:['六主题玩法','玩家手动丢弃作用域'],
  GoldenAgePools:['六主题玩法','黄金时代注册表候选与玩家食物查询'],
  FarReachCombat:['六主题玩法','伊卡洛斯增伤、免疫与确认点燃'],
  IcarusCaelusApiMixin:['六主题玩法','Caelus默认结果的伊卡洛斯扩展'],
  IcarusCaelusCompat:['六主题玩法','Caelus拒绝查询与链接失败降级'],
  IcarusCapeMixin:['六主题玩法','伊卡洛斯可见时隐藏披风'],
  IcarusElytraLayerMixin:['六主题玩法','伊卡洛斯翅膀渲染替换'],
  IcarusEnvironment:['六主题玩法','伊卡洛斯时间曲线与露天查询'],
  IcarusFlight:['六主题玩法','伊卡洛斯开关、滑翔与可见装备查询'],
  IcarusLivingFlightMixin:['六主题玩法','持续滑翔资格与刻回调桥接'],
  IcarusLocalFlightMixin:['六主题玩法','本地起飞输入资格桥接'],
  IcarusPlayerFlightMixin:['六主题玩法','玩家起飞资格桥接'],
  BabelBrainMixin:['六主题玩法','巴别塔记忆写入与清理'],
  BabelBrainOwner:['六主题玩法','巴别塔Brain所有者契约'],
  BabelEndermanMixin:['六主题玩法','巴别塔末影人注视与目标桥接'],
  BabelLivingMixin:['六主题玩法','巴别塔伤害对象与Brain绑定'],
  BabelMerchantAccess:['六主题玩法','巴别塔交易容器访问'],
  BabelMerchantMenuMixin:['六主题玩法','巴别塔交易点击资格复核'],
  BabelMobMixin:['六主题玩法','巴别塔目标提交与AI清理'],
  BabelOfferMixin:['六主题玩法','巴别塔固定交易成本'],
  BabelPiglinMixin:['六主题玩法','巴别塔猪灵怒气桥接'],
  BabelProtection:['六主题玩法','巴别塔保护资格与挑衅记录'],
  BabelTargetingMixin:['六主题玩法','巴别塔战斗目标条件'],
  BabelTrades:['六主题玩法','巴别塔报价增量与交易资格'],
  BabelVillagerMixin:['六主题玩法','巴别塔价格保存与补货计数'],
  DonQuixoteCombat:['六主题玩法','堂吉诃德攻击快照与追加结算'],
  DonQuixoteState:['六主题玩法','堂吉诃德目标防护与冷却'],
  FloralCombat:['六主题玩法','花卉输出、减伤与反击'],
  FloralEvents:['六主题玩法','花卉属性对账、效果与死亡确认'],
  FloralPassives:['六主题玩法','花卉被动属性与有害时长'],
  FlowerData:['六主题玩法','春日一隅三花组合数据与生效查询'],
  GrowthAdapter:['六主题玩法','单次催生机会扩展契约'],
  GrowthScheduler:['六主题玩法','服务器共享预算与植物催生轮转'],
  SpringRecipes:['六主题玩法','春日一隅合成、加花与水洗'],
  DemonKingCombat:['六主题玩法','第六天魔王环境增伤、亡灵减伤与吸血'],
  SoulBannerKills:['六主题玩法','万魂幡死亡确认与实例去重'],
  DummySoulRules:['外部模组适配','假人命名空间收魂排除'],
  ArsNouveauAttributeRules:['外部模组适配','新生魔艺属性增幅排除规则'],
  UnderworldMaidAttackMixin:['外部模组适配','女仆冥府见证攻击类型桥接'],
  UnderworldRelicItem:['六主题玩法','第六天魔王与万魂幡装备依赖入口'],
  UnderworldRelics:['六主题玩法','冥府遗物装备、魂数与属性对账'],
  LorentzArrowAccessor:['六主题玩法','洛伦兹幻想箭矢入地状态访问'],
  LorentzArrowMixin:['六主题玩法','洛伦兹幻想箭矢受击作用域桥接'],
  LorentzFantasy:['六主题玩法','洛伦兹幻想箭矢快照、命中与虚空结算'],
  LorentzGeometryConfig:['六主题玩法','洛伦兹幻想爆头几何修正配置'],
  Affection:['六主题玩法','喜欢的心情绑定与有效目标缓存'],
  AffectionDeaths:['六主题玩法','喜欢的心情死亡绑定持久化账本'],
  Mistletoe:['六主题玩法','槲寄生叠级、到期与奖励协调'],
  MistletoeExpiryMixin:['六主题玩法','效果遍历后结算与异常撤销'],
  MidnightLafite:['六主题玩法','午夜拉菲夜间与药效处理入口'],
  LafiteInstantEffectMixin:['六主题玩法','午夜拉菲夜间与药效处理入口'],
  TimeStopSync:['时停与恢复','玩家场域视图筛选与差异发送'],
  PrecipitationScope:['六主题玩法','可嵌套降水查询世界作用域'],
  RainBiomeMixin:['六主题玩法','作用域内降水类型和雪冰规则'],
  RainCommandMixin:['六主题玩法','天气命令放弃技能托管'],
  RainEvents:['六主题玩法','降雨技能、玩家被动和状态同步协调'],
  RainFireMixin:['六主题玩法','玩家抗火查询桥接'],
  RainLevelAccessor:['六主题玩法','原始雷雨强度读取'],
  RainLevelMixin:['六主题玩法','位置降雨查询作用域'],
  RainRenderMixin:['六主题玩法','客户端降水渲染作用域'],
  RainServerMixin:['六主题玩法','技能天气推进与自然周期桥接'],
  RainState:['六主题玩法','雨天装备携带资格与效果查询'],
  RainWeather:['六主题玩法','主世界技能天气托管和持久化'],
  DreamState:['六主题玩法','入梦记录、计时与退出恢复协调'],
  DreamLandingSearch:['六主题玩法','同维度梦醒落点分步搜索与区块票据'],
  BoundlessEquipment:['六主题玩法','逍遥游和星之声运行装备查询'],
  CatalogFrame:['客户端与兼容','目录框架与渐隐提交顺序'],
  DiaryEntryHold:['客户端与兼容','日记和目录按住入口归属'],
  DiaryFontEffects:['客户端与兼容','字体材质、Shader 与资源代次'],
  DiaryFontMixin:['客户端与兼容','字体路由和光晕绘制桥接'],
  DiaryHoldProgress:['客户端与兼容','按住进度与衰减'],
  DiaryReadingTarget:['客户端与兼容','阅读物品来源与旅程身份绑定'],
  DiaryText:['客户端与兼容','字体去标记与多次绘制序列'],
  DrawScopedCache:['客户端与兼容','单次绘制身份缓存'],
  GuiGraphicsTooltipMixin:['客户端与兼容','阅读背景提示框抑制'],
  LanternEmbeddiumLightMixin:['客户端与兼容','可选网格顶点光照桥接'],
  LanternEntityLightMixin:['客户端与兼容','实体 packed light 采样桥接'],
  LanternLevelLightMixin:['客户端与兼容','世界 packed light 采样桥接'],
  LanternLightSnapshot:['客户端与兼容','只读分区光源快照'],
  ThemeTooltipMixin:['客户端与兼容','物品提示主题整理入口'],
  BoundlessPacket:['客户端与兼容','逍遥游请求、状态和提示协议'],
  EffectNotice:['客户端与兼容','效果音策略通知协议'],
  ItemSoundCue:['客户端与兼容','物品动作音效枚举协议'],
  LanternPacket:['客户端与兼容','剑与灯玩家光源状态协议'],
  RainMessagePacket:['客户端与兼容','雨技能操作提示协议'],
  RainPacket:['客户端与兼容','雨状态和提示快照协议'],
  StiffPacket:['客户端与兼容','僵硬剩余时间协议'],
  TimeStopCuePacket:['客户端与兼容','时停提示坐标协议'],
  TimeStopPacket:['客户端与兼容','时停场域快照与载荷校验'],
  VisualDeprivationPacket:['客户端与兼容','视觉剥夺状态和进度协议'],
  TimeStopClient:['时停与恢复','客户端维度视图与冻结查询'],
  TimeStopWorldMixin:['时停与恢复','实体、随机刻及方块事件桥接'],
  TimeStopPlayerMixin:['时停与恢复','服务端玩家更新桥接'],
  TimeStopEntityMixin:['时停与恢复','移动边界与实体计时桥接'],
  TimeStopLevelMixin:['时停与恢复','方块实体与方块修改桥接'],
  TimeStopScheduledMixin:['时停与恢复','计划刻调度、执行和查询桥接'],
  TimeStopNeighborMixin:['时停与恢复','邻居通知和形状更新桥接'],
  TimeStopFluidMixin:['时停与恢复','流体边界转存桥接'],
  FrozenContainers:['时停与恢复','容器和菜单来源冻结查询'],
  TimeStopChunkTicksMixin:['时停与恢复','计划刻容器变更版本'],
  TimeStopClientLevelMixin:['时停与恢复','客户端实体和环境更新桥接'],
  TimeStopCompoundAccessor:['时停与恢复','复合容器来源访问'],
  TimeStopConnectionMixin:['时停与恢复','服务端输入拦截和客户端纠正'],
  TimeStopEntityRenderMixin:['时停与恢复','冻结实体插值参数'],
  TimeStopExplosionMixin:['时停与恢复','爆炸目标方块过滤'],
  TimeStopHopperMixin:['时停与恢复','漏斗吸取与容器传输拦截'],
  TimeStopItemMergeMixin:['时停与恢复','冻结物品合并资格'],
  TimeStopLocalPlayerMixin:['时停与恢复','本地玩家更新冻结'],
  TimeStopMobPickupMixin:['时停与恢复','生物拾取冻结物品拦截'],
  TimeStopOrbMergeMixin:['时停与恢复','经验球合并与生成位置检查'],
  TimeStopParticleAccessor:['时停与恢复','粒子当前及旧坐标访问'],
  TimeStopParticleMixin:['时停与恢复','冻结粒子更新及提示豁免'],
  TimeStopPickupMixin:['时停与恢复','玩家拾取冻结物品和经验拦截'],
  TimeStopPistonMixin:['时停与恢复','活塞事件和邻居通知转存'],
  TimeStopProjectileAccessor:['时停与恢复','投射物主人身份访问'],
  TimeStopTicksAccessor:['时停与恢复','计划刻容器与下次执行表访问'],
  ConfigFiles:['配置与运行时数据','配置注册与默认池事件装配'],
  ManagedConfig:['配置与运行时数据','单文件候选发布与可写状态'],
  ConfigFileIO:['配置与运行时数据','UTF-8解析与临时文件替换'],
  ConfigValidation:['配置与运行时数据','定义校验与字段回退'],
  ConfigRecovery:['配置与运行时数据','启动恢复、备份与冲突检测'],
  ConfigWatcher:['配置与运行时数据','后台文件监听与事件合并'],
  RuntimePools:['配置与运行时数据','运行池快照与缓存失效协调'],
  ModAttributes:['战斗结算','实体属性注册与挂载'],
  CombatAttributes:['战斗结算','三属性安全读取'],
  AttributeDamage:['战斗结算','单次属性防御与反击安排'],
  CombatConsequences:['战斗结算','尝试与确认阶段玩法分发'],
  DamageReplay:['战斗结算','延迟已结算伤害队列与续链'],
  InsightRetaliation:['战斗结算','见切反击及同步反伤链保护'],
  LivingCombatMixin:['战斗结算','非玩家伤害作用域与损伤记录桥接'],
  PlayerCombatMixin:['战斗结算','玩家伤害作用域与损伤记录桥接'],
  FluidErosionUnavoidableMixin:['战斗结算','Forge伤害阶段与取消桥接'],
  DamageReplayMixin:['战斗结算','防御阶段与吸收前桥接'],
  DamageReplayStateAccessor:['战斗结算','受伤冷却状态访问桥接'],
  DiaryEquipmentRules:['装备与携带','日记专属佩戴与卸下规则'],
  AccessoryEquipRules:['装备与携带','佩戴前置与唯一性查询'],
  AccessoryRestore:['装备与携带','恢复快照与嵌套作用域'],
  DependentAccessories:['装备与携带','前置失效清理与返还协调'],
  TravailCurios:['装备与携带','日记定位、额外槽位与返还入口'],
  CuriosRestoreMixin:['装备与携带','Curios恢复注入桥接'],
  ItemDelivery:['公共基础','物品拆分、主背包插入与掉落'],
  HomecomingItem:['装备与携带','专用日记卸下流程'],
  CarryingConfig:['装备与携带','携带来源全局配置'],
  BabelCarrying:['装备与携带','巴别塔携带查询适配'],
  JourneyState:['旅程与主题状态','领域格式、身份与状态读取'],
  JourneyOperations:['旅程与主题状态','条件生成与数据操作'],
  JourneyService:['旅程与主题状态','服务端写入与运行副作用协调'],
  LongTravailData:['旅程与主题状态','日记初始化、自然到访与展示读取'],
  JourneyQueries:['旅程与主题状态','服务端探索查询与缓存'],
  JourneyVisits:['旅程与主题状态','服务端延迟结构确认'],
  JourneySpatial:['旅程与主题状态','结构包围盒、群系邻点与空间缓存'],
  WayguideSearch:['探索与路引','分步目标搜索、缓存与区块请求'],
  WayguideGeometry:['探索与路引','螺旋扫描与指向几何'],
  WayguideReturns:['探索与路引','持久化托管与失败返还调度'],
  WayguideEntity:['探索与路引','搜索展示、指引和物品返还生命周期'],
  WayguideItem:['探索与路引','服务端路引使用入口'],
  RequirementPools:['旅程与主题状态','配置与注册表候选池'],
  PlayerJourneyData:['旅程与主题状态','玩家累计探索与揭示存储'],
  TravailJourneyApi:['旅程与主题状态','公开服务端写入入口'],
  TravailStateApi:['旅程与主题状态','公开只读状态入口'],
  JourneySync:['旅程与主题状态','客户端玩家摘要缓存'],
  AspectEssenceItem:['旅程与主题状态','主题精粹使用与反馈'],
  RenewalItem:['旅程与主题状态','整本重置物品入口'],
  WeightedPicker:['公共基础','通用基础'], WeightedEntry:['公共基础','基础数据'],
  WeightedTable:['公共基础','基础数据'], CombatContext:['战斗协调','核心协调（依赖具体玩法）'],
  TravailAspect:['主题定义','领域基础'], AspectTheme:['主题定义','主题映射'],
  TheLongTravail:['注册与装配','启动入口'], PrinceFlightPermission:['小王子飞行','客户端授权状态'],
  LittlePrince:['小王子飞行','服务端事件与玩法协调'],
  LittlePrinceFlight:['小王子飞行','服务端授权状态'], PrinceFlightClient:['小王子飞行','客户端接入'],
  PrinceFlightPacket:['小王子飞行','网络协议'], CarryingApi:['装备与携带','共享查询（含物品特例）']
};
// Lexical candidates, not symbol resolution. Comments and strings are excluded.
const strip = s => s.replace(/\/\*[\s\S]*?\*\/|\/\/[^\r\n]*|"""[\s\S]*?"""|"(?:\\.|[^"\\])*"|'(?:\\.|[^'\\])*'/g,
  m => m.replace(/[^\r\n]/g,' '));
const rows = walk(source).filter(p=>p.endsWith('.java')).map(p=>{
  const bytes=fs.readFileSync(p), text=bytes.toString('utf8'), clean=strip(text);
  const pkg=clean.match(/\bpackage\s+([\w.]+)\s*;/)?.[1];
  const name=path.basename(p,'.java'), area=pkg?.split('.')[2]||'root';
  const rule=overrides[name]||groups[area]||['待归类','待确认'];
  const signals=[];
  if(clean.includes('@SubscribeEvent')) signals.push('Forge事件');
  if(clean.includes('@Mixin')) signals.push('Mixin');
  if(/registerMessage\s*\(/.test(clean)) signals.push('网络注册');
  if(/implements[^\{]*ICurioItem/.test(clean)) signals.push('Curios回调');
  const side=['PrinceFlightPermission','PrinceFlightClient','JourneySync'].includes(name)||area==='client'||pkg?.includes('.client')
    ? '客户端职责' : ['LittlePrince','LittlePrinceFlight','CarryingApi','JourneyService','JourneyQueries','JourneyVisits','TravailJourneyApi','RequirementPools'].includes(name)
    ? '服务端入口' : '待按方法核对';
  return {path:slash(path.relative(root,p)),name,pkg,type:pkg+'.'+name,module:rule[0],role:rule[1],
    classification:overrides[name]?'重点归类':'目录初分，待逐类复核',side,entry:signals.join('、')||'普通调用/待核对',
    sha256:hash(bytes),clean};
});
const byName=new Map();
for(const r of rows) byName.set(r.name,[...(byName.get(r.name)||[]),r]);
const edges=[];
for(const r of rows) {
  const imports=[...r.clean.matchAll(/\bimport\s+(?:static\s+)?([\w.*]+)\s*;/g)].map(m=>m[1]);
  const body=r.clean.replace(/\b(?:import|package)\s+[^;]+;/g,m=>m.replace(/[^\r\n]/g,' '));
  const seen=new Set();
  for(const m of body.matchAll(/\b[A-Z][A-Za-z0-9_$]*\b/g)) {
    for(const target of byName.get(m[0])||[]) {
      if(target===r||seen.has(target.type)) continue;
      const explicit=imports.some(i=>i===target.type||i.startsWith(target.type+'.'));
      const fq=body.includes(target.type);
      const same=target.pkg===r.pkg;
      const wildcard=imports.includes(target.pkg+'.*');
      if(!explicit&&!fq&&!same&&!wildcard) continue;
      seen.add(target.type);
      edges.push({from:r.type,to:target.type,kind:'lexical-reference-candidate',
        evidence:r.path,tokenLine:r.clean.slice(0,m.index).split('\n').length,
        basis:fq?'fully-qualified':explicit?'explicit-import':same?'same-package':'wildcard-import'});
    }
  }
}
edges.sort((a,b)=>(a.from+a.to).localeCompare(b.from+b.to,'en'));
const snapshot=hash(rows.map(r=>r.path+'\0'+r.sha256).join('\n'));
const link=r=>'../../../../'+r.path;
const lines=['# 主源码归属索引','',
  '> 自动生成；修改归类规则后运行 `node docs/开发者查阅/架构/维护/generate.mjs`。', '',
  `范围：src/main/java；${rows.length} 个 Java 文件。指纹：\`${snapshot}\`。`, '',
  '目录初分是检索入口，不代表逐类审计完成。运行端没有证据时明确待核对；“普通调用”不排除资源注册、反射或框架回调。', '',
  '[返回项目书](../README.md) · [引用候选索引](引用候选.md)', '',
  '| 文件 | 模块 | 角色 | 运行端 | 入口信号 | 归类状态 |','|---|---|---|---|---|---|'];
for(const r of rows) lines.push(`| [${r.path.replace('src/main/java/com/thelongtravail/','')}](${link(r)}) | ${r.module} | ${r.role} | ${r.side} | ${r.entry} | ${r.classification} |`);
const dep=['# 正反向引用候选','',
  '> 自动生成的词法候选，不是语义调用图。排除注释和字符串；识别同包、显式导入、通配导入与全限定类名。局部同名标识可能误报，反射、方法引用目标、虚调用及嵌套类型可能漏报。', '',
  '事件、Mixin、网络及数据流必须结合人工章节阅读。完整候选及证据位置见 [JSON](引用候选.json)。不得据此声称完整调用覆盖。', '',
  '[返回项目书](../README.md) · [源码归属](源码归属.md)', '',
  '| 类 | 引用候选 | 被引用候选 |','|---|---|---|'];
for(const r of rows) {
  const out=edges.filter(e=>e.from===r.type).map(e=>e.to.replace('com.thelongtravail.',''));
  const inc=edges.filter(e=>e.to===r.type).map(e=>e.from.replace('com.thelongtravail.',''));
  dep.push(`| [${r.type.replace('com.thelongtravail.','')}](${link(r)}) | ${out.join('、')||'未检出'} | ${inc.join('、')||'未检出'} |`);
}
const inputs = inputSnapshot(root);
const outputs={
  '资源与构建指纹.json':JSON.stringify(inputs,null,2)+'\n',
  '源码归属.md':lines.join('\n')+'\n', '引用候选.md':dep.join('\n')+'\n',
  '引用候选.json':JSON.stringify({schema:1,method:'lexical-candidates-not-call-graph',sourceFingerprint:snapshot,
    files:rows.map(({clean,...r})=>r),edges},null,2)+'\n'
};
if (!check) fs.mkdirSync(path.join(doc,'索引'),{recursive:true});
for(const [name,body] of Object.entries(outputs)) {
  const p=path.join(doc,'索引',name);
  if(check) {if(!fs.existsSync(p)||fs.readFileSync(p,'utf8')!==body) throw Error('Index stale: '+name);}
  else if(!fs.existsSync(p)||fs.readFileSync(p,'utf8')!==body) fs.writeFileSync(p,body);
}
const checkedLinks = validateLinks(doc);
console.log(JSON.stringify({files:rows.length,unclassified:rows.filter(r=>r.module==='待归类').length,
  candidateEdges:edges.length,sourceFingerprint:snapshot,inputFingerprint:inputs.fingerprint,
  inputFiles:inputs.files.length,mode:check?'check':'generate',links:'PASS',localLinks:checkedLinks.links,anchors:checkedLinks.anchors}));
