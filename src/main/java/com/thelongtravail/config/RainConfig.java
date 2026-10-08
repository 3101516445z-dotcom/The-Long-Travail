package com.thelongtravail.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraft.resources.ResourceLocation;
import java.util.List;

public final class RainConfig {
    public enum Weather { RAIN_OR_THUNDER, THUNDER_ONLY, ANY_WEATHER }
    public enum Repeat { REFRESH, REJECT }
    public static final int MAX_SLOT_LENGTH = 128;
    public static ForgeConfigSpec.DoubleValue DAMAGE, SPEED, IMMUNITY, HEAL, HEAL_SECONDS,
            SATURATION, FOOD_SECONDS, DURATION, COOLDOWN;
    public static ForgeConfigSpec.IntValue FOOD;
    public static ForgeConfigSpec.BooleanValue MALICE_SKIP, IMMUNE_SKIP, FALLBACK,
            INVENTORY, OFFHAND, ENDER, ARMOR, CURIOS, EXTERNAL_TICK, FIRE,
            EKI_LOCAL, EKI_SKY, TOKAIDO_LOCAL, TOKAIDO_SKY, DRY_RAIN, SNOW_RAIN, SKILL_ENABLED;
    public static ForgeConfigSpec.EnumValue<Weather> EKI_WEATHER, TOKAIDO_WEATHER;
    public static ForgeConfigSpec.EnumValue<Repeat> REPEAT;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> IMMUNITY_EXCLUSIONS, SLOTS;

