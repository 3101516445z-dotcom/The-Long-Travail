package com.thelongtravail.mixin.flourishing;

import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractVillager.class)
public abstract class MistletoeTradeMixin {
    @Inject(method = "readAdditionalSaveData", at = @At("HEAD"))
    private void travail$registeredOffers(
            net.minecraft.nbt.CompoundTag tag,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (!com.thelongtravail.config.ItemRegistrationConfig.anyDisabled()
                || !tag.contains("Offers", 10)) return;
        var original = tag.getCompound("Offers");
        var recipes = original.getList("Recipes", 10);
        net.minecraft.nbt.CompoundTag filtered = null;
        for (int i = recipes.size() - 1; i >= 0; i--) {
            var offer = recipes.getCompound(i);
            if (travail$disabledStack(offer, "buy")
                    || travail$disabledStack(offer, "buyB")
                    || travail$disabledStack(offer, "sell")) {
                if (filtered == null) filtered = original.copy();
                filtered.getList("Recipes", 10).remove(i);
            }
        }
        if (filtered != null) tag.put("Offers", filtered);
    }

    private static boolean travail$disabledStack(net.minecraft.nbt.CompoundTag offer, String key) {
        return com.thelongtravail.config.ItemRegistrationConfig.disabled(
                net.minecraft.resources.ResourceLocation.tryParse(
                        offer.getCompound(key).getString("id")));
    }

    @Inject(method = "getOffers", at = @At("RETURN"))
    private void travail$offers(CallbackInfoReturnable<MerchantOffers> cir) {
        if ((Object) this instanceof Villager villager) {
            com.thelongtravail.flourishing.MistletoeTrades.ensure(villager, cir.getReturnValue());
            com.thelongtravail.farreach.babel.BabelTrades.ensure(villager, cir.getReturnValue());
            com.thelongtravail.boundless.BoundlessAcquisition.ensure(
                    villager, cir.getReturnValue());
            com.thelongtravail.loot.ArchaeologyTrades.ensure(villager, cir.getReturnValue());
        }
    }
}
