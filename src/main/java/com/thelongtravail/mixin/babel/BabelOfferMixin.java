package com.thelongtravail.mixin.babel;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MerchantOffer.class)
public abstract class BabelOfferMixin {
    @Inject(method = "getCostA", at = @At("HEAD"), cancellable = true)
    private void travail$fixed(CallbackInfoReturnable<ItemStack> cir) {
        var o = (MerchantOffer) (Object) this;
        if (com.thelongtravail.farreach.babel.BabelTrades.fixed(o))
            cir.setReturnValue(o.getBaseCostA().copy());
    }
}
