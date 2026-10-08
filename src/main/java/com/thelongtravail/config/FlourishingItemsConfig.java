package com.thelongtravail.config;

import com.thelongtravail.flourishing.Flower;
import com.thelongtravail.network.TooltipConfigSync;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraft.resources.ResourceLocation;
import java.util.*;

public final class FlourishingItemsConfig {
    public static final Map<String, ForgeConfigSpec.DoubleValue> NUMBERS = new LinkedHashMap<>();
    public static final Map<Flower, ForgeConfigSpec.BooleanValue> ENABLED = new EnumMap<>(Flower.class);
    public static ForgeConfigSpec.BooleanValue SHARING;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> SPRING_SLOTS, AFFECTION_SLOTS, BLACKLIST, EXCLUSIONS;
    public static void define(ForgeConfigSpec.Builder b) {
        for (Flower f : Flower.values()) {
            String p = "items.spring_game.flowers." + f.id;
            ENABLED.put(f, b.comment("是否启用此花朵。关闭后，无法使用此花朵制作饰品或将其添加到饰品中；已有饰品中的此花朵会保留，但效果暂停生效。",
                    "Enable this flower. When disabled, it cannot be used to craft an accessory or added to one. Flowers already in accessories are retained, but their effects are inactive.").define(p + ".enabled", true));
            String value = switch(f) {
                case OXEYE_DAISY -> "healing";
                case LILY_OF_THE_VALLEY, WITHER_ROSE -> "level";
                default -> "bonus";
            };
            number(b, "flowers." + f.id + "." + value, p + "." + value, f.initial,
                    value.equals("level") ? 1 : 0, value.equals("level") ? 256 : reduction(f) ? 1 : 1000,
                    value.equals("level") ? "效果等级；1表示Ⅰ级，小数向下取整。\nEffect level; 1 means level I. Fractional values are rounded down." : value.equals("healing") ? "每次恢复的生命值；2点为1颗心。\nHealth restored each time; 2 points equal 1 heart." : primaryComment(f));
        }
        extra(b, "dandelion", "saturationBonus", .2, 0, 1000, "食物提供的饱和度增加比例。\nFractional increase in saturation restored by food.");
        extra(b, "red_tulip", "healthThreshold", .5, 0, 1, "佩戴者当前生命值占最大生命值的比例低于此值时生效。\nApplies while the wearer's current health as a fraction of maximum health is below this value.");
        extra(b, "white_tulip", "healthThreshold", .5, 0, 1, "佩戴者当前生命值占最大生命值的比例低于此值时生效。\nApplies while the wearer's current health as a fraction of maximum health is below this value.");
        extra(b, "oxeye_daisy", "intervalSeconds", 5, .05, 1000000, "生命值恢复间隔，单位：秒；恢复不受饱食度限制。\nInterval between health restorations, in seconds; restoration is not restricted by hunger level.");
        for (String id : List.of("lily_of_the_valley", "wither_rose")) {
            extra(b,id,"durationSeconds",4,.05,1000000,"效果持续时间，单位：秒。\nEffect duration, in seconds.");
            extra(b,id,"cooldownSeconds",8,0,1000000,"同一玩家对同一目标的触发冷却，单位：秒；0表示无冷却。\nTrigger cooldown for the same player against the same target, in seconds; 0 means no cooldown.");
        }
        extra(b,"sunflower","speedBonus",.1,0,1000,"移动速度增加比例；固定要求白天且露天。\nFractional movement speed increase; always requires daytime and open sky.");
        extra(b,"rose_bush","cooldownSeconds",1,0,1000000,"反伤的触发冷却时间，单位：秒。同一佩戴者对不同攻击者触发反伤时共用此冷却，不同佩戴者分别计时；0表示无冷却。\nRetaliation trigger cooldown, in seconds. The same wearer shares this cooldown across different attackers; different wearers have separate timers. 0 means no cooldown.");
        extra(b,"pitcher_plant","cooldownSeconds",3,0,1000000,"击杀恢复的冷却，单位：秒；0表示无冷却。\nCooldown for healing from kills, in seconds; 0 means no cooldown.");
        affinity(b,"servantDamageBonus",1.5,0,1000,"绑定的伙伴生物可获得的最大伤害加成比例；实际加成随佩戴者已损失生命值占最大生命值的比例线性提高。\nMaximum fractional damage bonus for the bound companion. The actual bonus scales linearly with the fraction of the wearer's maximum health that is missing.");
        affinity(b,"servantDamageReduction",.7,0,1,"绑定的伙伴生物可获得的最大伤害减免比例；实际减免随佩戴者已损失生命值占最大生命值的比例线性提高。\nMaximum fractional damage reduction for the bound companion. The actual reduction scales linearly with the fraction of the wearer's maximum health that is missing.");
        affinity(b,"playerDamageBonus",.5,0,1000,"佩戴者可获得的最大伤害加成比例；实际加成随绑定的伙伴生物已损失生命值占最大生命值的比例线性提高。\nMaximum fractional damage bonus for the wearer. The actual bonus scales linearly with the fraction of the bound companion's maximum health that is missing.");
        affinity(b,"playerSpeedBonus",.75,0,1000,"佩戴者可获得的最大移动速度加成比例；实际加成随绑定的伙伴生物已损失生命值占最大生命值的比例线性提高。\nMaximum fractional movement speed bonus for the wearer. The actual bonus scales linearly with the fraction of the bound companion's maximum health that is missing.");
        affinity(b,"range",32,0,1024,"生效距离，单位：方块；双方必须同维度。\nEffective distance, in blocks; both must be in the same dimension.");
        affinity(b,"shareFraction",.5,0,1,"佩戴者受到的伤害中，交由绑定的伙伴生物分担的比例；在计算佩戴者的防御减免前分出。\nFraction of incoming damage transferred from the wearer to the bound companion before the wearer's defensive reductions are calculated.");
        affinity(b,"servantShareMultiplier",.5,0,1,"伙伴生物分担的伤害所乘的倍率；0.5表示将分担伤害减半，再计算伙伴生物自身的防御减免。\nMultiplier applied to the companion's share of damage; 0.5 halves the shared damage before the companion's own defensive reductions are calculated.");
        SHARING=b.comment("是否启用伤害分担；关闭后仍提供双方加成。", "Enable damage sharing; bonuses for both parties remain active when disabled.").define("items.affection.sharing",true);
        SPRING_SLOTS=slots(b,"spring_game","head"); AFFECTION_SLOTS=slots(b,"affection","body");
        BLACKLIST=ids(b,"items.affection.entityBlacklist",List.of(),"禁止绑定的实体类型ID。只使用原版所有权接口识别从属。\nEntity type IDs that cannot be bound. Companions are identified using only vanilla ownership interfaces.");
        EXCLUSIONS=ids(b,"items.affection.sharingExclusions",List.of("minecraft:generic_kill","minecraft:out_of_world","the_long_travail:fluid_erosion","the_long_travail:rose_retaliation"),"不参与伤害分担的伤害类型ID；直接处死始终排除。\nDamage type IDs excluded from damage sharing; direct killing is always excluded.");
    }
    private static boolean reduction(Flower f) { return f==Flower.ALLIUM||f==Flower.AZURE_BLUET||f==Flower.WHITE_TULIP||f==Flower.TORCHFLOWER; }
    private static String primaryComment(Flower f) {
        String meaning=switch(f) {
            case DANDELION -> "食物恢复的饱食度提高的比例；不足1点的部分会保留，并在之后进食时累计计算。\nFractional increase in hunger restored by food. Fractions below 1 point are retained and accumulated across later meals.";
            case POPPY,RED_TULIP,SUNFLOWER -> "造成的伤害提高的比例。\nFractional increase in damage dealt.";
            case BLUE_ORCHID -> "水中移动速度的增加比例。\nFractional increase in swimming speed.";
            case ALLIUM -> "中毒伤害的减免比例；不影响其他魔法伤害。\nFractional reduction in Poison damage; does not affect other magic damage.";
            case AZURE_BLUET -> "摔落伤害的减免比例。\nFractional reduction in fall damage.";
            case ORANGE_TULIP -> "攻击速度的增加比例。\nFractional increase in attack speed.";
            case WHITE_TULIP -> "受到的伤害降低的比例。\nFractional reduction in damage taken.";
            case PINK_TULIP -> "佩戴者受到的治疗量提高的比例。\nFractional increase in healing received by the wearer.";
            case CORNFLOWER -> "造成的投射物伤害增加比例。\nFractional increase in projectile damage dealt.";
            case TORCHFLOWER -> "火焰、燃烧和熔岩伤害的减免比例。\nFractional reduction in fire, burning, and lava damage.";
            case LILAC -> "施加的中毒、凋零、缓慢和虚弱效果持续时间延长的比例。\nFractional increase in the duration of Poison, Wither, Slowness, and Weakness effects applied.";
            case ROSE_BUSH -> "反伤占玩家当前最大生命值的比例；真实伤害，不受增伤影响。\nRetaliation damage as a fraction of the player's current maximum health. Deals true damage and is unaffected by damage bonuses.";
            case PEONY -> "最大生命值的增加比例。\nFractional increase in maximum health.";
            case PITCHER_PLANT -> "每次击杀敌对生物时，恢复量占佩戴者最大生命值的比例。\nHealth restored per hostile mob killed, as a fraction of the wearer's maximum health.";
            case PINK_PETALS -> "移动速度的增加比例。\nFractional increase in movement speed.";
            default -> "效果比例。\nEffect fraction.";
        };
        return meaning.replace("\n", "0.1表示10%，0表示不提供此项效果。\n") + " 0.1 means 10%; 0 disables this effect.";
    }
    private static void number(ForgeConfigSpec.Builder b,String key,String path,double v,double min,double max,String comment) {
        NUMBERS.put(key,b.comment(comment).defineInRange(path,v,min,max));
    }
    private static void extra(ForgeConfigSpec.Builder b,String id,String key,double v,double min,double max,String comment) { number(b,"flowers."+id+"."+key,"items.spring_game.flowers."+id+"."+key,v,min,max,comment); }
    private static void affinity(ForgeConfigSpec.Builder b,String key,double v,double min,double max,String comment) { number(b,"affection."+key,"items.affection."+key,v,min,max,comment); }
    private static ForgeConfigSpec.ConfigValue<List<? extends String>> slots(ForgeConfigSpec.Builder b,String id,String slot) { return b.comment("饰品可生效的Curios槽位。使用自定义槽位时，还需配置允许该饰品装备到该槽位的物品标签。", "Curios slots in which the accessory can take effect. Custom slots also require item tags allowing the accessory to be equipped in those slots.").defineListAllowEmpty("items."+id+".slots",List.of(slot),v->v instanceof String s&&s.matches("[a-z0-9_/-]{1,128}")); }
    private static ForgeConfigSpec.ConfigValue<List<? extends String>> ids(ForgeConfigSpec.Builder b,String key,List<String> values,String comment) {return b.comment(comment).defineListAllowEmpty(key,values,v->v instanceof String s&&ResourceLocation.tryParse(s)!=null);}
    public static double get(String key) { return NUMBERS.get(key).get(); }
    public static double value(Flower f) { return get("flowers."+f.id+"."+(f==Flower.OXEYE_DAISY?"healing":f==Flower.LILY_OF_THE_VALLEY||f==Flower.WITHER_ROSE?"level":"bonus")); }
    public static double extra(Flower f,String key) {return get("flowers."+f.id+"."+key);}
    public static double shown(String key) {return TooltipConfigSync.decimal("floral."+key,get(key));}
    public static boolean shownEnabled(Flower f) {return TooltipConfigSync.decimal("floral.enabled."+f.id,ENABLED.get(f).get()?1:0)>0;}
    public static int ticks(double seconds) {return (int)Math.min(Integer.MAX_VALUE,Math.ceil(seconds*20));}
    private FlourishingItemsConfig() {}
}
