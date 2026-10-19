package com.thelongtravail.mixin;

import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractVillager.class)
public abstract class MistletoeTradeMixin {
    @Inject(method = "getOffers", at = @At("RETURN"))
    private void travail$offers(CallbackInfoReturnable<MerchantOffers> cir) {
        if ((Object) this instanceof Villager villager) {
            com.thelongtravail.flourishing.MistletoeTrades.ensure(villager, cir.getReturnValue());
            com.thelongtravail.farreach.BabelTrades.ensure(villager, cir.getReturnValue());
            com.thelongtravail.boundless.BoundlessAcquisition.ensure(villager, cir.getReturnValue());
        }
    }
}
