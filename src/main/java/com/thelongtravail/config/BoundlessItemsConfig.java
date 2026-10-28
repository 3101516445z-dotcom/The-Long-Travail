package com.thelongtravail.config;

import com.thelongtravail.common.TickDurations;

import net.minecraftforge.common.ForgeConfigSpec;
import java.util.*;

public final class BoundlessItemsConfig {
    public static final Map<String, ForgeConfigSpec.DoubleValue> NUMBERS = new LinkedHashMap<>();
    public static ForgeConfigSpec.IntValue PER_CHUNK;

    public static void define(ForgeConfigSpec.Builder b) {
        LorentzGeometryConfig.define(b);
        n(
                b,
                "prince.stepHeight",
                1,
                0,
                4,
                "小王子提供的最低跨步高度，单位：方块。\nLittle Prince: minimum step height, in blocks.");
        n(
                b,
                "prince.poppyChance",
                .002,
                0,
                1,
                "小王子：玩家破坏虞美人时，触发掉落的概率。\nLittle Prince: drop chance when a player breaks a poppy.");
        n(
                b,
                "prince.roseChance",
                .005,
                0,
                1,
                "小王子：玩家破坏玫瑰丛时，触发掉落的概率；每株只判定一次。\nLittle Prince: drop chance when a player "
                        + "breaks a rose bush, rolled once per plant.");
        n(
                b,
                "lorentz.pierce",
                3,
                0,
                127,
                "洛伦兹幻想：箭矢额外获得的穿透等级。\nLorentz Fantasy: additional arrow piercing levels.");
        n(
                b,
                "lorentz.critical",
                .10,
                0,
                1,
                "洛伦兹幻想：恶意状态下，通用必杀概率的增加量。\nLorentz Fantasy: amount added to the general "
                        + "critical-hit chance while Malice is active.");
        n(
                b,
                "lorentz.damage",
                .25,
                0,
                100,
                "洛伦兹幻想：恶意状态下，投射物伤害的加成比例。\nLorentz Fantasy: fractional projectile damage bonus while Malice is active.");
        n(
                b,
                "lorentz.headshot",
                .25,
                0,
                100,
                "洛伦兹幻想：恶意状态下，箭矢爆头的额外伤害加成比例，与普通伤害加成相加。\nLorentz Fantasy: additional "
                        + "fractional damage bonus for arrow headshots while Malice is active; adds "
                        + "to the normal damage bonus.");
        n(
                b,
                "lorentz.headTolerance",
                .12,
                0,
                1,
                "爆头判定向眼睛下方延伸的距离，占实体身高的比例。\nHeadshot tolerance below eye level, as a fraction of the entity's height.");
        n(
                b,
                "lorentz.voidRatio",
                .02,
                0,
                1,
                "见证状态下，直击伤害占命中时目标当前生命值的比例。\nDirect-strike damage with Witness, as a fraction of the "
                        + "target's current health at the time of impact.");
        n(
                b,
                "lorentz.interval",
                .5,
                .05,
                3600,
                "同一射手对同一目标进行见证判定的间隔，单位：秒。\nInterval between Witness rolls for the same "
                        + "shooter and target, in seconds.");
        n(
                b,
                "lorentz.arrowLifetime",
                60,
                1,
                3600,
                "无重力箭矢的有效飞行时间上限，单位：秒；时停期间暂停计时。\nMaximum effective flight time for "
                        + "gravity-free arrows, in seconds. The timer pauses during a time stop.");

        n(b, "dream.duration", 10, .05, 3600, "入梦的持续时间，单位：秒。\nDream duration, in seconds.");
        n(
                b,
                "dream.cooldown",
                30,
                0,
                86400,
                "入梦成功后开始计算的技能冷却时间，单位：秒。每名玩家单独计时，离线时暂停。\nSkill cooldown starting after a "
                        + "successful dream entry, in seconds. Tracked separately for each player and "
                        + "paused while offline.");
        n(
                b,
                "star.maliceChance",
                .1,
                0,
                1,
                "佩戴者受到非自身来源的攻击时，触发时停的概率；0.1表示10%。\nChance to trigger a time stop when the "
                        + "wearer is attacked by a source other than themselves; 0.1 means 10%.");
        n(
                b,
                "star.maliceInterval",
                .2,
                0,
                3600,
                "无垠之恶意每次受击判定后的隐藏冷却时间，单位：秒；无论是否触发时停，均进入此冷却。\nHidden cooldown after each "
                        + "on-hit roll for Boundless Malice, in seconds. Applies whether or not a "
                        + "time stop is triggered.");
        n(
                b,
                "star.witnessChance",
                .05,
                0,
                1,
                "佩戴者实际造成伤害后，触发时停的概率；0.05表示5%。\nChance to trigger a time stop after the "
                        + "wearer actually deals damage; 0.05 means 5%.");
        n(
                b,
                "star.maliceDuration",
                2,
                .05,
                3600,
                "无垠之恶意触发的时停持续时间，单位：秒。\nDuration of a time stop triggered by Boundless Malice, in seconds.");
        n(
                b,
                "star.witnessDuration",
                5,
                .05,
                3600,
                "无垠之见证触发的时停持续时间，单位：秒。\nDuration of a time stop triggered by Boundless Witness, in seconds.");
        n(
                b,
                "star.witnessInterval",
                1,
                0,
                3600,
                "无垠之见证每次进行时停概率判定后的冷却时间，单位：秒。无论是否触发时停，均进入此冷却。\nRoll cooldown after each "
                        + "time-stop chance roll with Boundless Witness, in seconds. Applies whether "
                        + "or not the roll triggers a time stop.");
        n(
                b,
                "star.witnessCooldown",
                5,
                0,
                86400,
                "见证时停结束后的额外判定冷却，单位：秒。\n时停发生合并时，从整个时停组结束后开始计时。离线时暂停。\nAdditional roll "
                        + "cooldown after a Witness time stop ends, in seconds.\nIf it merges with "
                        + "other time stops, the cooldown starts when the entire group ends. Paused "
                        + "while offline.");
        n(
                b,
                "star.radius",
                32,
                1,
                128,
                "以触发位置为固定中心的球形范围半径，单位：方块。\nRadius of the spherical area, in blocks. Its "
                        + "center remains fixed at the trigger location.");
        n(
                b,
                "star.groupLimit",
                15,
                .05,
                3600,
                "时停组的最长持续时间，单位：秒。\n从组内最早一次时停的开始时间算起，合并其他时停不会重新计时。\n相互合并的时停区域按同一时停组处理。\n"
                        + "Maximum duration of a time-stop group, in seconds.\nMeasured from the "
                        + "earliest start time in the group. Merging other time stops does not "
                        + "restart this timer.\nMerged time-stop areas are treated as one group.");
        PER_CHUNK =
                b.comment(
                                "每个区块最多同时覆盖的时停数量。区块只用于计数，不会扩大时停的实际球形范围。\nMaximum number of time stops that "
                                        + "may cover a chunk at once. Chunks are used only for counting; the actual "
                                        + "spherical areas are unchanged.")
                        .defineInRange("items.boundless.star.maxFieldsPerChunk", 2, 1, 16);
    }

    private static void n(
            ForgeConfigSpec.Builder b, String k, double d, double lo, double hi, String text) {
        NUMBERS.put(k, b.comment(text).defineInRange("items.boundless." + k, d, lo, hi));
    }

    public static double get(String k) {
        return NUMBERS.get(k).get();
    }

    public static int ticks(String k) {
        return TickDurations.ceilNonNegative(get(k));
    }

    private BoundlessItemsConfig() {}
}
