package com.thelongtravail.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class AzraelConfig {
    public static ForgeConfigSpec.DoubleValue MALICE, TARGET, SELF;
    public static ForgeConfigSpec.IntValue ATTEMPTS;
    public static ForgeConfigSpec.BooleanValue KILL_FALLBACK, DEATH_FALLBACK;
    public static void define(ForgeConfigSpec.Builder b) {
        MALICE = b.comment("幽谷之恶意生效时，佩戴者每次受到非自身来源的有来源伤害后，接下来首次实际造成伤害时处决目标的概率。处决判定次数和处决概率均不可累计；0.025表示2.5%。\nWith Deep Valley Malice active, the chance to execute the next target the wearer actually damages after taking damage from a source other than themselves. Neither execution attempts nor execution chance can accumulate; 0.025 means 2.5%.")
                .defineInRange("items.azrael.maliceChance", .025, 0, 1);
        TARGET = b.comment("幽谷之见证生效时，每次判定中处决目标的概率。处决目标与处决佩戴者不会在同一次判定中同时发生；两项概率之和超过1时，按原比例缩放至合计1。\nWith Deep Valley Witness active, the chance to execute the target on each roll. Executing the target and executing the wearer are mutually exclusive outcomes of the same roll. If their chances total more than 1, they are scaled proportionally to total 1.")
                .defineInRange("items.azrael.targetChance", .005, 0, 1);
        SELF = b.comment("幽谷之见证生效时，每次判定中处决佩戴者的概率；默认值0.005表示0.5%。\nWith Deep Valley Witness active, the chance to execute the wearer on each roll; the default, 0.005, means 0.5%.")
                .defineInRange("items.azrael.selfChance", .005, 0, 1);
        ATTEMPTS = b.comment("处决第一阶段使用无限大真实伤害的最大尝试次数。每次尝试前重置原版受伤无敌时间，处决成功后停止尝试。\nMaximum attempts to deal infinite true damage in the first execution stage. Vanilla hurt invulnerability is reset before each attempt; attempts stop once execution succeeds.")
                .defineInRange("items.azrael.damageAttempts", 10, 1, 10);
        KILL_FALLBACK = b.comment("第一阶段失败后，尝试一次保留玩家来源的generic_kill伤害。\nIf the first stage fails, attempt generic_kill damage once, retaining the player as its source.")
                .define("items.azrael.genericKillFallback", true);
        DEATH_FALLBACK = b.comment("前两个阶段均失败后，尝试将生命值设为0并调用死亡流程，最多尝试一次。死亡事件仍可被取消。\nIf both previous stages fail, attempt once to set health to 0 and invoke the death process. The death event can still be canceled.")
                .define("items.azrael.directDeathFallback", true);
    }
    public static double targetChance() { return normalized(TARGET.get(), SELF.get()); }
    public static double selfChance() { return normalized(SELF.get(), TARGET.get()); }
    public static double normalized(double value, double other) { return value / Math.max(1, value + other); }
    private AzraelConfig() {}
}
