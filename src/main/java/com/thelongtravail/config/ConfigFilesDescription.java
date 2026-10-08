package com.thelongtravail.config;

// 配置文件生成与文档共用文件说明。
public final class ConfigFilesDescription {
    public static void describe(net.minecraftforge.common.ForgeConfigSpec.Builder builder, String file) {
        if (file.startsWith("aspects/")) {
            section(builder, "travail", header(file));
            section(builder, "travail.malice", "苦旅：恶意状态的惩罚与触发规则。\nThe Long Travail: penalties and trigger rules while Malice is active.");
            section(builder, "travail.witness", "苦旅：完成探索后获得的见证能力。\nThe Long Travail: Witness abilities gained after completing exploration.");
            section(builder, "travail.requirements", "苦旅探索条件池。仅影响新生成的旅程；统一抽取数量见 general.toml 的 requirements。\nLong Travail exploration requirement pools. Only affects newly generated journeys; shared draw counts are under requirements in general.toml.");
            if (file.endsWith("abyss.toml")) {
                section(builder, "items", "归墟主题关联物品；按物品ID分别管理。\nItems associated with Abyss, configured separately by item ID.");
                section(builder, "items.eki", "駅：携带駅并佩戴苦旅时生效，与归墟恶意/见证联动。\nEki: active when carried while wearing The Long Travail; interacts with Abyss Malice and Witness.");
                section(builder, "items.eki.carrying", "检测携带駅的位置；在多个位置携带时，效果不叠加。\nLocations checked for carried Eki; carrying copies in multiple locations does not stack their effects.");
                section(builder, "items.tokaido", "东海道五十三次：装备在指定Curios槽位时生效，无需佩戴苦旅。\nTokaido: active in the specified Curios slots; does not require The Long Travail.");
                section(builder, "items.tokaido.skill", "主动唤雨；时长与冷却修改只影响之后释放的技能。\nActive Call Rain skill. Duration and cooldown changes affect only future casts.");
            } else if (file.endsWith("far_reach.toml")) {
                section(builder, "items", "穷遐主题关联饰品：基础效果独立，恶意和见证分支需要佩戴苦旅。\nAccessories associated with Far Reach. Base effects work independently; Malice and Witness effects require The Long Travail.");
                section(builder, "items.golden_age", "黄金时代：护符。空手掏取不消耗方块；投入金块后按穷遐状态转化。\nGolden Age: charm. Empty-handed digging does not consume blocks; thrown gold ingots transform according to the Far Reach state.");
                section(builder, "items.icarus", "伊卡洛斯：背饰。鞘翅飞行、命中点燃、基础免疫、按世界时间计算的太阳增伤、见证魔法免疫。\nIcarus: back accessory. Elytra flight, ignition on hit, base immunities, sunlight damage bonuses based on world time, and magic immunity with Witness.");
            } else if (file.endsWith("deep_valley.toml")) {
                section(builder, "items", "幽谷主题关联饰品。\nAccessories associated with Deep Valley.");
                section(builder, "items.azrael", "Azrael：需佩戴苦旅的头饰。同类只生效一件；见证每次实际造成伤害只抽取一次，结果互斥。\nAzrael: head accessory requiring The Long Travail. Only one copy takes effect. With Witness, each instance of actual damage dealt makes one roll with mutually exclusive outcomes.");
            } else if (file.endsWith("flourishing.toml")) {
                section(builder, "items", "繁茂主题关联饰品。\nAccessories associated with Flourishing.");
                section(builder, "items.spring_game", "春日的游戏：一朵永久主花、两朵可清洗配花。\nSpringtime Game: one permanent primary flower and two secondary flowers that can be washed off.");
                section(builder, "items.spring_game.flowers", "按花朵分别调整适配和数值。禁用适配不会删除旧物品中的花朵。\nConfigure support and values separately for each flower. Disabling support does not remove flowers from existing items.");
                section(builder, "items.affection", "喜欢的心情：绑定、血量加成与防御前伤害分担。\nAffection: binding, health-based bonuses, and damage sharing before defensive reductions.");
            } else if (file.endsWith("underworld.toml")) section(builder, "travail.witness.fishing", "冥府见证钓鱼：概率、物品池与实体池。\nUnderworld Witness fishing: probabilities, item pools, and entity pools.");
            else if (file.endsWith("boundless.toml")) section(builder, "travail.witness.phantom", "无垠见证：幻翼检测、处死限制及奖励池；全服发放上限见 general.toml。\nBoundless Witness: phantom detection, kill limits, and reward pools. Server-wide delivery limits are in general.toml.");
            else if (file.endsWith("deep_valley.toml")) section(builder, "travail.visualDeprivation", "视觉剥夺：持续时间、过渡与画面样式；触发概率见 travail.malice。\nVisual Deprivation: duration, transitions, and appearance. Trigger chance is under travail.malice.");
        } else if (file.equals("general.toml")) {
            section(builder, "starter", header(file));
            section(builder, "loot", "苦旅统一战利品：每张表一次抽取，按总概率与物品权重分配。修改后/reload或重启生效。支持新增完整表ID。父子表独立抽取。\nUnified Long Travail loot: one roll per table, using the overall chance and item weights. Changes apply after /reload or a restart. Additional full table IDs are supported. Parent and child tables roll independently.");
            section(builder, "requirements", "六类主题共用探索抽取数量。仅影响新生成的旅程；各自候选池见 aspects/。\nExploration draw counts shared by all six themes. Only affects newly generated journeys; each theme's candidate pools are under aspects/.");
            section(builder, "receivedMalice", "六类主题共用的受击恶意触发规则。\nOn-hit Malice trigger rules shared by all six themes.");
            section(builder, "effectLimits", "跨主题的自动效果尝试次数限制。\nCross-theme limits on automatic effect attempts.");
            section(builder, "rewardDelivery", "全服额外奖励的发放上限；具体奖励池位于对应主题文件中。\nServer-wide extra-reward delivery limits. Reward pools are in the corresponding theme files.");
            section(builder, "compatibility", "其他模组兼容行为。\nCompatibility behavior with other mods.");
        } else if (file.equals("utility_items.toml")) {
            section(builder, "items", header(file));
            section(builder, "items.wayguide", "路引：查找当前苦旅尚未完成的探索条件。\nWayguide: locate unfinished exploration requirements for the current Long Travail.");
            section(builder, "items.wayguide.search", "搜索范围与实际时间上限。\nSearch range and real-time timeout.");
            section(builder, "items.wayguide.sampling", "群系采样精度。\nBiome sampling precision.");
            section(builder, "items.wayguide.budget", "全服每游戏刻及单次搜索的次数和耗时限制。\nServer-wide per-game-tick and per-search limits on operations and elapsed time.");
            section(builder, "items.wayguide.cache", "搜索结果的缓存设置。\nSearch-result cache settings.");
        } else if (file.equals("client.toml")) {
            section(builder, "nameEffects", header(file));
            section(builder, "visualDeprivation", "本客户端的视觉剥夺动态效果设置，不改变服务端的触发规则和强度设置。\nVisual Deprivation animation preferences for this client. Does not change server-side trigger rules or intensity settings.");
            section(builder, "messages", "本地技能提示显示。\nLocal skill-message display settings.");
            section(builder, "timeStop", "时停区域边界的本地显示与性能档位，不改变范围和豁免规则。\nLocal time-stop boundary display and performance mode. Does not change the area or exemption rules.");
        }
    }

