package com.thelongtravail.mixin;

import com.thelongtravail.TravailAspect;
import com.thelongtravail.data.LongTravailData;
import com.thelongtravail.config.TravailConfig;
import com.thelongtravail.helper.TravailCurios;
import com.thelongtravail.network.TooltipConfigSync;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin {
    @ModifyConstant(method = "createResult", constant = @Constant(intValue = 40))
    private int theLongTravail$allowExpensiveOperations(int original) {
        return hasUnderworldWitness() ? Integer.MAX_VALUE : original;
    }

    @Inject(method = "createResult", at = @At("TAIL"))
    private void theLongTravail$capCost(CallbackInfo ci) {
        if (hasUnderworldWitness()) {
            AnvilMenu self = (AnvilMenu) (Object) this;
            int cap = theLongTravail$costCap();
            if (self.getCost() > cap) self.setMaximumCost(cap);
        }
    }

    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void theLongTravail$allowFreeOperation(Player player, boolean hasOutput, CallbackInfoReturnable<Boolean> cir) {
        if (hasOutput && hasUnderworldWitness() && theLongTravail$costCap() == 0) {
            cir.setReturnValue(true);
        }
    }

    private int theLongTravail$costCap() {
        Player player = ((ItemCombinerMenuAccessor) (Object) this).theLongTravail$getPlayer();
        int configured = TravailConfig.UNDERWORLD_ANVIL_LEVEL_CAP.get();
        return player.level().isClientSide
                ? TooltipConfigSync.integer("underworld.anvilCap", configured) : configured;
    }

    private boolean hasUnderworldWitness() {
        var player = ((ItemCombinerMenuAccessor) (Object) this).theLongTravail$getPlayer();
        ItemStack stack = TravailCurios.stack(player);
        return !stack.isEmpty() && LongTravailData.hasWitness(stack, TravailAspect.UNDERWORLD);
    }
}
