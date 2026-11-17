package com.thelongtravail.mixin.anvil;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.thelongtravail.TravailAspect;
import com.thelongtravail.equipment.TravailCurios;
import com.thelongtravail.journey.LongTravailData;

import net.minecraft.world.inventory.AnvilMenu;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AnvilMenu.class)
public abstract class AnvilLevelLimitMixin {
    // 在已有上限表达式上叠加见证效果，保留其他修改者提供的值。
    @ModifyExpressionValue(
            method = "createResult",
            at = @At(value = "CONSTANT", args = "intValue=40"))
    private int theLongTravail$allowExpensiveOperations(int original) {
        var player = ((ItemCombinerMenuAccessor) (Object) this).theLongTravail$getPlayer();
        var stack = TravailCurios.stack(player);
        return !stack.isEmpty() && LongTravailData.hasWitness(stack, TravailAspect.UNDERWORLD)
                ? Integer.MAX_VALUE
                : original;
    }
}
