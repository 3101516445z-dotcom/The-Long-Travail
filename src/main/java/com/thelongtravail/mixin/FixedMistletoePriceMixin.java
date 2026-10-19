package com.thelongtravail.mixin;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MerchantOffer.class)
public abstract class FixedMistletoePriceMixin {
    @Inject(method = "getCostA", at = @At("HEAD"), cancellable = true)
    private void travail$fixedPrice(CallbackInfoReturnable<ItemStack> cir) {
        var offer = (MerchantOffer) (Object) this;
        if (com.thelongtravail.flourishing.MistletoeTrades.fixed(offer))
            cir.setReturnValue(offer.getBaseCostA().copy());
    }
}
