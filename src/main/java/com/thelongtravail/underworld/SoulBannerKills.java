package com.thelongtravail.underworld;

import com.thelongtravail.compat.dummmmmmy.DummySoulRules;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

@Mod.EventBusSubscriber(modid = "the_long_travail")
public final class SoulBannerKills {
    public record Claim(LivingEntity killer, ItemStack banner) {}

    private record Pending(
            LivingEntity victim, Claim claim, LivingDeathEvent event, boolean forced) {}

    private static final Map<LivingEntity, Pending> PENDING = new IdentityHashMap<>();
    private static final Set<LivingEntity> COUNTED = Collections.newSetFromMap(new WeakHashMap<>());

    public static Claim capture(LivingEntity victim, Entity source) {
        if (victim instanceof Player || victim instanceof ArmorStand || victim.level().isClientSide)
            return null;
        var id =
                net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(victim.getType());
        if (DummySoulRules.excludesSoulCapture(id)) return null;
        Set<Entity> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        while (source instanceof Projectile p && seen.add(source)) source = p.getOwner();
        if (!(source instanceof LivingEntity actor)
                || actor == victim
                || !(actor instanceof Player
                        || com.thelongtravail.compat.maid.MaidAccessories.isMaid(actor)))
            return null;
        ItemStack stack = UnderworldRelics.equipped(actor, true);
        return stack.isEmpty() ? null : new Claim(actor, stack);
    }

    @SubscribeEvent
    public static void death(LivingDeathEvent e) {
        Claim c = capture(e.getEntity(), e.getSource().getEntity());
        if (c != null)
            PENDING.compute(
                    e.getEntity(),
                    (victim, old) ->
                            old == null || !old.forced && old.event.isCanceled()
                                    ? new Pending(victim, c, e, false)
                                    : old);
    }

    public static void executed(LivingEntity target, Claim claim) {
        if (claim != null) PENDING.put(target, new Pending(target, claim, null, true));
    }

    @SubscribeEvent
    public static void living(LivingEvent.LivingTickEvent e) {
        if (!e.getEntity().level().isClientSide && e.getEntity().isAlive())
            COUNTED.remove(e.getEntity());
    }

    @SubscribeEvent
    public static void end(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        var batch = new ArrayList<>(PENDING.values());
        PENDING.clear();
        for (var p : batch) {
            if (!p.forced && (p.event.isCanceled() || p.victim.isAlive())) continue;
            if (!COUNTED.add(p.victim)) continue;
            var c = p.claim;
            if (UnderworldRelics.equipped(c.killer, true) != c.banner) continue;
            if (UnderworldRelics.addSoul(c.banner)) UnderworldRelics.refresh(c.killer);
        }
    }

    @SubscribeEvent
    public static void stop(net.minecraftforge.event.server.ServerStoppedEvent e) {
        PENDING.clear();
        COUNTED.clear();
    }

    private SoulBannerKills() {}
}