    public static void define(ForgeConfigSpec.Builder b) {

        DAMAGE = number(b, "items.eki.damageBonus", .5, 0, 1000, "最终有效伤害的增加比例；0.5表示增加50%，0表示无加成。\nFractional increase in final effective damage; 0.5 means +50%, and 0 means no bonus.");
        SPEED = number(b, "items.eki.speedBonus", .2, 0, 100, "移动速度的增加比例，与无垠的速度倍率相乘；僵硬期间移动速度仍为零。\nFractional movement speed increase, multiplied by the Boundless speed multiplier. Movement speed remains zero while Stiff is active.");
        IMMUNITY = number(b, "items.eki.immunityChance", .75, 0, 1, "拥有归墟之见证时，駅单独判定的免伤概率。\nEki's separate chance to negate damage while Abyss Witness is active.");
        MALICE_SKIP = flag(b, "items.eki.skipMaliceOnHit", true, "处于归墟之恶意状态时，跳过苦旅的受击惩罚。\nSkip The Long Travail's on-hit penalties while Abyss Malice is active.");
        IMMUNE_SKIP = flag(b, "items.eki.skipPenaltyOnImmunity", true, "駅免伤成功时，跳过苦旅的受击惩罚；关闭后仍按配置的受击触发方式处理。\nSkip The Long Travail's on-hit penalties when Eki negates damage. When disabled, penalties still follow the configured received-hit trigger mode.");
        FALLBACK = flag(b, "items.eki.fallbackToAbyss", true, "駅免伤失败后，继续按归墟之见证原有的条件判定免伤。\nIf Eki fails to negate damage, continue checking Abyss Witness's original immunity conditions.");
        IMMUNITY_EXCLUSIONS = b.comment("只排除駅额外免伤的伤害类型ID；minecraft:generic_kill始终排除。\nDamage type IDs excluded only from Eki's additional damage immunity; minecraft:generic_kill is always excluded.")
                .defineListAllowEmpty("items.eki.immunityExclusions", List.of(), RainConfig::id);
        EKI_WEATHER = b.comment("天气要求；ANY_WEATHER仍只在主世界生效。\nWeather requirement; even ANY_WEATHER applies only in the Overworld.").defineEnum("items.eki.weather", Weather.RAIN_OR_THUNDER);
        EKI_LOCAL = flag(b, "items.eki.requireLocalPrecipitation", false, "开启后，仅在允许降雨或降雪的群系中生效；是否要求露天由 requireOpenSky 控制。\nWhen enabled, applies only in biomes that allow rain or snow. The open-sky requirement is controlled by requireOpenSky.");
        EKI_SKY = flag(b, "items.eki.requireOpenSky", false, "开启后，仅在玩家头顶无遮挡时生效。\nWhen enabled, applies only while the player has open sky overhead.");

        INVENTORY = flag(b, "items.eki.carrying.inventory", true, "检查主物品栏，包含快捷栏和主手。\nCheck the main inventory, including the hotbar and main hand.");
        OFFHAND = flag(b, "items.eki.carrying.offhand", true, "检查副手。\nCheck the offhand.");
        ENDER = flag(b, "items.eki.carrying.enderChest", true, "检查末影箱。\nCheck the ender chest.");
        ARMOR = flag(b, "items.eki.carrying.armor", true, "检查四个原版护甲槽；此设置不会赋予駅装备到护甲槽的能力。\nCheck the four vanilla armor slots. This does not make Eki equippable in armor slots.");
        CURIOS = flag(b, "items.eki.carrying.curios", true, "检查Curios装备槽，不检查外观槽。\nCheck equipped Curios slots, excluding cosmetic slots.");
        EXTERNAL_TICK = flag(b, "items.eki.carrying.externalInventoryTick", true, "允许通过外部容器模拟的物品栏更新检测駅；需要该容器支持并启用此功能。检测记录最多保留2游戏刻，不写入存档。\nAllow Eki detection through inventory updates simulated by external containers. The container must support and enable this feature. Detection records last at most 2 game ticks and are not saved.");


        SLOTS = b.comment("东海道五十三次可生效的Curios槽位。自定义槽位还需通过数据包允许装备此物品；装备多个时效果不叠加。\nCurios slots in which Tokaido can take effect. Custom slots also require a data pack allowing this item to be equipped there. Equipping multiple copies does not stack their effects.")
                .defineListAllowEmpty("items.tokaido.slots", List.of("feet"), v -> v instanceof String s && s.length() <= MAX_SLOT_LENGTH && s.matches("[a-z0-9_/-]+"));
        TOKAIDO_WEATHER = b.comment("天气要求；ANY_WEATHER仍只在主世界生效。\nWeather requirement; even ANY_WEATHER applies only in the Overworld.").defineEnum("items.tokaido.weather", Weather.RAIN_OR_THUNDER);
        TOKAIDO_LOCAL = flag(b, "items.tokaido.requireLocalPrecipitation", false, "开启后，仅在允许降雨或降雪的群系中生效。\nWhen enabled, applies only in biomes that allow rain or snow.");
        TOKAIDO_SKY = flag(b, "items.tokaido.requireOpenSky", false, "开启后，仅在玩家头顶无遮挡时生效。\nWhen enabled, applies only while the player has open sky overhead.");
        HEAL = number(b, "items.tokaido.healing", 1, 0, 1000000, "每次恢复的生命值，2点为1颗心；仍受其他治疗加成和限制影响。\nHealth restored each time; 2 points equal 1 heart. Other healing bonuses and restrictions still apply.");
        HEAL_SECONDS = number(b, "items.tokaido.healingIntervalSeconds", 1, .05, 1000000, "治疗间隔，单位：秒，最短0.05秒（1游戏刻）。\nHealing interval, in seconds; minimum 0.05 seconds (1 game tick).");
        FIRE = flag(b, "items.tokaido.fireResistance", true, "提供与原版抗火相同的伤害免疫，但不添加抗火状态效果。\nProvide the same damage immunity as vanilla Fire Resistance without adding the Fire Resistance status effect.");
        FOOD = b.comment("每次恢复的饱食度；设为0时仍可恢复饱和度。\nHunger restored each time; setting this to 0 still allows saturation restoration.").defineInRange("items.tokaido.food", 1, 0, 20);
        SATURATION = number(b, "items.tokaido.saturation", .5, 0, 20, "每次恢复的饱和度点数，不是饱和度倍率。\nSaturation points restored each time, not a saturation multiplier.");
        FOOD_SECONDS = number(b, "items.tokaido.foodIntervalSeconds", 5, .05, 1000000, "饱食度与饱和度的恢复间隔，单位：秒。\nInterval between hunger and saturation restorations, in seconds.");

        SKILL_ENABLED = flag(b, "items.tokaido.skill.enabled", true, "启用主动唤雨；关闭后被动效果仍然生效，并恢复技能生效前的天气。\nEnable the active Call Rain skill. Disabling it leaves passive effects active and restores the weather from before the skill took effect.");
        DURATION = number(b, "items.tokaido.skill.durationSeconds", 60, .05, 1000000, "技能降雨的持续时间，单位：秒；修改不影响已经释放的技能。\nDuration of rain caused by the skill, in seconds. Changes do not affect skills already cast.");
        COOLDOWN = number(b, "items.tokaido.skill.cooldownSeconds", 300, 0, 1000000, "技能冷却时间，单位：秒；0表示无冷却。离线时暂停计时，修改不影响已有冷却。\nSkill cooldown, in seconds; 0 means no cooldown. Paused while offline; changes do not affect existing cooldowns.");
        DRY_RAIN = flag(b, "items.tokaido.skill.rainInDryBiomes", true, "技能期间允许干燥群系降雨。\nAllow rain in dry biomes while the skill is active.");
        SNOW_RAIN = flag(b, "items.tokaido.skill.replaceSnowWithRain", true, "技能持续期间，将降雪改为降雨，并暂停当地自然积雪和结冰。\nReplace snow with rain while the skill is active, and suspend natural snow accumulation and ice formation locally.");
        REPEAT = b.comment("REFRESH：重新开始计算完整持续时间。\nREJECT：拒绝重复释放，且不进入冷却。\nREFRESH: restart the full duration.\nREJECT: reject repeat casts without starting a cooldown.")
                .defineEnum("items.tokaido.skill.repeatCast", Repeat.REFRESH);



    }
    public static int ticks(double seconds) { return Math.max(0, (int)Math.ceil(seconds * 20)); }
    private static boolean id(Object v) { return v instanceof String s && ResourceLocation.tryParse(s) != null; }
    private static ForgeConfigSpec.BooleanValue flag(ForgeConfigSpec.Builder b, String key, boolean value, String comment) {
        return b.comment(comment).define(key, value);
    }
    private static ForgeConfigSpec.DoubleValue number(ForgeConfigSpec.Builder b, String key, double value, double min, double max, String comment) {
        return b.comment(comment).defineInRange(key, value, min, max);
    }
    private RainConfig() {}
}
