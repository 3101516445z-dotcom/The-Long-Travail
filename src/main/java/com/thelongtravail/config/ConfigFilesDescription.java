package com.thelongtravail.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class ConfigFilesDescription {
    public static final String GUIDE = guide("zh_cn");
    public static final String GUIDE_EN = guide("en_us");

    static String presetMarker(String modName, String category, String categoryEnglish) {
        return modName
                + " "
                + category
                + "预设处理标记；true 表示停止补入，false 表示等待处理。\n"
                + "Processing marker for "
                + modName
                + " "
                + categoryEnglish
                + " presets; true stops additions, false marks processing as pending.";
    }

    public static void describe(ForgeConfigSpec.Builder builder, String file) {
        switch (file) {
            case "client.toml" -> {
                section(
                        builder,
                        "nameEffects",
                        "The Long Travail / 苦旅 — client.toml\n"
                                + "客户端显示偏好\n"
                                + "Client display preferences\n"
                                + "仅影响本客户端。保存后自动重新加载；重启客户端可确保生效。\n"
                                + "Only affects this client. Reloads automatically after saving;"
                                + " restarting the client ensures changes take effect.\n"
                                + "名称光晕两项独立：HIGH 高、MEDIUM 中、LOW 低、OFF 关闭。\n"
                                + "The two name-halo settings are independent: HIGH, MEDIUM, LOW,"
                                + " OFF.\n"
                                + "降低档位可减少绘制开销；关闭光晕仍保留名称内部动态效果。\n"
                                + "Lower settings reduce rendering cost; disabling halos preserves"
                                + " animation inside the names.\n"
                                + "详细规则见同目录“配置说明.txt”[M05]。\n"
                                + "Details: Configuration Guide.txt [M05].");
                section(
                        builder,
                        "visualDeprivation",
                        "本客户端的视觉剥夺动态效果设置，不改变服务端的触发规则和强度设置。\n"
                                + "Visual Deprivation animation preferences for this client. Does not"
                                + " change server-side trigger rules or intensity settings.");
                section(builder, "messages", "本地技能提示显示。\nLocal skill-message display settings.");
                section(
                        builder,
                        "timeStop",
                        "时停区域边界的本地显示与性能档位，不改变范围和豁免规则。\n"
                                + "Local time-stop boundary display and performance mode. Does not"
                                + " change the area or exemption rules.");
            }
            case "general.toml" -> {
                section(
                        builder,
                        "starter",
                        "The Long Travail / 苦旅 — general.toml\n"
                                + "通用规则\n"
                                + "Shared rules\n"
                                + "探索条件的抽取数量仅影响新生成的旅程。\n"
                                + "Exploration draw counts affect only newly generated journeys.");
                section(
                        builder,
                        "requirements",
                        "六类主题共用探索抽取数量。仅影响新生成的旅程；各自候选池见 aspects/。\n"
                                + "Exploration draw counts shared by all six themes. Only affects newly"
                                + " generated journeys; each theme's candidate pools are under"
                                + " aspects/.");
                section(builder, "malice", "恶意生效的通用条件。\nShared conditions for Malice effects.");
                section(
                        builder,
                        "receivedMalice",
                        "六类主题共用的受击恶意触发规则。\nOn-hit Malice trigger rules shared by all six themes.");
                section(
                        builder,
                        "carrying",
                        "全局携带位置开关，适用于駅、巴别塔和小王子。\n"
                                + "Shared carrying location switches for Eki, Tower of Babel and Little"
                                + " Prince.\n"
                                + "駅和巴别塔还须允许各自的独立位置开关；不会递归检查嵌套容器。\n"
                                + "Eki and Babel also require their per-item switches; nested"
                                + " containers are not searched recursively.\n"
                                + "详见同目录“配置说明.txt”[C03] 携带位置开关。\n"
                                + "See [C03] Carrying locations in Configuration Guide.txt in this"
                                + " directory.");
                section(builder, "journeyDetection", "探索进度检测。\nExploration progress detection.");
                section(
                        builder,
                        "effectLimits",
                        "跨主题的自动效果尝试次数限制。\n"
                                + "Cross-theme limits on automatic effect attempts.\n"
                                + "详细规则见同目录“配置说明.txt”[M02]。\n"
                                + "Details: Configuration Guide.txt [M02].");
                section(
                        builder,
                        "rewardDelivery",
                        "全服额外奖励的发放上限；具体奖励池位于对应主题文件中。\n"
                                + "Server-wide extra-reward delivery limits. Reward pools are in the"
                                + " corresponding theme files.\n"
                                + "详细规则见同目录“配置说明.txt”[M02]。\n"
                                + "Details: Configuration Guide.txt [M02].");
                section(
                        builder,
                        "compatibility",
                        "其他模组兼容行为。\nCompatibility behavior with other mods.");
                section(
                        builder,
                        "loot",
                        "共享战利品：各表独立判定，触发后按权重选一个条目。保存后执行 /reload 或重启。\n"
                                + "Shared loot: tables roll independently, then select one weighted"
                                + " entry on success. Save and use /reload or restart.\n"
                                + "详细规则见同目录“配置说明.txt”[C06]。\n"
                                + "Details: Configuration Guide.txt [C06].");
                section(
                        builder,
                        "loot.fishing_treasure",
                        "详细规则见同目录“配置说明.txt”[C06]。\nDetails: Configuration Guide.txt [C06].");
                section(
                        builder,
                        "loot.tables",
                        "按完整战利品表 ID 分组，可新增原版或模组表；表 ID 须用双引号包围。\n"
                                + "Group by quoted full loot-table IDs; vanilla and modded tables may"
                                + " be added.\n"
                                + "chance 为总概率（0～1）；items 格式为 物品ID|正整数权重|最小数量|最大数量。\n"
                                + "chance is the total probability (0–1); items use"
                                + " item_id|positive_integer_weight|min_count|max_count.\n"
                                + "每次最多选中一个条目；数量范围 1～2304；概率为 0 或列表为空时停止本表奖励。\n"
                                + "Select at most one entry per roll; counts range from 1 to 2304. Zero"
                                + " chance or an empty list disables this table.");
                section(
                        builder,
                        "upgrades",
                        "内部配置升级记录，由模组维护，请勿修改。\n"
                                + "Internal configuration upgrade records maintained by the mod. Do not"
                                + " edit.");
            }
            case "utility_items.toml" -> {
                section(
                        builder,
                        "items",
                        "The Long Travail / 苦旅 — utility_items.toml\n"
                                + "通用功能物品\n"
                                + "General-purpose utility items");
                section(
                        builder,
                        "items.wayguide",
                        "路引：查找当前苦旅尚未完成的探索条件。\n"
                                + "Wayguide: locate unfinished exploration requirements for the current"
                                + " Long Travail.\n"
                                + "详细规则见同目录“配置说明.txt”[M04]。\n"
                                + "Details: Configuration Guide.txt [M04].");
                section(
                        builder,
                        "items.wayguide.search",
                        "搜索范围与实际时间上限。\nSearch range and real-time timeout.");
                section(builder, "items.wayguide.sampling", "群系采样精度。\nBiome sampling precision.");
                section(
                        builder,
                        "items.wayguide.budget",
                        "全服每游戏刻及单次搜索的次数和耗时限制。\n"
                                + "Server-wide per-game-tick and per-search limits on operations and"
                                + " elapsed time.");
                section(
                        builder,
                        "items.wayguide.cache",
                        "搜索结果的缓存设置。\nSearch-result cache settings.");
            }
            case "aspects/abyss.toml" -> {
                section(
                        builder,
                        "travail",
                        "The Long Travail / 苦旅 — aspects/abyss.toml\n"
                                + "归墟主题\n"
                                + "Abyss theme\n"
                                + "探索条件池仅影响新生成的旅程。\n"
                                + "Exploration requirement pools affect only newly generated"
                                + " journeys.\n"
                                + "技能时长和冷却的修改仅影响之后释放的技能。\n"
                                + "Skill duration and cooldown changes affect only future casts.");
                section(
                        builder,
                        "travail.requirements",
                        "苦旅探索条件池。仅影响新生成的旅程；统一抽取数量见 general.toml 的 requirements。\n"
                                + "Long Travail exploration requirement pools. Only affects newly"
                                + " generated journeys; shared draw counts are under requirements in"
                                + " general.toml.\n"
                                + "上一级“配置说明.txt”：填写格式 [E01]；@ 名称 [E02]；抽取与无效条目 [E03]。\n"
                                + "See ../Configuration Guide.txt: entry format [E01], @ names [E02],"
                                + " selection and invalid entries [E03].");
                section(
                        builder,
                        "travail.malice",
                        "苦旅：恶意状态的惩罚与触发规则。\n"
                                + "The Long Travail: penalties and trigger rules while Malice is"
                                + " active.");
                section(
                        builder,
                        "travail.witness",
                        "苦旅：完成探索后获得的见证能力。\n"
                                + "The Long Travail: Witness abilities gained after completing"
                                + " exploration.");
                section(
                        builder,
                        "items",
                        "归墟主题关联物品；按物品ID分别管理。\n"
                                + "Items associated with Abyss, configured separately by item ID.");
                section(
                        builder,
                        "items.eki",
                        "駅：携带駅并佩戴苦旅时生效，与归墟恶意/见证联动。\n"
                                + "Eki: active when carried while wearing The Long Travail; interacts"
                                + " with Abyss Malice and Witness.");
                section(
                        builder,
                        "items.eki.carrying",
                        "駅的独立携带位置开关；与 general.toml / carrying 同位置的开关同时启用才生效。\n"
                                + "Eki per-item carrying switches; the matching general.toml / carrying"
                                + " switch must also be enabled.\n"
                                + "详见上一级“配置说明.txt”[C03] 携带位置开关。\n"
                                + "See [C03] Carrying locations in ../Configuration Guide.txt.\n"
                                + "仅适用于玩家，多个携带位置不叠加；女仆只检查原生饰品栏。\n"
                                + "Players only; multiple carried copies do not stack. Maids use native"
                                + " bauble slots only.");
                section(
                        builder,
                        "items.tokaido",
                        "东海道五十三次：玩家需装备在指定的 Curios 槽位，无需佩戴苦旅。\n"
                                + "女仆需放入原生饰品栏，仅提供生命值恢复与火焰免疫。\n"
                                + "Tokaido: players use the specified Curios slots without requiring"
                                + " The Long Travail.\n"
                                + "Maids use native bauble slots and receive only healing and fire"
                                + " immunity.");
                section(
                        builder,
                        "items.tokaido.skill",
                        "主动唤雨；时长与冷却修改只影响之后释放的技能。\n"
                                + "Active Call Rain skill. Duration and cooldown changes affect only"
                                + " future casts.");
                section(
                        builder,
                        "items.perfect_fluid",
                        "完美流体：用于复制物品。检测到车万女仆及其性格模组时，分别将初始魂符及已收纳女仆的魂符、怪兽战斗仪加入黑名单一次（不包含空魂符）；之后可自由调整。初始化记录随本配置保存。\n"
                                + "Perfect Fluid: duplicates items. Initial and occupied Soul Spells"
                                + " (excluding empty Soul Spells) and Monster Battle Device are added"
                                + " to the blacklist once when their respective maid mods are detected."
                                + " The blacklist remains editable; initialization records belong to"
                                + " this configuration.");
                section(
                        builder,
                        "items.liquefied_abundance",
                        "液化丰饶：随机药水转换；长按所需时间、默认持续时间与效果白名单、黑名单。\n"
                                + "Liquefied Abundance: random potion conversion, hold threshold,"
                                + " fallback duration, effect whitelist and blacklist.");
                section(
                        builder,
                        "items.midnight_lafite",
                        "午夜拉菲：手饰，基础效果独立；夜间伤害与免疫依赖归墟状态。\n"
                                + "Midnight Lafite: hands accessory with independent base effects;"
                                + " nighttime damage and immunity depend on Abyss state.");
            }
            case "aspects/boundless.toml" -> {
                section(
                        builder,
                        "travail",
                        "The Long Travail / 苦旅 — aspects/boundless.toml\n"
                                + "无垠主题\n"
                                + "Boundless theme\n"
                                + "探索条件池仅影响新生成的旅程。\n"
                                + "Exploration requirement pools affect only newly generated"
                                + " journeys.");
                section(
                        builder,
                        "travail.requirements",
                        "苦旅探索条件池。仅影响新生成的旅程；统一抽取数量见 general.toml 的 requirements。\n"
                                + "Long Travail exploration requirement pools. Only affects newly"
                                + " generated journeys; shared draw counts are under requirements in"
                                + " general.toml.\n"
                                + "上一级“配置说明.txt”：填写格式 [E01]；@ 名称 [E02]；抽取与无效条目 [E03]。\n"
                                + "See ../Configuration Guide.txt: entry format [E01], @ names [E02],"
                                + " selection and invalid entries [E03].");
                section(
                        builder,
                        "travail.malice",
                        "苦旅：恶意状态的惩罚与触发规则。\n"
                                + "The Long Travail: penalties and trigger rules while Malice is"
                                + " active.");
                section(
                        builder,
                        "travail.witness",
                        "苦旅：完成探索后获得的见证能力。\n"
                                + "The Long Travail: Witness abilities gained after completing"
                                + " exploration.");
                section(
                        builder,
                        "travail.witness.phantom",
                        "无垠见证：幻翼检测、处死限制及奖励池；全服发放上限见 general.toml。\n"
                                + "Boundless Witness: phantom detection, kill limits, and reward pools."
                                + " Server-wide delivery limits are in general.toml.");
            }
            case "aspects/deep_valley.toml" -> {
                section(
                        builder,
                        "travail",
                        "The Long Travail / 苦旅 — aspects/deep_valley.toml\n"
                                + "幽谷主题\n"
                                + "Deep Valley theme\n"
                                + "探索条件池仅影响新生成的旅程。\n"
                                + "Exploration requirement pools affect only newly generated"
                                + " journeys.");
                section(
                        builder,
                        "travail.requirements",
                        "苦旅探索条件池。仅影响新生成的旅程；统一抽取数量见 general.toml 的 requirements。\n"
                                + "Long Travail exploration requirement pools. Only affects newly"
                                + " generated journeys; shared draw counts are under requirements in"
                                + " general.toml.\n"
                                + "上一级“配置说明.txt”：填写格式 [E01]；@ 名称 [E02]；抽取与无效条目 [E03]。\n"
                                + "See ../Configuration Guide.txt: entry format [E01], @ names [E02],"
                                + " selection and invalid entries [E03].");
                section(
                        builder,
                        "travail.malice",
                        "苦旅：恶意状态的惩罚与触发规则。\n"
                                + "The Long Travail: penalties and trigger rules while Malice is"
                                + " active.");
                section(
                        builder,
                        "travail.witness",
                        "苦旅：完成探索后获得的见证能力。\n"
                                + "The Long Travail: Witness abilities gained after completing"
                                + " exploration.");
                section(
                        builder,
                        "travail.visualDeprivation",
                        "视觉剥夺：持续时间、过渡与画面样式；触发概率见 travail.malice。\n"
                                + "Visual Deprivation: duration, transitions, and appearance. Trigger"
                                + " chance is under travail.malice.");
                section(builder, "items", "幽谷主题关联物品。\nItems associated with Deep Valley.");
                section(
                        builder,
                        "items.high_frequency_tremor",
                        "高频战栗：见证状态潜行右键破坏黑名单；同样作用于 FTB 连锁。\n"
                                + "High Frequency Tremor: Witness sneak-use block blacklist, also applied to FTB Ultimine selections.");
                section(
                        builder,
                        "items.azrael",
                        "死告天使：玩家需同时佩戴苦旅。同类只生效一件。\n"
                                + "见证状态下，每次实际造成伤害只判定一次。处决目标与处决佩戴者不会同时发生。\n"
                                + "Azrael: players must also wear The Long Travail. Only one copy takes"
                                + " effect.\n"
                                + "With Witness, each instance of actual damage dealt makes one roll;"
                                + " executing the target and executing the wearer cannot occur on the"
                                + " same roll.\n"
                                + "详细规则见上一级“配置说明.txt”[M03]。\n"
                                + "Details: ../Configuration Guide.txt [M03].");
                section(
                        builder,
                        "items.phosphophyllite",
                        "磷叶石：需同时佩戴苦旅的通用饰品，支持女仆。按比例增加各项属性，受击触发方式与苦旅一致；有效溢出治疗转化为伤害吸收量。\n"
                                + "Phosphophyllite: a general-purpose curio that requires The Long"
                                + " Travail and supports maids. Applies multiplicative attribute"
                                + " bonuses, follows the diary's on-hit trigger mode, and converts"
                                + " effective overhealing into absorption.");
                section(
                        builder,
                        "items.underground_velvet",
                        "地下丝绒：右键消耗1个，基础奖励8个同种矿石或锭；恶意抽取箱子条目，见证增加15级。\n"
                                + "Underground Velvet: consume one for eight matching ores or ingots;"
                                + " Malice samples chest entries, Witness grants 15 levels.");
            }
            case "aspects/far_reach.toml" -> {
                section(
                        builder,
                        "travail",
                        "The Long Travail / 苦旅 — aspects/far_reach.toml\n"
                                + "穷遐主题\n"
                                + "Far Reach theme\n"
                                + "探索条件池仅影响新生成的旅程。\n"
                                + "Exploration requirement pools affect only newly generated"
                                + " journeys.");
                section(
                        builder,
                        "travail.requirements",
                        "苦旅探索条件池。仅影响新生成的旅程；统一抽取数量见 general.toml 的 requirements。\n"
                                + "Long Travail exploration requirement pools. Only affects newly"
                                + " generated journeys; shared draw counts are under requirements in"
                                + " general.toml.\n"
                                + "上一级“配置说明.txt”：填写格式 [E01]；@ 名称 [E02]；抽取与无效条目 [E03]。\n"
                                + "See ../Configuration Guide.txt: entry format [E01], @ names [E02],"
                                + " selection and invalid entries [E03].");
                section(
                        builder,
                        "travail.malice",
                        "苦旅：恶意状态的惩罚与触发规则。\n"
                                + "The Long Travail: penalties and trigger rules while Malice is"
                                + " active.");
                section(
                        builder,
                        "travail.witness",
                        "苦旅：完成探索后获得的见证能力。\n"
                                + "The Long Travail: Witness abilities gained after completing"
                                + " exploration.");
                section(
                        builder,
                        "items",
                        "穷遐主题关联饰品：基础效果独立，恶意和见证分支需要佩戴苦旅。\n"
                                + "Accessories associated with Far Reach. Base effects work"
                                + " independently; Malice and Witness effects require The Long"
                                + " Travail.");
                section(
                        builder,
                        "items.babel.carrying",
                        "巴别塔的独立携带位置开关；与 general.toml / carrying 同位置的开关同时启用才生效。\n"
                                + "Tower of Babel per-item carrying switches; the matching general.toml"
                                + " / carrying switch must also be enabled.\n"
                                + "详见上一级“配置说明.txt”[C03] 携带位置开关。\n"
                                + "See [C03] Carrying locations in ../Configuration Guide.txt.");
                section(
                        builder,
                        "items.golden_age",
                        "黄金时代：护符。空手掏取不消耗方块；投入金块后按穷遐状态转化。\n"
                                + "Golden Age: charm. Empty-handed digging does not consume blocks;"
                                + " thrown gold blocks transform according to the Far Reach state.");
                section(
                        builder,
                        "items.icarus",
                        "伊卡洛斯：背饰。鞘翅飞行、命中点燃、基础免疫、按世界时间计算的太阳增伤、见证魔法免疫。\n"
                                + "Icarus: back accessory. Elytra flight, ignition on hit, base"
                                + " immunities, sunlight damage bonuses based on world time, and magic"
                                + " immunity with Witness.");
            }
            case "aspects/flourishing.toml" -> {
                section(
                        builder,
                        "items.immortal_bud",
                        "永生芽：赋予时的修复开关与目标黑名单；仅影响新的赋予。\n"
                                + "Immortal Bud: repair-on-application switch and target blacklist;"
                                + " affects new applications only.");
                section(
                        builder,
                        "travail",
                        "The Long Travail / 苦旅 — aspects/flourishing.toml\n"
                                + "繁茂主题\n"
                                + "Flourishing theme\n"
                                + "探索条件池仅影响新生成的旅程。\n"
                                + "Exploration requirement pools affect only newly generated"
                                + " journeys.");
                section(
                        builder,
                        "travail.requirements",
                        "苦旅探索条件池。仅影响新生成的旅程；统一抽取数量见 general.toml 的 requirements。\n"
                                + "Long Travail exploration requirement pools. Only affects newly"
                                + " generated journeys; shared draw counts are under requirements in"
                                + " general.toml.\n"
                                + "上一级“配置说明.txt”：填写格式 [E01]；@ 名称 [E02]；抽取与无效条目 [E03]。\n"
                                + "See ../Configuration Guide.txt: entry format [E01], @ names [E02],"
                                + " selection and invalid entries [E03].");
                section(
                        builder,
                        "travail.malice",
                        "苦旅：恶意状态的惩罚与触发规则。\n"
                                + "The Long Travail: penalties and trigger rules while Malice is"
                                + " active.");
                section(
                        builder,
                        "travail.witness",
                        "苦旅：完成探索后获得的见证能力。\n"
                                + "The Long Travail: Witness abilities gained after completing"
                                + " exploration.");
                section(builder, "items", "繁茂主题关联饰品。\nAccessories associated with Flourishing.");
                section(
                        builder,
                        "items.spring_corner",
                        "春日一隅：一朵永久主花、两朵可清洗配花。\n"
                                + "A Corner of Spring: one permanent primary flower and two secondary"
                                + " flowers that can be washed off.");
                section(
                        builder,
                        "items.spring_corner.flowers",
                        "各花朵独立设置。禁用后不能用于制作或添加；已有花朵保留，但对应效果暂停。\n"
                                + "Configure flowers independently. Disabled flowers cannot be used in"
                                + " crafting or added; existing flowers remain but their effects are"
                                + " inactive.");
                section(
                        builder,
                        "items.affection",
                        "喜欢的心情：伙伴绑定与生命值相关加成。\nAffection: binding and health-based bonuses.");
            }
            case "aspects/underworld.toml" -> {
                section(
                        builder,
                        "travail",
                        "The Long Travail / 苦旅 — aspects/underworld.toml\n"
                                + "冥府主题\n"
                                + "Underworld theme\n"
                                + "探索条件池仅影响新生成的旅程。\n"
                                + "Exploration requirement pools affect only newly generated"
                                + " journeys.");
                section(
                        builder,
                        "travail.requirements",
                        "苦旅探索条件池。仅影响新生成的旅程；统一抽取数量见 general.toml 的 requirements。\n"
                                + "Long Travail exploration requirement pools. Only affects newly"
                                + " generated journeys; shared draw counts are under requirements in"
                                + " general.toml.\n"
                                + "上一级“配置说明.txt”：填写格式 [E01]；@ 名称 [E02]；抽取与无效条目 [E03]。\n"
                                + "See ../Configuration Guide.txt: entry format [E01], @ names [E02],"
                                + " selection and invalid entries [E03].");
                section(
                        builder,
                        "travail.malice",
                        "苦旅：恶意状态的惩罚与触发规则。\n"
                                + "The Long Travail: penalties and trigger rules while Malice is"
                                + " active.");
                section(
                        builder,
                        "travail.witness",
                        "苦旅：完成探索后获得的见证能力。\n"
                                + "The Long Travail: Witness abilities gained after completing"
                                + " exploration.");
                section(
                        builder,
                        "travail.witness.fishing",
                        "冥府见证钓鱼：概率、物品池与实体池。\n"
                                + "Underworld Witness fishing: probabilities, item pools, and entity"
                                + " pools.");
                section(
                        builder,
                        "items.relics.king",
                        "第六天魔王；下列加成设为 0 时禁用对应加成。\n"
                                + "Demon King of the Sixth Heaven; 0 disables the corresponding"
                                + " bonus.");
                section(
                        builder,
                        "items.relics.banner",
                        "万魂幡；有效魂数不超过 capacity。下列加成设为 0 时禁用对应加成。\n"
                                + "Myriad Souls Banner; effective souls are capped by capacity. A bonus"
                                + " of 0 disables that bonus.");
            }
            default -> throw new IllegalArgumentException("Unknown config file: " + file);
        }
    }

    private static void section(ForgeConfigSpec.Builder builder, String path, String comment) {
        if (comment.startsWith("The Long Travail /")) {
            String directory = path.equals("travail") ? "上一级" : "同目录";
            String relative = path.equals("travail") ? "../" : "";
            comment +=
                    "\n填写方法见"
                            + directory
                            + "“配置说明.txt”：基础写法与单位 [C04]；列表格式速查 [C05]。\nEditing help: "
                            + relative
                            + "Configuration Guide.txt [C04] syntax and units, [C05] list formats.";
        }
        builder.comment(comment).push(path).pop(path.split("\\.").length);
    }

    static String carrying(String area) {
        return switch (area) {
            case "inventory" -> "启用主背包与快捷栏检测。\nEnable main inventory and hotbar checks.";
            case "offhand" -> "启用副手检测。\nEnable offhand checks.";
            case "armor" ->
                    "启用四个原版盔甲槽检测；不会赋予物品装备权限。\n"
                            + "Enable checks in the four vanilla armor slots; does not grant equipment"
                            + " permission.";
            case "enderChest" -> "启用末影箱检测。\nEnable ender chest checks.";
            case "curios" ->
                    "启用 Curios 装备槽检测，不包括外观槽。\n"
                            + "Enable equipped Curios slot checks, excluding cosmetic slots.";
            case "externalInventoryTick" ->
                    "启用外部容器的模拟物品栏更新检测，须容器支持并启用。\n"
                            + "Enable simulated inventory updates from external containers; requires"
                            + " container support and activation.";
            default -> throw new IllegalArgumentException(area);
        };
    }

    private static String guide(String language) {
        String resource =
                "/assets/the_long_travail/config/configuration-guide." + language + ".txt";
        try (var input = ConfigFilesDescription.class.getResourceAsStream(resource)) {
            if (input == null)
                throw new IllegalStateException("Missing configuration guide: " + resource);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new java.io.UncheckedIOException("Cannot read " + resource, failure);
        }
    }

    private ConfigFilesDescription() {}
}
