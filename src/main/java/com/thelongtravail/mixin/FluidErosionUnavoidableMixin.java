package com.thelongtravail.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.thelongtravail.helper.CombatContext;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.ForgeHooks;
import org.spongepowered.asm.mixin.Mixin;

/** Rules surround event dispatch; they do not depend on event-listener registration order. */
@Mixin(value = ForgeHooks.class, remap = false)
public abstract class FluidErosionUnavoidableMixin {
    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(
            method = {"onLivingHurt", "onLivingDamage"}, at = @org.spongepowered.asm.mixin.injection.At(value = "INVOKE",
            target = "Lnet/minecraftforge/eventbus/api/IEventBus;post(Lnet/minecraftforge/eventbus/api/Event;)Z"), remap = false)
    private static boolean travail$observeCancellation(net.minecraftforge.eventbus.api.IEventBus bus,
            net.minecraftforge.eventbus.api.Event event, Operation<Boolean> original) {
        boolean cancelled = original.call(bus, event);
        CombatContext.observeCancellation(event, cancelled);
        return cancelled;
    }
    @WrapMethod(method = "onPlayerAttack", remap = false)
    private static boolean travail$playerAttack(LivingEntity entity, DamageSource source, float amount,
                                                Operation<Boolean> original) {
        return travail$attack(entity, source, amount, original);
    }

    @WrapMethod(method = "onLivingAttack", remap = false)
    private static boolean travail$livingAttack(LivingEntity entity, DamageSource source, float amount,
                                                Operation<Boolean> original) {
        // Player.hurt has already dispatched its attack; LivingEntity.hurt delegates through here.
        if (entity instanceof Player) return original.call(entity, source, amount);
        return travail$attack(entity, source, amount, original);
    }

    private static boolean travail$attack(LivingEntity entity, DamageSource source, float amount,
                                          Operation<Boolean> original) {
        CombatContext context = CombatContext.current(entity, source);
        if (context == null) return original.call(entity, source, amount);
        if (!context.prepareAttack(amount)) return false;
        boolean accepted = original.call(entity, source, amount);
        return accepted || context.overridesCancellation();
    }

    @WrapMethod(method = "onLivingHurt", remap = false)
    private static float travail$hurt(LivingEntity entity, DamageSource source, float amount,
                                      Operation<Float> original) {
        CombatContext context = CombatContext.current(entity, source);
        if (context == null) return original.call(entity, source, amount);
        if (context.blocked()) return 0;
        return context.finishHurt(original.call(entity, source, context.prepareHurt(amount)));
    }

    @WrapMethod(method = "onLivingDamage", remap = false)
    private static float travail$damage(LivingEntity entity, DamageSource source, float amount,
                                        Operation<Float> original) {
        CombatContext context = CombatContext.current(entity, source);
        if (context == null) return original.call(entity, source, amount);
        if (context.blocked()) return 0;
        return context.finishDamage(original.call(entity, source, amount));
    }
}
