package com.thelongtravail.config;

import static com.thelongtravail.config.TravailDefinitionSupport.integer;

import com.thelongtravail.config.TravailConfig.RewardOverflow;

import net.minecraftforge.common.ForgeConfigSpec;

// 奖励投递预算与溢出策略。
record RewardDeliveryDefinition(
        ForgeConfigSpec.IntValue rewardEntitiesPerTick,
        ForgeConfigSpec.IntValue rewardQueueCapacity,
        ForgeConfigSpec.EnumValue<RewardOverflow> rewardOverflow,
        ForgeConfigSpec.IntValue rewardLimit) {
    static RewardDeliveryDefinition define(ForgeConfigSpec.Builder builder) {
        ForgeConfigSpec.IntValue rewardEntitiesPerTick =
                integer(
                        builder,
                        "rewardDelivery.rewardMaxItemEntitiesPerTick",
                        32,
                        1,
                        1024,
                        "全服每游戏刻最多生成的额外奖励物品实体数；不限制原版掉落。",
                        "Maximum extra-reward item entities spawned per game tick across the"
                                + " server. Does not limit vanilla drops.");
        ForgeConfigSpec.IntValue rewardQueueCapacity =
                integer(
                        builder,
                        "rewardDelivery.rewardQueueCapacity",
                        1024,
                        1,
                        16384,
                        "待发奖励的记录数量上限。达到上限时，暂停自动击杀幻翼，并跳过钓鱼的额外物品奖励。调低上限不会删除已有记录。",
                        "Maximum pending reward records. At the limit, automatic phantom kills"
                                + " pause and extra fishing item rewards are skipped. Lowering the"
                                + " limit does not delete existing records.");
        ForgeConfigSpec.EnumValue<RewardOverflow> rewardOverflow =
                builder.comment(
                                "DEFER：分批发放全部奖励。\n"
                                        + "LIMIT：每次最多发放 rewardMaxEntitiesPerReward 堆物品，超出部分不发放。\n"
                                        + "两种方式均受全服每游戏刻发放上限限制。",
                                "DEFER: deliver all rewards in batches.\n"
                                        + "LIMIT: deliver at most rewardMaxEntitiesPerReward item"
                                        + " stacks per reward; discard the excess.\n"
                                        + "Both modes are subject to the server-wide per-tick delivery"
                                        + " limit.")
                        .defineEnum("rewardDelivery.rewardOverflowPolicy", RewardOverflow.DEFER);
        ForgeConfigSpec.IntValue rewardLimit =
                integer(
                        builder,
                        "rewardDelivery.rewardMaxEntitiesPerReward",
                        64,
                        1,
                        4096,
                        "LIMIT模式下，每次奖励最多发放的物品堆数。",
                        "Maximum item stacks delivered per reward in LIMIT mode.");
        return new RewardDeliveryDefinition(
                rewardEntitiesPerTick, rewardQueueCapacity, rewardOverflow, rewardLimit);
    }
}
