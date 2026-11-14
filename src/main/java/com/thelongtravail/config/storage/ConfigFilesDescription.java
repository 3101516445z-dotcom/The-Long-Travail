package com.thelongtravail.config.storage;

import net.minecraftforge.common.ForgeConfigSpec;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class ConfigFilesDescription {
    public static final String GUIDE = guide("zh_cn");
    public static final String GUIDE_EN = guide("en_us");

    public static String itemLabel(String id) {
        return switch (id) {
            case "spring_corner" -> "春日一隅\nA Corner of Spring";
            case "affection" -> "喜欢的心情\nAffection";
            case "mistletoe" -> "槲寄生\nMistletoe";
            case "everlasting_flower" -> "无穷花\nEverlasting Flower";
            case "immortal_bud" -> "永生芽\nImmortal Bud";
            case "moon_toad_pastry" -> "月蟾酥\nMoon Toad Pastry";
            case "eki" -> "駅\nEki";
            case "tokaido" -> "东海道五十三次\nThe Fifty-three Stations of the Tokaido";
            case "perfect_fluid" -> "完美流体\nPerfect Fluid";
            case "midnight_lafite" -> "午夜拉菲\nMidnight Lafite";
            case "liquefied_abundance" -> "液化丰饶\nLiquefied Abundance";
            case "sea_in_a_bottle" -> "瓶中海\nSea in a Bottle";
            case "golden_age" -> "黄金时代\nGolden Age";
            case "icarus" -> "伊卡洛斯\nIcarus";
            case "don_quixote" -> "堂吉诃德\nDon Quixote";
            case "tower_of_babel" -> "巴别塔\nTower of Babel";
            case "sun_chaser" -> "逐日\nSun Chaser";
            case "horizon_theory" -> "地平论\nHorizon Theory";
            case "azrael" -> "死告天使\nAzrael";
            case "sword_and_lantern" -> "剑与灯\nSword and Lantern";
            case "phosphophyllite" -> "磷叶石\nPhosphophyllite";
            case "underground_velvet" -> "地下丝绒\nUnderground Velvet";
            case "high_frequency_tremor" -> "高频战栗\nHigh Frequency Tremor";
            case "last_affairs" -> "身后事\nLast Affairs";
            case "a_thousand_years_later" -> "一千年后\nA Thousand Years Later";
            case "book_of_the_dead" -> "死者之书\nBook of the Dead";
            case "demon_king_of_the_sixth_heaven" -> "第六天魔王\nDemon King of the Sixth Heaven";
            case "myriad_souls_banner" -> "万魂幡\nMyriad Souls Banner";
            case "kagotsurube" -> "籠釣瓶\nKagotsurube";
            case "living_flesh" -> "活血肉\nLiving Flesh";
            case "free_and_easy_wandering" -> "逍遥游\nFree and Easy Wandering";
            case "star_voice" -> "星之声\nVoice of the Stars";
            case "little_prince" -> "小王子\nThe Little Prince";
            case "lorentz_fantasy" -> "洛伦兹幻想\nLorentz Fantasy";
            case "ancient_strange_tales" -> "古今语怪\nAncient Strange Tales";
            case "holy_fool" -> "圣愚\nHoly Fool";
            default -> throw new IllegalArgumentException("Unknown theme item: " + id);
        };
    }

    public static String presetMarker(String modName, String category, String categoryEnglish) {
        return modName + " " + category + "预设\n" + modName + " " + categoryEnglish + " presets";
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
                                + "仅影响本客户端，保存后自动重新加载。\n"
                                + "Client-only; reloads after saving.\n"
                                + "名称光晕档位：HIGH 高、MEDIUM 中、LOW 低、OFF 关闭。\n"
                                + "Name halo levels: HIGH, MEDIUM, LOW, OFF.");
                section(
                        builder,
                        "visualDeprivation",
                        "本客户端的视觉剥夺显示设置。\n" + "Local Visual Deprivation display settings.");
                section(builder, "messages", "本地技能提示显示。\nLocal skill-message display settings.");
                section(builder, "timeStop", "时停边界显示。\n" + "Time-stop boundary display.");
            }
            case "general.toml" -> {
                section(
                        builder,
                        "starter",
                        "The Long Travail / 苦旅 — general.toml\n" + "通用规则\n" + "Shared rules");
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
                        "苦旅自身的受击恶意触发规则。\nOn-hit trigger rules for The Long Travail's own Malice effects.");
                section(
                        builder,
                        "carrying",
                        "携带位置开关，适用于駅、巴别塔和小王子；駅和巴别塔还须启用各自的对应开关。\n"
                                + "Carrying locations for Eki, Tower of Babel and Little Prince; Eki"
                                + " and Babel also require their corresponding per-item switches.");
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
                section(builder, "compatibility", "其他功能开关\n" + "Other feature switches");
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
                section(builder, "items.wayguide", "路引\n" + "Wayguide");
                section(builder, "items.wayguide.search", "搜索范围与等待时间。\nSearch range and timeout.");
                section(
                        builder,
                        "items.wayguide.sampling",
                        "高级设置：搜索的精细程度，通常无需修改。\n"
                                + "Advanced: sampling precision; usually no change is needed.");
                section(
                        builder,
                        "items.wayguide.budget",
                        "高级设置：限制搜索的处理次数和耗时，通常无需修改。\n"
                                + "Server-wide per-game-tick and per-search limits on operations and"
                                + " elapsed time.");
                section(
                        builder,
                        "items.wayguide.cache",
                        "高级设置：搜索结果保留多久，通常无需修改。\n"
                                + "Advanced: search-result cache; usually no change is needed.");
            }
            case "aspects/abyss.toml" -> {
                section(
                        builder,
                        "travail",
                        "The Long Travail / 苦旅 — aspects/abyss.toml\n" + "归墟主题\n" + "Abyss theme");
                section(
                        builder,
                        "travail.requirements",
                        "探索条件池，仅影响新旅程。抽取数量见 general.toml 的 requirements。\n"
                                + "Exploration pools for new journeys; draw counts are in general.toml"
                                + " under requirements.");
                section(builder, "travail.malice", "苦旅恶意\n" + "The Long Travail: Malice");
                section(builder, "travail.witness", "苦旅见证\n" + "The Long Travail: Witness");
                section(builder, "items", "归墟主题物品\n" + "Abyss items");
                section(builder, "items.eki", itemLabel("eki"));
                section(
                        builder,
                        "items.eki.carrying",
                        "携带位置开关；须同时启用 general.toml 中 carrying 下对应位置的开关。\n"
                                + "Carrying locations; the matching switch under carrying in"
                                + " general.toml must also be enabled.");
                section(builder, "items.tokaido", itemLabel("tokaido"));
                section(
                        builder,
                        "items.tokaido.skill",
                        "主动唤雨；时长与冷却修改只影响之后释放的技能。\n"
                                + "Active Call Rain skill. Duration and cooldown changes affect only"
                                + " future casts.");
                section(builder, "items.perfect_fluid", itemLabel("perfect_fluid"));
                section(builder, "items.liquefied_abundance", itemLabel("liquefied_abundance"));
                section(builder, "items.midnight_lafite", itemLabel("midnight_lafite"));
            }
            case "aspects/boundless.toml" -> {
                section(
                        builder,
                        "travail",
                        "The Long Travail / 苦旅 — aspects/boundless.toml\n"
                                + "无垠主题\n"
                                + "Boundless theme");
                section(
                        builder,
                        "travail.requirements",
                        "探索条件池，仅影响新旅程。抽取数量见 general.toml 的 requirements。\n"
                                + "Exploration pools for new journeys; draw counts are in general.toml"
                                + " under requirements.");
                section(builder, "travail.malice", "苦旅恶意\n" + "The Long Travail: Malice");
                section(builder, "travail.witness", "苦旅见证\n" + "The Long Travail: Witness");
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
                                + "Deep Valley theme");
                section(
                        builder,
                        "travail.requirements",
                        "探索条件池，仅影响新旅程。抽取数量见 general.toml 的 requirements。\n"
                                + "Exploration pools for new journeys; draw counts are in general.toml"
                                + " under requirements.");
                section(builder, "travail.malice", "苦旅恶意\n" + "The Long Travail: Malice");
                section(builder, "travail.witness", "苦旅见证\n" + "The Long Travail: Witness");
                section(
                        builder,
                        "travail.visualDeprivation",
                        "视觉剥夺：持续时间、过渡与画面样式；触发概率见 travail.malice。\n"
                                + "Visual Deprivation: duration, transitions, and appearance. Trigger"
                                + " chance is under travail.malice.");
                section(builder, "items", "幽谷主题物品\n" + "Deep Valley items");
                section(builder, "items.high_frequency_tremor", itemLabel("high_frequency_tremor"));
                section(builder, "items.azrael", itemLabel("azrael"));
                section(builder, "items.phosphophyllite", itemLabel("phosphophyllite"));
                section(builder, "items.underground_velvet", itemLabel("underground_velvet"));
            }
            case "aspects/far_reach.toml" -> {
                section(
                        builder,
                        "travail",
                        "The Long Travail / 苦旅 — aspects/far_reach.toml\n"
                                + "穷遐主题\n"
                                + "Far Reach theme");
                section(
                        builder,
                        "travail.requirements",
                        "探索条件池，仅影响新旅程。抽取数量见 general.toml 的 requirements。\n"
                                + "Exploration pools for new journeys; draw counts are in general.toml"
                                + " under requirements.");
                section(builder, "travail.malice", "苦旅恶意\n" + "The Long Travail: Malice");
                section(builder, "travail.witness", "苦旅见证\n" + "The Long Travail: Witness");
                section(builder, "items", "穷遐主题物品\n" + "Far Reach items");
                section(
                        builder,
                        "items.babel.carrying",
                        "携带位置开关；须同时启用 general.toml 中 carrying 下对应位置的开关。\n"
                                + "Carrying locations; the matching switch under carrying in"
                                + " general.toml must also be enabled.");
                section(builder, "items.golden_age", itemLabel("golden_age"));
                section(builder, "items.icarus", itemLabel("icarus"));
            }
            case "aspects/flourishing.toml" -> {
                section(builder, "items.immortal_bud", itemLabel("immortal_bud"));
                section(
                        builder,
                        "travail",
                        "The Long Travail / 苦旅 — aspects/flourishing.toml\n"
                                + "繁茂主题\n"
                                + "Flourishing theme");
                section(
                        builder,
                        "travail.requirements",
                        "探索条件池，仅影响新旅程。抽取数量见 general.toml 的 requirements。\n"
                                + "Exploration pools for new journeys; draw counts are in general.toml"
                                + " under requirements.");
                section(builder, "travail.malice", "苦旅恶意\n" + "The Long Travail: Malice");
                section(builder, "travail.witness", "苦旅见证\n" + "The Long Travail: Witness");
                section(builder, "items", "繁茂主题物品\n" + "Flourishing items");
                section(builder, "items.spring_corner", itemLabel("spring_corner"));
                section(builder, "items.spring_corner.flowers", "花朵效果\n" + "Flower effects");
                section(builder, "items.affection", itemLabel("affection"));
            }
            case "aspects/underworld.toml" -> {
                section(
                        builder,
                        "travail",
                        "The Long Travail / 苦旅 — aspects/underworld.toml\n"
                                + "冥府主题\n"
                                + "Underworld theme");
                section(
                        builder,
                        "travail.requirements",
                        "探索条件池，仅影响新旅程。抽取数量见 general.toml 的 requirements。\n"
                                + "Exploration pools for new journeys; draw counts are in general.toml"
                                + " under requirements.");
                section(builder, "travail.malice", "苦旅恶意\n" + "The Long Travail: Malice");
                section(builder, "travail.witness", "苦旅见证\n" + "The Long Travail: Witness");
                section(
                        builder,
                        "travail.witness.fishing",
                        "冥府见证钓鱼：概率、物品池与实体池。\n"
                                + "Underworld Witness fishing: probabilities, item pools, and entity"
                                + " pools.");
                section(builder, "items.relics.king", itemLabel("demon_king_of_the_sixth_heaven"));
                section(builder, "items.relics.banner", itemLabel("myriad_souls_banner"));
            }
            default -> throw new IllegalArgumentException("Unknown config file: " + file);
        }
        itemHeadings(builder, file);
    }

    private static void itemHeadings(ForgeConfigSpec.Builder builder, String file) {
        switch (file) {
            case "aspects/flourishing.toml" -> {
                section(builder, "items.mistletoe", itemLabel("mistletoe"));
                section(builder, "items.everlasting_flower", itemLabel("everlasting_flower"));
                section(builder, "items.moon_toad_pastry", itemLabel("moon_toad_pastry"));
            }
            case "aspects/abyss.toml" -> {
                section(builder, "items.sea_in_a_bottle", itemLabel("sea_in_a_bottle"));
            }
            case "aspects/far_reach.toml" -> {
                section(builder, "items.don_quixote", itemLabel("don_quixote"));
                section(builder, "items.babel", itemLabel("tower_of_babel"));
                section(builder, "items.sun_chaser", itemLabel("sun_chaser"));
                section(builder, "items.horizon_theory", itemLabel("horizon_theory"));
            }
            case "aspects/deep_valley.toml" -> {
                section(builder, "items.swordAndLantern", itemLabel("sword_and_lantern"));
                section(builder, "items.last_affairs", itemLabel("last_affairs"));
            }
            case "aspects/underworld.toml" -> {
                section(builder, "items.thousandYears", itemLabel("a_thousand_years_later"));
                section(builder, "items.book", itemLabel("book_of_the_dead"));
                section(builder, "items.kagotsurube", itemLabel("kagotsurube"));
                section(builder, "items.living_flesh", itemLabel("living_flesh"));
            }
            case "aspects/boundless.toml" -> {
                section(builder, "items.boundless.dream", itemLabel("free_and_easy_wandering"));
                section(builder, "items.boundless.star", itemLabel("star_voice"));
                section(builder, "items.boundless.prince", itemLabel("little_prince"));
                section(builder, "items.boundless.lorentz", itemLabel("lorentz_fantasy"));
                section(builder, "items.holy_fool", itemLabel("holy_fool"));
            }
            default -> {}
        }
    }

    private static void section(ForgeConfigSpec.Builder builder, String path, String comment) {
        if (comment.startsWith("The Long Travail /")) {
            String directory = path.equals("travail") ? "上一级" : "同目录";
            String relative = path.equals("travail") ? "../" : "";
            comment +=
                    "\n填写说明："
                            + directory
                            + "“配置说明.txt”。\nEditing help: "
                            + relative
                            + "Configuration Guide.txt.";
        }
        builder.comment(comment).push(path).pop(path.split("\\.").length);
    }

    public static String carrying(String area) {
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
                    "启用外部容器的携带检测，须容器支持物品栏更新。\n"
                            + "Enable carrying detection in external containers that support inventory"
                            + " updates.";
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
