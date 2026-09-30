package com.thelongtravail.client;
import com.thelongtravail.TheLongTravail;
import com.thelongtravail.config.TravailConfig;
import com.thelongtravail.data.WitnessSoundRule;
import com.thelongtravail.network.EffectNotice;
import com.thelongtravail.network.TooltipConfigSync;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;
/** Client-only adapter, loaded by client setup, not referenced by common packet classes. */
public final class EffectSounds {
    private static final EffectSoundPolicy POLICY = new EffectSoundPolicy(EffectSounds::count);
    private static final Map<String, Integer> COUNTS = new LinkedHashMap<>();
    private static Object owner, level;
    private static int diagnosticTicks;
    private static java.lang.reflect.Method playMethod;
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
    public static void notification(ResourceLocation id, boolean gain, Object configuredSound) {
        if (!Boolean.getBoolean("the_long_travail.soundsAdapterVerified") || !syncOwner() || id == null) { play(configuredSound); return; }
        // Filter at actual playback, including queue overflow. A delayed notification must use
        // current equipment and synchronized settings, not stale state captured two ticks ago.
        POLICY.notification(id.toString(), gain, () -> {
            boolean enabled = TooltipConfigSync.integer("general.muteBeneficialEffectGainSounds",
                    TravailConfig.MUTE_BENEFICIAL_EFFECT_GAIN_SOUNDS.get() ? 1 : 0) != 0;
            if (WitnessSoundRule.muteGain(Minecraft.getInstance().player,
                    ForgeRegistries.MOB_EFFECTS.getValue(id), gain, enabled)) {
                count("quiet-witness-beneficial-gain", id.toString());
                return false;
            }
            play(configuredSound);
            return true;
        });
    }
    private static void play(Object sound) {
        try {
            if (playMethod == null || !playMethod.getDeclaringClass().isInstance(sound)) playMethod = sound.getClass().getMethod("playSound");
            playMethod.invoke(sound);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Sounds playSound compatibility failed", error);
        }
    }
    private EffectSounds() {}
}
