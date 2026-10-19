package com.thelongtravail.config;

public final class ConfigFilesDescription {
    public static void describe(net.minecraftforge.common.ForgeConfigSpec.Builder builder, String file) {
        if (file.startsWith("aspects/")) {
            section(builder, "travail", header(file));
            section(
                    builder,
                    "travail.malice",
                    "苦旅：恶意状态的惩罚与触发规则。\nThe Long Travail: penalties and trigger rules while Malice is active.");
            section(
                    builder,
                    "travail.witness",
                    "苦旅：完成探索后获得的见证能力。\nThe Long Travail: Witness abilities gained after completing exploration.");
            section(
                    builder,
                    "travail.requirements",
                    "苦旅探索条件池。仅影响新生成的旅程；统一抽取数量见 general.toml 的 requirements。\nLong Travail exploration requirement pools. Only affects newly generated journeys; shared draw counts are under requirements in general.toml.");
            if (file.endsWith("abyss.toml")) {
                section(
                        builder,
                        "items.perfect_fluid",
                        "完美流体：用于复制物品，默认不设黑名单。\nPerfect Fluid: duplicates items. The blacklist is empty by default.");
                section(
                        builder,
                        "items.midnight_lafite",
                        "午夜拉菲：手饰，基础效果独立；夜间伤害与免疫依赖归墟状态。\nMidnight Lafite: hands accessory with independent base effects; nighttime damage and immunity depend on Abyss state.");
                section(
                        builder,
                        "items",
                        "归墟主题关联物品；按物品ID分别管理。\nItems associated with Abyss, configured separately by item ID.");
                section(
                        builder,
                        "items.eki",
                        "駅：携带駅并佩戴苦旅时生效，与归墟恶意/见证联动。\nEki: active when carried while wearing The Long Travail; interacts with Abyss Malice and Witness.");
                section(
                        builder,
                        "items.eki.carrying",
                        "以下开关为駅的旧版兼容限制，与 general.toml 的 carrying 统一位置开关同时生效。仅适用于玩家，多个位置不叠加。\n女仆只检查原生饰品栏，不受这些携带位置开关影响。\nLegacy Eki restrictions apply together with the shared carrying switches in general.toml; both must allow the location. These restrictions apply only to players, and carrying multiple copies does not stack their effects.\nMaids are checked only for accessories in their native bauble slots and are not affected by these location switches.");
                section(
                        builder,
                        "items.tokaido",
                        "东海道五十三次：玩家需装备在指定的 Curios 槽位，无需佩戴苦旅。\n女仆需放入原生饰品栏，仅提供生命值恢复与火焰免疫。\nTokaido: players use the specified Curios slots without requiring The Long Travail.\nMaids use native bauble slots and receive only healing and fire immunity.");
                section(
                        builder,
                        "items.tokaido.skill",
                        "主动唤雨；时长与冷却修改只影响之后释放的技能。\nActive Call Rain skill. Duration and cooldown changes affect only future casts.");
            } else if (file.endsWith("far_reach.toml")) {
                section(
                        builder,
                        "items",
                        "穷遐主题关联饰品：基础效果独立，恶意和见证分支需要佩戴苦旅。\nAccessories associated with Far Reach. Base effects work independently; Malice and Witness effects require The Long Travail.");
                section(
                        builder,
                        "items.golden_age",
                        "黄金时代：护符。空手掏取不消耗方块；投入金块后按穷遐状态转化。\nGolden Age: charm. Empty-handed digging does not consume blocks; thrown gold blocks transform according to the Far Reach state.");
                section(
                        builder,
                        "items.icarus",
                        "伊卡洛斯：背饰。鞘翅飞行、命中点燃、基础免疫、按世界时间计算的太阳增伤、见证魔法免疫。\nIcarus: back accessory. Elytra flight, ignition on hit, base immunities, sunlight damage bonuses based on world time, and magic immunity with Witness.");
            } else if (file.endsWith("deep_valley.toml")) {
                section(builder, "items", "幽谷主题关联物品。\nItems associated with Deep Valley.");
                section(
                        builder,
                        "items.phosphophyllite",
                        "磷叶石：需同时佩戴苦旅的通用饰品，支持女仆。按比例增加各项属性，受击触发方式与苦旅一致；有效溢出治疗转化为伤害吸收量。\nPhosphophyllite: a general-purpose curio that requires The Long Travail and supports maids. Applies multiplicative attribute bonuses, follows the diary's on-hit trigger mode, and converts effective overhealing into absorption.");
                section(
                        builder,
                        "items.underground_velvet",
                        "地下丝绒：右键消耗1个，基础奖励8个同种矿石或锭；恶意抽取箱子条目，见证增加15级。\nUnderground Velvet: consume one for eight matching ores or ingots; Malice samples chest entries, Witness grants 15 levels.");
                section(
                        builder,
                        "items.azrael",
                        "死告天使：玩家需同时佩戴苦旅。同类只生效一件。\n见证状态下，每次实际造成伤害只判定一次。处决目标与处决佩戴者不会同时发生。\nAzrael: a head accessory that requires The Long Travail. Only one copy takes effect. With Witness, each instance of actual damage dealt makes one roll; executing the target and executing the wearer cannot occur on the same roll.");
                section(
                        builder,
                        "travail.visualDeprivation",
                        "视觉剥夺：持续时间、过渡与画面样式；触发概率见 travail.malice。\nVisual Deprivation: duration, transitions, and appearance. Trigger chance is under travail.malice.");
            } else if (file.endsWith("flourishing.toml")) {
                section(builder, "items", "繁茂主题关联饰品。\nAccessories associated with Flourishing.");
                section(
                        builder,
                        "items.spring_corner",
                        "春日一隅：一朵永久主花、两朵可清洗配花。\nA Corner of Spring: one permanent primary flower and two secondary flowers that can be washed off.");
                section(
                        builder,
                        "items.spring_corner.flowers",
                        "按花朵分别调整适配和数值。禁用适配不会删除旧物品中的花朵。\nConfigure support and values separately for each flower. Disabling support does not remove flowers from existing items.");
                section(
                        builder,
                        "items.affection",
                        "喜欢的心情：伙伴绑定与生命值相关加成。\nAffection: binding and health-based bonuses.");
            } else if (file.endsWith("underworld.toml"))
                section(
                        builder,
                        "travail.witness.fishing",
                        "冥府见证钓鱼：概率、物品池与实体池。\nUnderworld Witness fishing: probabilities, item pools, and entity pools.");
            else if (file.endsWith("boundless.toml"))
                section(
                        builder,
                        "travail.witness.phantom",
                        "无垠见证：幻翼检测、处死限制及奖励池；全服发放上限见 general.toml。\nBoundless Witness: phantom detection, kill limits, and reward pools. Server-wide delivery limits are in general.toml.");
        } else if (file.equals("general.toml")) {
            section(builder, "starter", header(file));
            section(
                    builder,
                    "loot",
                    "苦旅统一战利品：每张表一次抽取，按总概率与物品权重分配。\n修改后/reload或重启生效。\n支持新增完整表ID。\n父子表独立抽取。\nUnified Long Travail loot: one roll per table, using the overall chance and item weights. Changes apply after /reload or a restart. Additional full table IDs are supported. Parent and child tables roll independently.");
            section(
                    builder,
                    "requirements",
                    "六类主题共用探索抽取数量。仅影响新生成的旅程；各自候选池见 aspects/。\nExploration draw counts shared by all six themes. Only affects newly generated journeys; each theme's candidate pools are under aspects/.");
            section(
                    builder,
                    "receivedMalice",
                    "六类主题共用的受击恶意触发规则。\nOn-hit Malice trigger rules shared by all six themes.");
            section(builder, "effectLimits", "跨主题的自动效果尝试次数限制。\nCross-theme limits on automatic effect attempts.");
            section(
                    builder,
                    "rewardDelivery",
                    "全服额外奖励的发放上限；具体奖励池位于对应主题文件中。\nServer-wide extra-reward delivery limits. Reward pools are in the corresponding theme files.");
            section(builder, "compatibility", "其他模组兼容行为。\nCompatibility behavior with other mods.");
        } else if (file.equals("utility_items.toml")) {
            section(builder, "items", header(file));
            section(
                    builder,
                    "items.wayguide",
                    "路引：查找当前苦旅尚未完成的探索条件。\nWayguide: locate unfinished exploration requirements for the current Long Travail.");
            section(builder, "items.wayguide.search", "搜索范围与实际时间上限。\nSearch range and real-time timeout.");
            section(builder, "items.wayguide.sampling", "群系采样精度。\nBiome sampling precision.");
            section(
                    builder,
                    "items.wayguide.budget",
                    "全服每游戏刻及单次搜索的次数和耗时限制。\nServer-wide per-game-tick and per-search limits on operations and elapsed time.");
            section(builder, "items.wayguide.cache", "搜索结果的缓存设置。\nSearch-result cache settings.");
        } else if (file.equals("client.toml")) {
            section(builder, "nameEffects", header(file));
            section(
                    builder,
                    "visualDeprivation",
                    "本客户端的视觉剥夺动态效果设置，不改变服务端的触发规则和强度设置。\nVisual Deprivation animation preferences for this client. Does not change server-side trigger rules or intensity settings.");
            section(builder, "messages", "本地技能提示显示。\nLocal skill-message display settings.");
            section(
                    builder,
                    "timeStop",
                    "时停区域边界的本地显示与性能档位，不改变范围和豁免规则。\nLocal time-stop boundary display and performance mode. Does not change the area or exemption rules.");
        }
    }

