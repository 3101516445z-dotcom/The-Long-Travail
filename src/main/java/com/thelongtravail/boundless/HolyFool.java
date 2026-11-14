package com.thelongtravail.boundless;

import com.thelongtravail.config.ThemeItemConfig;
import com.thelongtravail.equipment.AccessoryEquipRules;
import com.thelongtravail.network.BoundlessPacket;
import com.thelongtravail.network.TravailNetwork;
import com.thelongtravail.registry.ModRegistry;

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
        long remaining = p.getPersistentData().getLong(UNTIL) - now;
        if (remaining > 0) {
            TravailNetwork.sendSkillMessage(
                    p,
                    new BoundlessPacket.Message(
                            "message.the_long_travail.holy_fool.cooldown",
                            (int) Math.min(Integer.MAX_VALUE, remaining)));
            return;
        }
        int duration = ThemeItemConfig.ticks("holy_fool.durationSeconds");
        if (duration > 0) {
            p.addEffect(new MobEffectInstance(ModRegistry.AMONG_ALL_THINGS.get(), duration));
            // 拒收且没有既有效果时，不把这次尝试计为成功释放。
            // addEffect 返回 false 也可能只是保留了已有的更长效果。
            if (!p.hasEffect(ModRegistry.AMONG_ALL_THINGS.get())) return;
        }
        p.getPersistentData()
                .putLong(UNTIL, now + ThemeItemConfig.ticks("holy_fool.cooldownSeconds"));
        p.displayClientMessage(
                net.minecraft.network.chat.Component.translatable(
                        "message.the_long_travail.holy_fool.success"),
                true);
    }

    @SubscribeEvent
    public static void clone(PlayerEvent.Clone e) {
        if (e.getOriginal().getPersistentData().contains(UNTIL))
            e.getEntity()
                    .getPersistentData()
                    .putLong(UNTIL, e.getOriginal().getPersistentData().getLong(UNTIL));
    }

    private HolyFool() {}
}
