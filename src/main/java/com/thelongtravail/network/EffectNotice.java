package com.thelongtravail.network;
import net.minecraft.resources.ResourceLocation;
/** 仅由服务端发往客户端，报告本模组实际造成的效果变化，不是完整效果快照。 */
public record EffectNotice(ResourceLocation effect, Kind kind, long serial, long batch) {
    public enum Kind { GAIN, CLEAR, MALICE_CLEAR, EXPIRE, RESET, DIAGNOSE }
    public boolean gain() { return kind == Kind.GAIN; }
    public static java.util.function.Consumer<EffectNotice> receiver = notice -> {};
}
