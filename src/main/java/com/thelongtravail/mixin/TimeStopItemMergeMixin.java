package com.thelongtravail.mixin;
import com.thelongtravail.boundless.TimeStopManager;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ItemEntity.class)
public abstract class TimeStopItemMergeMixin {
    @Inject(method="isMergable",at=@At("HEAD"),cancellable=true)
    private void travail$merge(CallbackInfoReturnable<Boolean> ci){if(TimeStopManager.frozen((ItemEntity)(Object)this))ci.setReturnValue(false);}
}
