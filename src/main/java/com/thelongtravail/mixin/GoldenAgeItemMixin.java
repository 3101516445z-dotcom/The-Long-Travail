package com.thelongtravail.mixin;
import com.thelongtravail.farreach.GoldenAgeActions;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(ItemEntity.class)
public abstract class GoldenAgeItemMixin {
    @Inject(method="tick",at=@At("TAIL"))
    private void travail$goldWater(CallbackInfo ci){GoldenAgeActions.tick((ItemEntity)(Object)this);}
    @Inject(method="isMergable",at=@At("HEAD"),cancellable=true)
    private void travail$goldMerge(CallbackInfoReturnable<Boolean> ci){if(GoldenAgeActions.pending((ItemEntity)(Object)this))ci.setReturnValue(false);}
}
