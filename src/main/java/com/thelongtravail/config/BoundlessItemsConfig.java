package com.thelongtravail.config;

import net.minecraftforge.common.ForgeConfigSpec;
import java.util.*;

public final class BoundlessItemsConfig {
    public static final Map<String, ForgeConfigSpec.DoubleValue> NUMBERS = new LinkedHashMap<>();
    public static ForgeConfigSpec.IntValue PER_CHUNK;
    public static void define(ForgeConfigSpec.Builder b) {
        n(b,"dream.duration",10,.05,3600,"入梦持续时间，单位：秒。\nDream duration, in seconds.");
        n(b,"dream.cooldown",60,0,86400,"入梦成功后开始计算的技能冷却时间，单位：秒。每名玩家单独计时，离线时暂停。\nSkill cooldown starting after a successful dream entry, in seconds. Tracked separately for each player and paused while offline.");
        n(b,"star.maliceChance",.1,0,1,"佩戴者受到非自身来源的有来源伤害后，触发时停的概率；0.1表示10%。\nChance to trigger a time stop after the wearer takes damage from a source other than themselves; 0.1 means 10%.");
        n(b,"star.witnessChance",.05,0,1,"佩戴者实际造成伤害后，触发时停的概率；0.05表示5%。\nChance to trigger a time stop after the wearer actually deals damage; 0.05 means 5%.");
        n(b,"star.maliceDuration",2,.05,3600,"无垠之恶意触发的时停持续时间，单位：秒。\nDuration of a time stop triggered by Boundless Malice, in seconds.");
        n(b,"star.witnessDuration",5,.05,3600,"无垠之见证触发的时停持续时间，单位：秒。\nDuration of a time stop triggered by Boundless Witness, in seconds.");
        n(b,"star.witnessInterval",1,0,3600,"无垠之见证每次进行时停概率判定后的冷却时间，单位：秒。无论是否触发时停，均进入此冷却。\nRoll cooldown after each time-stop chance roll with Boundless Witness, in seconds. Applies whether or not the roll triggers a time stop.");
        n(b,"star.witnessCooldown",5,0,86400,"无垠之见证触发的时停所属时停组结束后，开始计算的额外判定冷却时间，单位：秒；离线时暂停计时。\nAdditional roll cooldown starting when the group containing the time stop triggered by Boundless Witness ends, in seconds. Paused while offline.");
        n(b,"star.radius",32,1,128,"以触发位置为固定中心的球形范围半径，单位：格。\nRadius of the spherical area, in blocks. Its center remains fixed at the trigger location.");
        n(b,"star.groupLimit",15,.05,3600,"时停组的最长持续时间，单位：秒。从组内最早一次时停的开始时间算起，合并其他时停不会重新计时。相互合并的时停区域按同一时停组处理。\nMaximum duration of a time-stop group, in seconds, measured from the earliest start time in the group. Merging other time stops does not restart this timer. Merged time-stop areas are treated as one group.");
        PER_CHUNK=b.comment("每区块最多同时覆盖的时停数量。数量统计不扩大实际球形范围。\nMaximum number of time stops covering a chunk at the same time. Counting them by chunk does not enlarge their actual spherical areas.")
                .defineInRange("items.boundless.star.maxFieldsPerChunk",2,1,16);
    }
    private static void n(ForgeConfigSpec.Builder b,String k,double d,double lo,double hi,String text){NUMBERS.put(k,b.comment(text).defineInRange("items.boundless."+k,d,lo,hi));}
    public static double get(String k){return NUMBERS.get(k).get();}
    public static int ticks(String k){return RainConfig.ticks(get(k));}
    private BoundlessItemsConfig(){}
}
