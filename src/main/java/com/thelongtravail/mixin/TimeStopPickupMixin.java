package com.thelongtravail.mixin;
import com.thelongtravail.boundless.TimeStopManager;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin({ItemEntity.class,ExperienceOrb.class})
public abstract class TimeStopPickupMixin {
    @Inject(method="playerTouch",at=@At("HEAD"),cancellable=true)
    private void travail$pickup(Player p,CallbackInfo ci){if(TimeStopManager.frozen((Entity)(Object)this))ci.cancel();}
}
