package com.thelongtravail.mixin;

import net.minecraft.world.inventory.*;
import net.minecraft.world.item.trading.Merchant;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MerchantMenu.class)
public interface BabelMerchantAccess {
    @Accessor("trader")
    Merchant travail$merchant();

    @Accessor("tradeContainer")
    MerchantContainer travail$container();
}
