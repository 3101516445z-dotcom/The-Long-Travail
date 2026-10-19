package com.thelongtravail.mixin;

import com.thelongtravail.client.ThemeTooltips;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.List;

@Mixin(ItemStack.class)
public abstract class ThemeTooltipMixin {
    @Inject(method = "getTooltipLines", at = @At("RETURN"))
    private void travail$summary(Player viewer, TooltipFlag flag, CallbackInfoReturnable<List<Component>> cir) {
        ThemeTooltips.finish((ItemStack) (Object) this, viewer, cir.getReturnValue());
    }
}
