package com.thelongtravail.mixin;
import com.thelongtravail.boundless.TimeStopManager;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin({Mob.class,Allay.class})
public abstract class TimeStopMobPickupMixin {
    @Inject(method="pickUpItem",at=@At("HEAD"),cancellable=true)
    private void travail$pickup(ItemEntity item,CallbackInfo ci){if(TimeStopManager.frozen(item))ci.cancel();}
}
