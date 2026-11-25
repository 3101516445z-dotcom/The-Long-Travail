package com.thelongtravail.boundless;

import com.thelongtravail.config.ThemeItemConfig;
import com.thelongtravail.equipment.AccessoryEquipRules;
import com.thelongtravail.network.TravailNetwork;
import com.thelongtravail.registry.ModRegistry;
import com.thelongtravail.runtime.effects.EffectInsertion;
import com.thelongtravail.runtime.player.PlayerCooldowns;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "the_long_travail")
public final class HolyFool {
    private static final String UNTIL = "the_long_travail:holy_fool_until";

    public static boolean equipped(Player p) {
        return AccessoryEquipRules.hasEquipped(p, "head", ModRegistry.HOLY_FOOL);
    }

    public static void cast(ServerPlayer p) {
        if (!p.isAlive()
                || p.isSpectator()
                || !equipped(p)
                || com.thelongtravail.boundless.timestop.TimeStopManager.frozen(p)) return;
        long now = p.server.overworld().getGameTime();
        long remaining = PlayerCooldowns.remainingUntil(p, UNTIL, now);
        if (remaining > 0) {
            TravailNetwork.sendWhisperCooldown(p, (int) Math.min(Integer.MAX_VALUE, remaining));
            return;
        }
        int duration = ThemeItemConfig.ticks("holy_fool.durationSeconds");
        if (duration > 0) {
            if (EffectInsertion.addOrGet(
                            p,
                            new MobEffectInstance(ModRegistry.AMONG_ALL_THINGS.get(), duration),
                            null)
                    == null) return;
        }
        PlayerCooldowns.startDeadline(
                p, UNTIL, now, ThemeItemConfig.ticks("holy_fool.cooldownSeconds"));
        p.displayClientMessage(
                net.minecraft.network.chat.Component.translatable(
                        "message.the_long_travail.holy_fool.success"),
                true);
    }

    @SubscribeEvent
    public static void clone(PlayerEvent.Clone e) {
        PlayerCooldowns.copyDeadlineIfPresent(e.getOriginal(), e.getEntity(), UNTIL);
    }

    private HolyFool() {}
}
