package com.thelongtravail.compat.mixin.maid;

import com.thelongtravail.client.MaidAccessoryTooltips;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(
        targets = "com.github.tartaricacid.touhoulittlemaid.client.event.AddInformationEvent",
        remap = false)
public abstract class MaidTooltipMixin {
    @Inject(method = "onRenderTooltips", at = @At("RETURN"), remap = false)
    private static void travail$append(ItemTooltipEvent event, CallbackInfo ci) {
        MaidAccessoryTooltips.append(event.getItemStack(), event.getToolTip());
    }
}
