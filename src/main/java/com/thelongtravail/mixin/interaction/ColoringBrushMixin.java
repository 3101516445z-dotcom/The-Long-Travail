package com.thelongtravail.mixin.interaction;

import com.thelongtravail.item.ColoringItem;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Slice;

@Mixin(BrushableBlockEntity.class)
abstract class ColoringBrushMixin {
    @ModifyConstant(
            method = "brush",
            constant = @Constant(intValue = 1),
            slice =
                    @Slice(
                            to =
                                    @At(
                                            value = "INVOKE",
                                            target =
                                                    "Lnet/minecraft/world/level/block/entity/BrushableBlockEntity;brushingCompleted(Lnet/minecraft/world/entity/player/Player;)V")),
            require = 1,
            allow = 1)
    private int travail$coloringProgress(int increment, long time, Player player, Direction face) {
        return player.getUseItem().getItem() instanceof ColoringItem ? 4 : increment;
    }
}
