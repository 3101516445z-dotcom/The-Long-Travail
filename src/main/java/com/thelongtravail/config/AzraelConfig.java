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
                                "强制处决后主动防护的最长持续时间，单位：秒，每秒按 20 游戏刻计算。默认 300 秒；未被重新插入世界的目标只观察 10 秒。目标被重新插入或清理失败均不会重置此时限。到期后停止主动清理，但仍保留对旧实体实例的被动拦截。修改仅影响之后发生的处决。\nMaximum duration of active protection after a forced execution, in seconds at 20 game ticks per second. Defaults to 300 seconds; targets that are not reinserted are observed for only 10 seconds. Reinsertion and cleanup failures do not reset this limit. Active cleanup stops when the limit expires, but passive guards against the old entity instance remain. Changes apply only to subsequent executions.")
                        .defineInRange("items.azrael.executionWatchSeconds", 300, 10, 3600);
        MALICE =
                b.comment(
                                "幽谷之恶意生效时，受击可获得一次处决判定机会，自身造成的攻击除外。\n此值为接下来首次实际造成伤害时，处决目标的概率。\n处决判定次数和处决概率均不可累计；\n0.025表示2.5%。\nWith Deep Valley Malice active, being attacked grants one execution attempt, except for attacks from the wearer.\nThis is the chance to execute the first target the wearer actually damages afterward.\nNeither execution attempts nor execution chance can accumulate.\n0.025 means 2.5%.")
                        .defineInRange("items.azrael.maliceChance", .025, 0, 1);
        TARGET =
                b.comment(
                                "幽谷之见证生效时，每次判定中处决目标的概率。\n处决目标与处决佩戴者不会在同一次判定中同时发生；\n两项概率之和超过1时，按原比例缩放至合计1。\nWith Deep Valley Witness active, the chance to execute the target on each roll.\nExecuting the target and executing the wearer are mutually exclusive outcomes of the same roll.\nIf their chances total more than 1, they are scaled proportionally to total 1.")
                        .defineInRange("items.azrael.targetChance", .005, 0, 1);
        SELF =
                b.comment(
                                "幽谷之见证生效时，每次判定中处决佩戴者的概率；默认值0.005表示0.5%。\nWith Deep Valley Witness active, the chance to execute the wearer on each roll; the default, 0.005, means 0.5%.")
                        .defineInRange("items.azrael.selfChance", .005, 0, 1);
        ATTEMPTS =
                b.comment(
                                "处决第一阶段使用无限大真实伤害的最大尝试次数。每次尝试前重置原版受伤无敌时间，处决成功后停止尝试。\nMaximum number of attempts to deal infinite true damage during the first execution stage. Vanilla damage-invulnerability time is reset before each attempt. Stops as soon as execution succeeds.")
                        .defineInRange("items.azrael.damageAttempts", 10, 1, 10);
        KILL_FALLBACK =
                b.comment(
                                "第一阶段失败后，尝试一次保留玩家来源的generic_kill伤害。\nIf the first stage fails, attempt generic_kill damage once, retaining the player as its source.")
                        .define("items.azrael.genericKillFallback", true);
        DEATH_FALLBACK =
                b.comment(
                                "前两个阶段均失败后，跳过可取消死亡检查并锁定旧实体生命值；玩家保留重生流程，非玩家调用原版死亡方法后强制终结，可能绕过自定义掉落或Boss清理。\nIf both earlier stages fail, bypass cancelable death checks and lock the old entity instance's health. Players retain the respawn process. Non-player entities run the vanilla death method before being forcibly terminated, which may bypass custom drops or boss cleanup.")
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
