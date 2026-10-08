package com.thelongtravail.mixin;
import com.thelongtravail.boundless.TimeStopManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ExperienceOrb.class)
public abstract class TimeStopOrbMergeMixin {
    @Inject(method="canMerge(Lnet/minecraft/world/entity/ExperienceOrb;II)Z",at=@At("HEAD"),cancellable=true)
    private static void travail$existing(ExperienceOrb orb,int id,int value,CallbackInfoReturnable<Boolean> ci){if(TimeStopManager.frozen(orb))ci.setReturnValue(false);}
    @Inject(method="tryMergeToExisting",at=@At("HEAD"),cancellable=true)
    private static void travail$new(ServerLevel level,Vec3 pos,int value,CallbackInfoReturnable<Boolean> ci){if(TimeStopManager.fields(level).stream().anyMatch(f->f.contains(pos)))ci.setReturnValue(false);}
}