    private static void section(
            net.minecraftforge.common.ForgeConfigSpec.Builder builder, String path, String comment) {
        builder.comment(comment).push(path).pop(path.split("\\.").length);
    }

    public static String header(String file) {
        String purpose =
                switch (file) {
                    case "general.toml" -> "通用规则\nShared rules";
                    case "utility_items.toml" -> "通用功能物品\nGeneral-purpose utility items";
                    case "client.toml" -> "客户端显示偏好\nClient display preferences";
                    case "aspects/flourishing.toml" -> "繁茂主题\nFlourishing theme";
                    case "aspects/abyss.toml" -> "归墟主题\nAbyss theme";
                    case "aspects/far_reach.toml" -> "穷遐主题\nFar Reach theme";
                    case "aspects/deep_valley.toml" -> "幽谷主题\nDeep Valley theme";
                    case "aspects/underworld.toml" -> "冥府主题\nUnderworld theme";
                    case "aspects/boundless.toml" -> "无垠主题\nBoundless theme";
                    default -> throw new IllegalArgumentException("Unknown config file: " + file);
                };
        String scope =
                file.equals("client.toml")
                        ? "仅影响本客户端。保存后自动重新加载；重启客户端可确保生效。\nOnly affects this client. Reloads automatically after saving; restarting the client ensures changes take effect."
                        : "";
        String notes = "";
        if (file.equals("general.toml"))
            notes += "\n探索条件的抽取数量仅影响新生成的旅程。\nExploration draw counts affect only newly generated journeys.";
        if (file.startsWith("aspects/"))
            notes += "\n探索条件池仅影响新生成的旅程。\nExploration requirement pools affect only newly generated journeys.";
        if (file.equals("aspects/abyss.toml"))
            notes += "\n技能时长和冷却的修改仅影响之后释放的技能。\nSkill duration and cooldown changes affect only future casts.";
        return "The Long Travail / 苦旅 — " + file + "\n" + purpose + (scope.isEmpty() ? "" : "\n" + scope) + notes;
    }

