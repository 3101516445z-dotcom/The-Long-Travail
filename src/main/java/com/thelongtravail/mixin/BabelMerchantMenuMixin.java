package com.thelongtravail.mixin;

import com.thelongtravail.farreach.BabelTrades;
import net.minecraft.world.inventory.*;
import net.minecraft.world.entity.npc.*;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public abstract class BabelMerchantMenuMixin {
    @Inject(method = "clicked", at = @At("HEAD"))
    private void travail$validate(int slot, int button, ClickType type, Player p, CallbackInfo ci) {
        if (p.level().isClientSide) return;
        if ((Object) this instanceof MerchantMenu menu
                && menu instanceof BabelMerchantAccess access
                && access.travail$merchant() instanceof Villager v) {
            boolean changed = BabelTrades.refresh(v, p);
            access.travail$container().updateSellItem();
            if (changed) BabelTrades.sync(v);
        }
    }
}
