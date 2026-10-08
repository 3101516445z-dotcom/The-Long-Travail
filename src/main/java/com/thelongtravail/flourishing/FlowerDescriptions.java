package com.thelongtravail.flourishing;
import com.thelongtravail.config.FlourishingItemsConfig;
import net.minecraft.network.chat.Component;
import java.math.BigDecimal;
public final class FlowerDescriptions {
    public static String number(double n) {return BigDecimal.valueOf(n).setScale(2,java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();}
    public static String percent(double n) {return number(n*100);}
    public static Component describe(Flower f) {
        String root="flowers."+f.id+".";
        String key="tooltip.the_long_travail.flower."+f.id;
        double v=FlourishingItemsConfig.shown(root+(f==Flower.OXEYE_DAISY?"healing":f==Flower.LILY_OF_THE_VALLEY||f==Flower.WITHER_ROSE?"level":"bonus"));
        return switch(f) {
            case DANDELION -> FloralText.text(key,percent(v),percent(FlourishingItemsConfig.shown(root+"saturationBonus")));
            case RED_TULIP, WHITE_TULIP -> FloralText.text(key,percent(FlourishingItemsConfig.shown(root+"healthThreshold")),percent(v));
            case OXEYE_DAISY -> FloralText.text(key,number(FlourishingItemsConfig.shown(root+"intervalSeconds")),number(v));
            case LILY_OF_THE_VALLEY, WITHER_ROSE -> FloralText.text(key,number(Math.floor(v)),number(FlourishingItemsConfig.shown(root+"durationSeconds")),number(FlourishingItemsConfig.shown(root+"cooldownSeconds")));
            case SUNFLOWER -> FloralText.text(key,percent(FlourishingItemsConfig.shown(root+"speedBonus")),percent(v));
            case ROSE_BUSH,PITCHER_PLANT -> FloralText.text(key,percent(v),number(FlourishingItemsConfig.shown(root+"cooldownSeconds")));
            default -> FloralText.text(key,percent(v));
        };
    }
}
