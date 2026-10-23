package com.thelongtravail.config;

public final class ConfigFilesDescription {
    public static void describe(
            net.minecraftforge.common.ForgeConfigSpec.Builder builder, String file) {
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
                        "完美流体：用于复制物品。检测到车万女仆及其性格模组时，分别将初始魂符及已收纳女仆的魂符、怪兽战斗仪加入黑名单一次（不包含空魂符）；之后可自由调整。初始化记录随本配置保存。\nPerfect Fluid: duplicates items. Initial and occupied Soul Spells (excluding empty Soul Spells) and Monster Battle Device are added to the blacklist once when their respective maid mods are detected. The blacklist remains editable; initialization records belong to this configuration.");
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
                        "死告天使：玩家需同时佩戴苦旅。同类只生效一件。\n见证状态下，每次实际造成伤害只判定一次。处决目标与处决佩戴者不会同时发生。\nAzrael: players must also wear The Long Travail. Only one copy takes effect.\nWith Witness, each instance of actual damage dealt makes one roll; executing the target and executing the wearer cannot occur on the same roll.");
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
                    "苦旅统一战利品：每张表一次抽取，按总概率与物品权重分配。\n修改后/reload或重启生效。\n支持新增完整表ID。\n父子表独立抽取。\nUnified Long Travail loot: one roll per table, using the overall chance and item weights.\nChanges apply after /reload or a restart.\nAdditional full table IDs are supported.\nParent and child tables roll independently.");
            section(
                    builder,
                    "requirements",
                    "六类主题共用探索抽取数量。仅影响新生成的旅程；各自候选池见 aspects/。\nExploration draw counts shared by all six themes. Only affects newly generated journeys; each theme's candidate pools are under aspects/.");
            section(
                    builder,
                    "receivedMalice",
                    "六类主题共用的受击恶意触发规则。\nOn-hit Malice trigger rules shared by all six themes.");
            section(
                    builder,
                    "effectLimits",
                    "跨主题的自动效果尝试次数限制。\nCross-theme limits on automatic effect attempts.");
            section(
                    builder,
                    "rewardDelivery",
                    "全服额外奖励的发放上限；具体奖励池位于对应主题文件中。\nServer-wide extra-reward delivery limits. Reward pools are in the corresponding theme files.");
            section(builder, "malice", "恶意生效的通用条件。\nShared conditions for Malice effects.");
            section(
                    builder,
                    "carrying",
                    "统一携带检测：选择可以让相关物品生效的位置。\nShared carrying checks: select the locations where supported items can take effect.");
            section(builder, "journeyDetection", "探索进度检测。\nExploration progress detection.");
            section(
                    builder,
                    "upgrades",
                    "内部配置升级记录，由模组维护，请勿修改。\nInternal configuration upgrade records maintained by the mod. Do not edit.");
            section(builder, "compatibility", "其他模组兼容行为。\nCompatibility behavior with other mods.");
        } else if (file.equals("utility_items.toml")) {
            section(builder, "items", header(file));
            section(
                    builder,
                    "items.wayguide",
                    "路引：查找当前苦旅尚未完成的探索条件。\nWayguide: locate unfinished exploration requirements for the current Long Travail.");
            section(
                    builder,
                    "items.wayguide.search",
                    "搜索范围与实际时间上限。\nSearch range and real-time timeout.");
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
            net.minecraftforge.common.ForgeConfigSpec.Builder builder,
            String path,
            String comment) {
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
            notes +=
                    "\n探索条件的抽取数量仅影响新生成的旅程。\nExploration draw counts affect only newly generated journeys.";
        if (file.startsWith("aspects/"))
            notes +=
                    "\n探索条件池仅影响新生成的旅程。\nExploration requirement pools affect only newly generated journeys.";
        if (file.equals("aspects/abyss.toml"))
            notes +=
                    "\n技能时长和冷却的修改仅影响之后释放的技能。\nSkill duration and cooldown changes affect only future casts.";
        return "The Long Travail / 苦旅 — "
                + file
                + "\n"
                + purpose
                + (scope.isEmpty() ? "" : "\n" + scope)
                + notes;
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

            阅读顺序
            general.toml：初始物品、探索规则、恶意、携带检测与限制、兼容、战利品、内部升级记录。
            主题文件：恶意、见证、其他主题规则、探索条件池、关联物品。
            upgrades 和带有初始化记录说明的字段由模组维护，请勿手动修改。

            配置写错时如何处理
            使用 UTF-8 编码，可保留文件开头的 UTF-8 BOM。数值推荐直接填写，如 chance = 0.07；合法的数字字符串也会自动转换。

            启动时：缺值、括号或引号未闭合、重复键、编码损坏等导致文件无法解析时，先保存原件，再恢复该文件的默认设置，其他配置文件不变。
            普通配置项类型错误或超出范围时，只将该项改回默认值；正常补充缺失项不产生错误备份。

            运行时：无法解析的文件保留原处，继续使用已加载的配置，等待修正后保存。
            普通配置项的非法值沿用本次运行的有效值，其他合法修改继续生效。

            战利品：按整张表校验。某表的概率、物品 ID、权重或数量有误时，沿用该表本次运行的有效规则；没有旧规则则暂停该表的苦旅奖励。
            其他有效表和完美流体钓鱼概率独立处理。合法删除、概率为 0 或物品列表为空会停止对应奖励。

            原件与修复：恢复损坏文件或自动纠正已有值前，原始内容会保存到 invalid/<时间与唯一编号>/，保留主题子目录，并附 errors.txt 说明原因。
            原件保存失败时不覆盖配置。备份不自动删除，也不会在重启后自动恢复；需要旧设置时，从原件中取回并修正后填入当前 TOML 文件。

            日志与提醒：单人玩家或服务器管理员会收到一次汇总提醒。重复警告会被抑制，每 60 秒最多输出 20 条详细警告，其余约每分钟汇总数量和涉及文件，停服时补齐剩余汇总。
            按日志或 errors.txt 定位问题，修正当前配置并保存；共享战利品还需执行 /reload 或重启。

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
            general.toml: shared rules, exploration draw counts, unified loot, and carrying location switches.
            Eki, Babel, and Little Prince share the carrying service. Legacy Eki and Babel location switches also apply alongside the shared switches.
            utility_items.toml: general-purpose items such as the Wayguide.
            client.toml: local display preferences only.
            aspects/: six themes, their associated items, and separate exploration pools.
            Most settings reload after saving; unified loot also needs /reload or a restart.
            Pool and draw-count changes affect new journeys only.

            Reading order
            general.toml: starter items, exploration rules, Malice, carrying and limits, compatibility, loot, internal upgrade records.
            Theme files: Malice, Witness, other theme rules, exploration pools, associated items.
            The mod maintains upgrades and fields described as initialization records. Do not edit them manually.

            Handling configuration errors
            Use UTF-8; a leading UTF-8 BOM is accepted. Prefer plain numbers, such as chance = 0.07. Valid numeric strings are also converted automatically.

            At startup: if missing values, unclosed brackets or quotes, duplicate keys, or damaged encoding prevent parsing, the original file is preserved before restoring that file's defaults. Other configuration files are unchanged.
            Ordinary settings with invalid types or out-of-range values use their defaults individually. Filling in missing settings normally creates no error backup.

            During play: files that cannot be parsed remain on disk for repair, while the loaded configuration stays active. Save the corrected file to retry.
            Invalid ordinary values retain their effective values from this session; other valid edits still apply.

            Loot: each table is validated as a whole. Invalid chances, item IDs, weights or quantities retain that table's effective rules from this session; without previous rules, its added rewards are disabled.
            Other valid tables and Perfect Fluid's fishing chance are handled independently. Valid deletions, zero chance and empty item lists stop the affected rewards.

            Originals and repair: before restoring a damaged file or correcting existing values, the original contents are saved under invalid/<timestamp-and-unique-id>/ with theme subdirectories preserved and an errors.txt report.
            If preserving the original fails, the configuration is not overwritten. Backups are not deleted or restored automatically; recover any wanted settings from the original, correct them, and put them into the current TOML file.

            Logs and notices: single-player users or server administrators receive one summary notice. Duplicate warnings are suppressed. At most 20 detailed warnings are logged per 60 seconds; omitted counts and affected files are summarized about once a minute, with any pending summary flushed when the server stops.
            Locate the issue using the log or errors.txt, then correct and save the current configuration. Unified loot also needs /reload or a restart.

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
