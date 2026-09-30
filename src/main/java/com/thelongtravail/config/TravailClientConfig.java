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
    public static final ForgeConfigSpec.BooleanValue REDUCE_VISUAL_MOTION;
    static {
        var builder = new ForgeConfigSpec.Builder();
        NAME_HALO_QUALITY = builder.comment("名称外围光晕质量：HIGH=16次采样，MEDIUM=8次，LOW=4次，OFF=关闭光晕；正文材质保留。仅影响本客户端。")
                .defineEnum("nameHaloQuality", HaloQuality.HIGH);
        REDUCE_VISUAL_MOTION = builder.comment("Disable visual deprivation breathing locally.").define("reduceVisualMotion", false);
        SPEC = builder.build();
    }
    private TravailClientConfig() {}
}
