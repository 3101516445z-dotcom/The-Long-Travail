package com.thelongtravail.mixin.babel;

import com.thelongtravail.combat.EffectCombatRules;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.warden.AngerManagement;

import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

import java.util.ArrayList;
import java.util.UUID;
import java.util.function.Predicate;

@Mixin(AngerManagement.class)
public abstract class PeacefulAngerMixin {
    @Shadow @Final protected ArrayList<Entity> suspects;
    @Shadow @Final protected it.unimi.dsi.fastutil.objects.Object2IntMap<UUID> angerByUuid;

    @Shadow
    public abstract void clearAnger(Entity entity);

    @Inject(method = "increaseAnger", at = @At("HEAD"), cancellable = true)
    private void travail$increase(Entity entity, int anger, CallbackInfoReturnable<Integer> cir) {
        if (entity instanceof LivingEntity living && EffectCombatRules.peaceful(living)) {
            clearAnger(entity);
            cir.setReturnValue(0);
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void travail$clear(ServerLevel level, Predicate<Entity> predicate, CallbackInfo ci) {
        // clearAnger 会移除 suspects 元素，因此从末尾遍历。
        for (int i = suspects.size() - 1; i >= 0; i--) {
            Entity entity = suspects.get(i);
            if (entity instanceof LivingEntity living && EffectCombatRules.peaceful(living))
                clearAnger(entity);
        }
        angerByUuid
                .keySet()
                .removeIf(
                        id ->
                                level.getEntity(id) instanceof LivingEntity living
                                        && EffectCombatRules.peaceful(living));
    }
}
