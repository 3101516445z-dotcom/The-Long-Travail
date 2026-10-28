package com.thelongtravail.config;

import com.thelongtravail.common.TickDurations;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public final class RainConfig {
    public enum Repeat {
        REFRESH,
        REJECT
    }

    public static final int MAX_SLOT_LENGTH = 128;
    public static ForgeConfigSpec.DoubleValue DAMAGE,
            SPEED,
            IMMUNITY,
            HEAL,
            HEAL_SECONDS,
            SATURATION,
            FOOD_SECONDS,
            DURATION,
            COOLDOWN;
    public static ForgeConfigSpec.IntValue FOOD;
    public static ForgeConfigSpec.BooleanValue MALICE_SKIP,
            IMMUNE_SKIP,
            FALLBACK,
            INVENTORY,
            OFFHAND,
            ENDER,
            ARMOR,
            CURIOS,
            EXTERNAL_TICK,
            FIRE,
            EKI_LOCAL,
            EKI_SKY,
            TOKAIDO_LOCAL,
            TOKAIDO_SKY,
            DRY_RAIN,
            SNOW_RAIN,
            SKILL_ENABLED;
    public static ForgeConfigSpec.EnumValue<Repeat> REPEAT;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> IMMUNITY_EXCLUSIONS, SLOTS;

    public static void define(ForgeConfigSpec.Builder b) {

        DAMAGE =
                number(
                        b,
                        "items.eki.damageBonus",
                        .4,
                        0,
                        1000,
                        "最终有效伤害的增加比例；0.4 表示提高 40%，0 表示不提供加成。\n"
                                + "Fractional increase in final effective damage; 0.4 means a 40%"
                                + " increase, and 0 disables the bonus.");
        SPEED =
                number(
                        b,
                        "items.eki.speedBonus",
                        .2,
                        0,
                        100,
                        "移动速度的增加比例，与无垠的速度倍率相乘；僵硬期间移动速度仍为零。\n"
                                + "Fractional movement speed increase, multiplied by the Boundless"
                                + " speed multiplier. Movement speed remains zero while Stiff is"
                                + " active.");
        IMMUNITY =
                number(
                        b,
                        "items.eki.immunityChance",
                        .40,
                        0,
                        1,
                        "归墟之见证生效时，駅额外取消伤害的概率。\n"
                                + "Additional chance for Eki to negate damage while Abyss Witness is"
                                + " active.");
        MALICE_SKIP =
                flag(
                        b,
                        "items.eki.skipMaliceOnHit",
                        true,
                        "处于归墟之恶意状态时，跳过苦旅的受击惩罚。\n"
                                + "Skip The Long Travail's on-hit penalties while Abyss Malice is"
                                + " active.");
        IMMUNE_SKIP =
                flag(
                        b,
                        "items.eki.skipPenaltyOnImmunity",
                        true,
                        "駅免伤成功时，跳过苦旅的受击惩罚；关闭后仍按配置的受击触发方式处理。\n"
                                + "Skip The Long Travail's on-hit penalties when Eki negates damage."
                                + " When disabled, penalties still follow the configured received-hit"
                                + " trigger mode.");
        FALLBACK =
                flag(
                        b,
                        "items.eki.fallbackToAbyss",
                        true,
                        "駅未能取消伤害时，继续按归墟之见证原有的免伤条件判定。\n"
                                + "If Eki does not negate the damage, check the normal immunity"
                                + " conditions for Abyss Witness.");
        IMMUNITY_EXCLUSIONS =
                b.comment(
                                "只排除駅额外免伤的伤害类型ID；minecraft:generic_kill始终排除。\n"
                                        + "Damage type IDs excluded only from Eki's additional damage"
                                        + " immunity; minecraft:generic_kill is always excluded.")
                        .defineListAllowEmpty(
                                "items.eki.immunityExclusions", List.of(), RainConfig::id);
        EKI_LOCAL =
                flag(
                        b,
                        "items.eki.requireLocalPrecipitation",
                        false,
                        "启用后，仅在允许降雨或降雪的群系中生效；是否要求露天由 requireOpenSky 控制。\n"
                                + "When enabled, applies only in biomes that allow rain or snow. The"
                                + " open-sky requirement is controlled by requireOpenSky.");
        EKI_SKY =
                flag(
                        b,
                        "items.eki.requireOpenSky",
                        false,
                        "启用后，仅在玩家头顶无遮挡时生效。\n"
                                + "When enabled, applies only while the player has open sky overhead.");

        INVENTORY =
                flag(
                        b,
                        "items.eki.carrying.inventory",
                        true,
                        "启用主背包与快捷栏检测。\nEnable main inventory and hotbar checks.");
        OFFHAND = flag(b, "items.eki.carrying.offhand", true, "启用副手检测。\nEnable offhand checks.");
        ENDER =
                flag(
                        b,
                        "items.eki.carrying.enderChest",
                        true,
                        "启用末影箱检测。\nEnable ender chest checks.");
        ARMOR =
                flag(
                        b,
                        "items.eki.carrying.armor",
                        true,
                        "启用四个原版盔甲槽检测；不会赋予物品装备权限。\n"
                                + "Enable checks in the four vanilla armor slots; does not grant"
                                + " equipment permission.");
        CURIOS =
                flag(
                        b,
                        "items.eki.carrying.curios",
                        true,
                        "启用 Curios 装备槽检测，不包括外观槽。\n"
                                + "Enable equipped Curios slot checks, excluding cosmetic slots.");
        EXTERNAL_TICK =
                flag(
                        b,
                        "items.eki.carrying.externalInventoryTick",
                        true,
                        "启用外部容器的模拟物品栏更新检测，须容器支持并启用。\n"
                                + "Enable simulated inventory updates from external containers;"
                                + " requires container support and activation.\n"
                                + "检测记录最多保留 2 游戏刻，不写入存档。\n"
                                + "Detection records last at most 2 game ticks and are not saved.");

        SLOTS =
                b.comment(
                                "东海道五十三次可生效的Curios槽位。自定义槽位还需通过数据包允许装备此物品；装备多个时效果不叠加。\n"
                                        + "Curios slots in which Tokaido can take effect. Custom slots"
                                        + " also require a data pack allowing this item to be equipped"
                                        + " there. Equipping multiple copies does not stack their"
                                        + " effects.")
                        .defineListAllowEmpty(
                                "items.tokaido.slots",
                                List.of("feet"),
                                v ->
                                        v instanceof String s
                                                && s.length() <= MAX_SLOT_LENGTH
                                                && s.matches("[a-z0-9_/-]+"));
        TOKAIDO_LOCAL =
                flag(
                        b,
                        "items.tokaido.requireLocalPrecipitation",
                        false,
                        "启用后，仅在允许降雨或降雪的群系中生效。\n"
                                + "When enabled, applies only in biomes that allow rain or snow.");
        TOKAIDO_SKY =
                flag(
                        b,
                        "items.tokaido.requireOpenSky",
                        false,
                        "启用后，仅在玩家头顶无遮挡时生效。\n"
                                + "When enabled, applies only while the player has open sky overhead.");
        HEAL =
                number(
                        b,
                        "items.tokaido.healing",
                        1,
                        0,
                        1000000,
                        "每次恢复的生命值，2点为1颗心；仍受其他治疗加成和限制影响。\n"
                                + "Health restored each time; 2 points equal 1 heart. Other healing"
                                + " bonuses and restrictions still apply.");
        HEAL_SECONDS =
                number(
                        b,
                        "items.tokaido.healingIntervalSeconds",
                        1,
                        .05,
                        1000000,
                        "治疗间隔，单位：秒，最短0.05秒（1游戏刻）。\n"
                                + "Healing interval, in seconds; minimum 0.05 seconds (1 game tick).");
        FIRE =
                flag(
                        b,
                        "items.tokaido.fireResistance",
                        true,
                        "提供与原版抗火相同的伤害免疫，但不添加抗火状态效果。\n"
                                + "Provide the same damage immunity as vanilla Fire Resistance without"
                                + " adding the Fire Resistance status effect.");
        FOOD =
                b.comment(
                                "每次恢复的饱食度；设为0时仍可恢复饱和度。\n"
                                        + "Hunger restored each time; setting this to 0 still allows"
                                        + " saturation restoration.")
                        .defineInRange("items.tokaido.food", 1, 0, 20);
        SATURATION =
                number(
                        b,
                        "items.tokaido.saturation",
                        .5,
                        0,
                        20,
                        "每次恢复的饱和度点数，不是饱和度倍率。\n"
                                + "Saturation points restored per application, not a saturation"
                                + " multiplier.");
        FOOD_SECONDS =
                number(
                        b,
                        "items.tokaido.foodIntervalSeconds",
                        5,
                        .05,
                        1000000,
                        "饱食度与饱和度的恢复间隔，单位：秒。\n"
                                + "Interval between hunger and saturation restorations, in seconds.");

        SKILL_ENABLED =
                flag(
                        b,
                        "items.tokaido.skill.enabled",
                        true,
                        "启用主动唤雨；关闭后被动效果仍然生效，并恢复技能生效前的天气。\n"
                                + "Enable the active Call Rain skill. Disabling it leaves passive"
                                + " effects active and restores the weather from before the skill took"
                                + " effect.");
        DURATION =
                number(
                        b,
                        "items.tokaido.skill.durationSeconds",
                        60,
                        .05,
                        1000000,
                        "技能降雨的持续时间，单位：秒；修改不影响已经释放的技能。\n"
                                + "Duration of rain caused by the skill, in seconds. Changes do not"
                                + " affect skills already cast.");
        COOLDOWN =
                number(
                        b,
                        "items.tokaido.skill.cooldownSeconds",
                        120,
                        0,
                        1000000,
                        "技能冷却时间，单位：秒；0表示无冷却。离线时暂停计时，修改不影响已有冷却。\n"
                                + "Skill cooldown, in seconds; 0 means no cooldown. Paused while"
                                + " offline; changes do not affect existing cooldowns.");
        DRY_RAIN =
                flag(
                        b,
                        "items.tokaido.skill.rainInDryBiomes",
                        true,
                        "技能期间允许干燥群系降雨。\nAllow rain in dry biomes while the skill is active.");
        SNOW_RAIN =
                flag(
                        b,
                        "items.tokaido.skill.replaceSnowWithRain",
                        true,
                        "技能持续期间，将降雪改为降雨，并暂停当地自然积雪和结冰。\n"
                                + "Replace snow with rain while the skill is active, and suspend"
                                + " natural snow accumulation and ice formation locally.");
        REPEAT =
                b.comment(
                                "REFRESH：重新开始计算完整持续时间。\n"
                                        + "REJECT：拒绝重复释放，且不进入冷却。\n"
                                        + "REFRESH: restart the full duration.\n"
                                        + "REJECT: reject repeat casts without starting a cooldown.")
                        .defineEnum("items.tokaido.skill.repeatCast", Repeat.REFRESH);
    }

    public static int ticks(double seconds) {
        return TickDurations.ceilNonNegative(seconds);
    }

    private static boolean id(Object v) {
        return v instanceof String s && ResourceLocation.tryParse(s) != null;
    }

    private static ForgeConfigSpec.BooleanValue flag(
            ForgeConfigSpec.Builder b, String key, boolean value, String comment) {
        return b.comment(comment).define(key, value);
    }

    private static ForgeConfigSpec.DoubleValue number(
            ForgeConfigSpec.Builder b,
            String key,
            double value,
            double min,
            double max,
            String comment) {
        return b.comment(comment).defineInRange(key, value, min, max);
    }

    private RainConfig() {}
}