    public static final String GUIDE =
            """
            苦旅配置说明
            本文件由模组在启动时自动生成和更新，请勿在这里保存个人备注。
            实际配置位于同目录的 TOML 文件；旧版根目录配置不再读取。

            文件分组
            general.toml：跨主题公共规则、探索抽取数量、统一战利品及 carrying 携带位置开关。
            駅、巴别塔与小王子共用携带服务；駅和巴别塔的旧位置开关作为兼容限制，与全局开关同时生效。
            utility_items.toml：路引等通用功能物品。
            client.toml：仅影响本客户端的显示偏好。
            aspects/：六个主题及其关联物品。各主题保留独立的探索条件池。
            修改一般配置后会自动重新加载；统一战利品还需执行 /reload 或重启。
            探索池和抽取数量的修改只影响新生成的旅程，不改写已有苦旅。

            探索条件池
            群系格式：群系ID|显示名称|权重
            结构格式：结构ID|显示名称|权重
            权重必须为正整数。权重越大，抽中概率越高。
            每个条目单独占一行，放在方括号内，并用英文双引号包围。
            条目之间用英文逗号分隔，例如：
            biomes = [
                "minecraft:plains|高峰出云|3",
                "minecraft:forest|林间漫步|1"
            ]
            显示名称可以填写自定义文字或谜语，不会改变实际目标。
            上例中的“高峰出云”仍然要求探索平原，只改变苦旅页面上的名称。
            群系和结构的 ID 必须在当前整合包中实际存在。
            结构示例：example:stronghold|末路之门|3
            example:stronghold 仅为格式示例，使用时须替换成实际结构 ID。

            多语言名称
            显示名称以 @ 开头时，后面填写翻译键。例如：
            minecraft:plains|@biome.minecraft.plains|3
            example:stronghold|@structure.example.stronghold|3
            翻译键须在模组或资源包的语言文件中提供对应译文。
            找不到译文时显示目标 ID。普通文字不会随游戏语言切换。

            抽取与无效条目
            抽取数量位于 general.toml 的 requirements 分组。
            空列表或抽取数量为 0 时，禁用对应类别。
            相同目标 ID 只保留第一个有效条目，不通过重复填写增加权重。
            有效目标不足时抽取全部有效目标；无效或未注册条目会被跳过并记录日志。
            非空池中没有有效目标时，会阻止新旅程生成，须先修正该池。

            女仆兼容
            仅已接入的饰品支持女仆，且只能放入女仆原生饰品栏。
            女仆普通背包、主副手和 Curios 槽位均不作为生效位置。
            需要苦旅状态的效果读取在线主人所佩戴的苦旅，不消耗见证成果。
            主人未在线或未佩戴苦旅时，需要苦旅状态的效果不生效。
            一位主人的苦旅可以支持多名女仆；每名女仆仍须装备对应饰品。
            駅的 carrying 开关与饰品的 Curios 槽位设置仅适用于玩家。
            女仆的东海道五十三次仅提供生命值恢复和火焰免疫。
            """;
    public static final String GUIDE_EN =
            """
            The Long Travail Configuration Guide
            Generated and updated automatically at startup. Do not store personal notes here.
            Edit the TOML files in this directory. Legacy root-level files are not loaded.

            Files
            general.toml: shared rules, exploration draw counts, and unified loot.
            utility_items.toml: general-purpose items such as the Wayguide.
            client.toml: local display preferences only.
            aspects/: six themes, their associated items, and separate exploration pools.
            Most settings reload after saving; unified loot also needs /reload or a restart.
            Pool and draw-count changes affect new journeys only.

            Exploration pools
            Format: biome_id|display_name|weight or structure_id|display_name|weight.
            Use a positive integer weight. Larger weights increase selection probability.
            Put each quoted entry on its own line inside brackets, separated by commas:
            biomes = [
                "minecraft:plains|Peaks Above the Clouds|3",
                "minecraft:forest|A Walk in the Woods|1"
            ]
            A custom name or riddle changes only the diary label, not the actual target.
            IDs must exist in the current pack. example:stronghold is only a placeholder.

            Localized names
            Prefix a translation key with @, for example:
            minecraft:plains|@biome.minecraft.plains|3
            example:stronghold|@structure.example.stronghold|3
            Provide translations in a mod or resource pack. Missing translations show the ID.

            Selection and invalid entries
            Draw counts are under requirements in general.toml.
            An empty pool or a zero count disables that category.
            Duplicate IDs keep the first valid entry. Use weights instead of duplicate entries.
            Invalid or unavailable targets are logged and skipped.
            If too few valid targets remain, all remaining targets are selected.
            A nonempty enabled pool with no valid targets blocks new journey generation.

            Maid compatibility
            Supported accessories work only in native maid bauble slots.
            Maid inventories, hands, and Curios slots do not qualify.
            Diary-dependent effects read the diary worn by the online owner without consuming Witness progress.
            Without an online owner wearing the diary, those effects do not apply.
            One owner's diary can support several maids, each wearing its own accessories.
            Eki carrying switches and Curios slot settings apply only to players.
            Tokaido provides only healing and fire immunity to maids.
            """;

    private ConfigFilesDescription() {}
}
