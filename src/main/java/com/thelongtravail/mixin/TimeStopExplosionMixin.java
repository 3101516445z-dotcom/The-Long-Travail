package com.thelongtravail.mixin;
import com.thelongtravail.boundless.TimeStopManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Explosion.class)
public abstract class TimeStopExplosionMixin {
    @Shadow @Final private Level level;
    @Shadow @Final private it.unimi.dsi.fastutil.objects.ObjectArrayList<BlockPos> toBlow;
    @Inject(method="finalizeExplosion",at=@At("HEAD"))
    private void travail$blocks(boolean particles,CallbackInfo ci){toBlow.removeIf(p->TimeStopManager.frozen(level,p));}
}
