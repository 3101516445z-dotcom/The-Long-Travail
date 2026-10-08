package com.thelongtravail.config;
import net.minecraftforge.common.ForgeConfigSpec;

public final class TravailClientConfig {
    public enum HaloQuality {
        HIGH(8), MEDIUM(4), LOW(2), OFF(0);
        public final int samplesPerRing;
        HaloQuality(int samples) { samplesPerRing = samples; }
        public float opacity(float original) {
            if (samplesPerRing == 8) return original;
            return samplesPerRing == 0 ? 0 : 1F - (float) Math.pow(1F - original, 8F / samplesPerRing);
        }
    }
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.EnumValue<HaloQuality> NAME_HALO_QUALITY;
    public static final ForgeConfigSpec.EnumValue<HaloQuality> ASPECT_NAME_HALO_QUALITY;
    public static final ForgeConfigSpec.BooleanValue REDUCE_VISUAL_MOTION;
    public static final ForgeConfigSpec.BooleanValue RAIN_MESSAGES, DREAM_MESSAGES;
    public static final ForgeConfigSpec.IntValue TIME_STOP_BOUNDARY_LEVEL;
    static {
        var builder = new ForgeConfigSpec.Builder();
        NAME_HALO_QUALITY = builder.comment("苦旅名称的光晕质量：HIGH=高，MEDIUM=中，LOW=低，OFF=关闭。\n降低质量可减少绘制开销；关闭光晕后，仍保留名称内部的动态效果。", "Halo quality for The Long Travail's name: HIGH, MEDIUM, LOW, or OFF.\nLower quality reduces rendering cost. Disabling the halo preserves animation inside the name.")
                .defineEnum("nameEffects.nameHaloQuality", HaloQuality.HIGH);
        ASPECT_NAME_HALO_QUALITY = builder.comment("六类主题名称的光晕质量：HIGH=高，MEDIUM=中，LOW=低，OFF=关闭。\n与苦旅名称的光晕设置相互独立；关闭光晕后，仍保留名称内部的动态效果。", "Halo quality for the six theme names: HIGH, MEDIUM, LOW, or OFF.\nIndependent of The Long Travail's name halo setting. Disabling the halo preserves animation inside the names.")
                .defineEnum("nameEffects.aspectNameHaloQuality", HaloQuality.HIGH);
        REDUCE_VISUAL_MOTION = builder.comment("是否在本客户端关闭视觉剥夺的周期性强弱变化。", "Disable periodic changes in Visual Deprivation intensity on this client.").define("visualDeprivation.reduceVisualMotion", false);
        RAIN_MESSAGES = builder.comment("显示东海道技能台词。\nShow Tokaido skill messages.").define("messages.rainSkillMessages", true);
        TIME_STOP_BOUNDARY_LEVEL = builder.comment("时停边界显示档位，仅影响本客户端：1=完整半透明玻璃球面，2=线状球面。\n默认1；改用2可减少绘制开销。两种显示方式均使用亮青绿色表示豁免边界，橙红色表示危险边界。\nTime-stop boundary display mode, for this client only: 1 = continuous translucent glass sphere; 2 = wireframe sphere.\nDefault: 1. Mode 2 reduces rendering cost. Both modes use bright cyan-green for exempt boundaries and orange-red for dangerous boundaries.")
                .defineInRange("timeStop.boundaryLevel", 1, 1, 2);
        DREAM_MESSAGES = builder.comment("显示入梦技能台词。", "Show Daydream skill messages.").define("messages.dreamSkillMessages", true);
        ConfigFilesDescription.describe(builder, "client.toml");
        SPEC = builder.build();
    }
    private TravailClientConfig() {}
}
