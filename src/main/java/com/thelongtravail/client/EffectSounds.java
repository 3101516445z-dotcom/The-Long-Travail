package com.thelongtravail.client;
import com.thelongtravail.TheLongTravail;
import com.thelongtravail.mixin.ConfiguredSoundInvoker;
import com.thelongtravail.config.TravailConfig;
import com.thelongtravail.data.WitnessSoundRule;
import com.thelongtravail.network.EffectNotice;
import com.thelongtravail.network.TooltipConfigSync;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;
// 由客户端初始化加载；通用网络包类不得引用此客户端适配器。
public final class EffectSounds {
    private static final EffectSoundPolicy POLICY = new EffectSoundPolicy(EffectSounds::count);
    private static final Map<String, Integer> COUNTS = new LinkedHashMap<>();
    private static Object owner, level;
    private static int diagnosticTicks;
    public static void clear() { POLICY.clear(); COUNTS.clear(); diagnosticTicks = 0; owner = null; level = null; }
    private static boolean syncOwner() {
        var client = Minecraft.getInstance();
        if (owner != client.player || level != client.level) { clear(); owner = client.player; level = client.level; }
        return owner != null;
    }
    public static void receive(EffectNotice notice) {
        if (!syncOwner()) return;
        switch (notice.kind()) {
            case RESET -> { POLICY.clear(); COUNTS.clear(); diagnosticTicks = 0; }
            case DIAGNOSE -> { COUNTS.clear(); diagnosticTicks = 600; }
            default -> POLICY.mark(notice.effect().toString(), EffectSoundPolicy.Reason.valueOf(notice.kind().name()), notice.serial(), notice.batch());
        }
    }
    public static void tick() {
        if (!syncOwner()) return;
        POLICY.advance();
        if (diagnosticTicks > 0 && --diagnosticTicks % 100 == 0) {
            TheLongTravail.LOGGER.info("Travail effect diagnostic [client]: {}", COUNTS); COUNTS.clear();
        }
    }
    private static void count(String reason, String id) {
        if (diagnosticTicks <= 0) return;
        String key = reason + ":" + id;
        if (COUNTS.size() >= 256 && !COUNTS.containsKey(key)) key = "other";
        COUNTS.merge(key, 1, Integer::sum);
    }
    public static void notification(ResourceLocation id, boolean gain, ConfiguredSoundInvoker configuredSound) {
        if (!Boolean.getBoolean("the_long_travail.soundsAdapterVerified") || !syncOwner() || id == null) { configuredSound.travail$playSound(); return; }
        // 在实际播放时过滤，包括队列溢出路径。延迟通知须读取当前装备和同步配置，
        // 不能使用两刻前捕获的过期状态。
        POLICY.notification(id.toString(), gain, () -> {
            boolean enabled = TooltipConfigSync.integer("general.muteBeneficialEffectGainSounds",
                    TravailConfig.MUTE_BENEFICIAL_EFFECT_GAIN_SOUNDS.get() ? 1 : 0) != 0;
            if (WitnessSoundRule.muteGain(Minecraft.getInstance().player,
                    ForgeRegistries.MOB_EFFECTS.getValue(id), gain, enabled)) {
                count("quiet-witness-beneficial-gain", id.toString());
                return false;
            }
            configuredSound.travail$playSound();
            return true;
        });
    }
    private EffectSounds() {}
}
