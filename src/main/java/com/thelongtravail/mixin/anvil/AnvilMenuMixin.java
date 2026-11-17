package com.thelongtravail.mixin.anvil;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.thelongtravail.TravailAspect;
import com.thelongtravail.config.TravailConfig;
import com.thelongtravail.equipment.TravailCurios;
import com.thelongtravail.journey.LongTravailData;
import com.thelongtravail.network.TooltipConfigSync;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin {
    @WrapMethod(method = "createResult")
    private void theLongTravail$capCost(Operation<Void> original) {
        // 等结果计算及其 RETURN 注入全部完成，再应用见证的费用上限。
        original.call();
        if (hasUnderworldWitness()) {
            AnvilMenu self = (AnvilMenu) (Object) this;
            int cap = theLongTravail$costCap();
            if (self.getCost() > cap) self.setMaximumCost(cap);
        }
    }

    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void theLongTravail$allowFreeOperation(
            Player player, boolean hasOutput, CallbackInfoReturnable<Boolean> cir) {
        if (hasOutput && hasUnderworldWitness() && theLongTravail$costCap() == 0) {
            cir.setReturnValue(true);
        }
    }

    private int theLongTravail$costCap() {
        Player player = ((ItemCombinerMenuAccessor) (Object) this).theLongTravail$getPlayer();
        int configured = TravailConfig.UNDERWORLD_ANVIL_LEVEL_CAP.get();
        return player.level().isClientSide
                ? TooltipConfigSync.integer("underworld.anvilCap", configured)
                : configured;
    }

    private boolean hasUnderworldWitness() {
        var player = ((ItemCombinerMenuAccessor) (Object) this).theLongTravail$getPlayer();
        ItemStack stack = TravailCurios.stack(player);
        return !stack.isEmpty() && LongTravailData.hasWitness(stack, TravailAspect.UNDERWORLD);
    }
}