    private static void section(net.minecraftforge.common.ForgeConfigSpec.Builder builder, String path, String comment) {
        builder.comment(comment).push(path).pop(path.split("\\.").length);
    }

    public static String header(String file) {
        String purpose = switch (file) {
            case "general.toml" -> "通用规则 / Shared rules";
            case "utility_items.toml" -> "通用功能物品 / General-purpose utility items";
            case "client.toml" -> "客户端显示偏好 / Client display preferences";
            case "aspects/flourishing.toml" -> "繁茂主题 / Flourishing theme";
            case "aspects/abyss.toml" -> "归墟主题 / Abyss theme";
            case "aspects/far_reach.toml" -> "穷遐主题 / Far Reach theme";
            case "aspects/deep_valley.toml" -> "幽谷主题 / Deep Valley theme";
            case "aspects/underworld.toml" -> "冥府主题 / Underworld theme";
            case "aspects/boundless.toml" -> "无垠主题 / Boundless theme";
            default -> throw new IllegalArgumentException("Unknown config file: " + file);
        };
        String scope = file.equals("client.toml")
                ? "仅影响本客户端。保存后自动重新加载；重启客户端可确保生效。\nOnly affects this client. Reloads automatically after saving; restarting the client ensures changes take effect."
                : "";
        String notes = "";
        if (file.equals("general.toml")) notes += "探索条件的抽取数量仅影响新生成的旅程。\nExploration draw counts affect only newly generated journeys.";
        if (file.startsWith("aspects/")) notes += "探索条件池仅影响新生成的旅程。\nExploration requirement pools affect only newly generated journeys.";
        if (file.equals("aspects/abyss.toml")) notes += "技能时长和冷却的修改仅影响之后释放的技能。\nSkill duration and cooldown changes affect only future casts.";
        return "The Long Travail / 苦旅 — " + file + "\n" + purpose + scope + notes;
    }

    public static final String GUIDE = """
            苦旅配置目录
            general.toml：跨主题公共规则。
            utility_items.toml：通用功能物品相关配置。
            client.toml：仅本客户端的显示偏好。
            aspects/：繁茂、归墟、穷遐、幽谷、冥府、无垠六个主题及其关联物品的配置。
            """;
    public static final String GUIDE_EN = """
            The Long Travail Configuration Directory
            general.toml: Shared rules across themes.
            utility_items.toml: Settings for general-purpose utility items.
            client.toml: Display preferences for this client only.
            aspects/: Settings for the six themes—Flourishing, Abyss, Far Reach, Deep Valley, Underworld, and Boundless—and their related items.
            """;
    private ConfigFilesDescription() {}
}
