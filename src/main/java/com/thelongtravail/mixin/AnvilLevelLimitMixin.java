package com.thelongtravail.mixin;

import com.thelongtravail.TravailAspect;
import com.thelongtravail.data.LongTravailData;
import com.thelongtravail.helper.TravailCurios;
import net.minecraft.world.inventory.AnvilMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(AnvilMenu.class)
public abstract class AnvilLevelLimitMixin {
    @ModifyConstant(method = "createResult", constant = @Constant(intValue = 40))
    private int theLongTravail$allowExpensiveOperations(int original) {
        var player = ((ItemCombinerMenuAccessor) (Object) this).theLongTravail$getPlayer();
        var stack = TravailCurios.stack(player);
        return !stack.isEmpty() && LongTravailData.hasWitness(stack, TravailAspect.UNDERWORLD)
                ? Integer.MAX_VALUE
                : original;
    }
}
