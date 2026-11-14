package com.thelongtravail.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class AzraelConfig {
    public static ForgeConfigSpec.DoubleValue MALICE, TARGET, SELF;
    public static ForgeConfigSpec.IntValue ATTEMPTS;
    public static ForgeConfigSpec.IntValue WATCH_SECONDS;
    public static ForgeConfigSpec.BooleanValue KILL_FALLBACK, DEATH_FALLBACK;

    public static void define(ForgeConfigSpec.Builder b) {
        WATCH_SECONDS =
                b.comment(
                                "强制处决后的最长防护时间，单位：秒（20 游戏刻）；仅影响之后的处决。\n"
                                        + "Maximum protection duration after forced execution, in"
                                        + " seconds (20 ticks); affects future executions only.")
                        .defineInRange("items.azrael.executionWatchSeconds", 300, 10, 3600);
        MALICE =
                b.comment(
                                "恶意状态下，受击后首次实际造成伤害时处决目标的概率；0.025 表示 2.5%。\n"
                                        + "Chance to execute the first target actually damaged after"
                                        + " receiving an attack during Malice; 0.025 means 2.5%.")
                        .defineInRange("items.azrael.maliceChance", .025, 0, 1);
        TARGET =
                b.comment(
                                "见证状态下处决目标的概率；与 selfChance 之和超过 1 时，按原比例缩放至合计 1。\n"
                                        + "Witness chance to execute the target; if this and selfChance"
                                        + " total more than 1, both are scaled proportionally to total"
                                        + " 1.")
                        .defineInRange("items.azrael.targetChance", .005, 0, 1);
        SELF =
                b.comment(
                                "幽谷之见证生效时，每次判定中处决佩戴者的概率；默认值0.005表示0.5%。\n"
                                        + "With Deep Valley Witness active, the chance to execute the"
                                        + " wearer on each roll; the default, 0.005, means 0.5%.")
                        .defineInRange("items.azrael.selfChance", .005, 0, 1);
        ATTEMPTS =
                b.comment(
                                "处决时最多尝试造成伤害的次数；成功后停止。\n"
                                        + "Maximum damage attempts during execution; stops on success.")
                        .defineInRange("items.azrael.damageAttempts", 10, 1, 10);
        KILL_FALLBACK =
                b.comment(
                                "伤害处决失败后，是否追加一次 generic_kill 伤害。\n"
                                        + "Attempt generic_kill damage once if the initial damage"
                                        + " execution fails.")
                        .define("items.azrael.genericKillFallback", true);
        DEATH_FALLBACK =
                b.comment(
                                "前两步处决失败后，是否直接强制死亡；可能跳过自定义掉落或死亡清理。\n"
                                        + "Force death if both earlier execution stages fail; may"
                                        + " bypass custom drops or death cleanup.")
                        .define("items.azrael.directDeathFallback", true);
    }

    public static double targetChance() {
        return normalized(TARGET.get(), SELF.get());
    }

    public static double selfChance() {
        return normalized(SELF.get(), TARGET.get());
    }

    public static double normalized(double value, double other) {
        return value / Math.max(1, value + other);
    }

    private AzraelConfig() {}
}
