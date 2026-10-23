package com.thelongtravail.mixin;

import com.thelongtravail.farreach.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.lang.ref.WeakReference;
import java.util.Optional;

@Mixin(Brain.class)
public abstract class BabelBrainMixin implements BabelBrainOwner {
    @Unique private WeakReference<LivingEntity> travail$owner = new WeakReference<>(null);

    @Override
    public void travail$babelOwner(LivingEntity owner) {
        if (travail$owner.get() != owner) {
            travail$owner = new WeakReference<>(owner);
            BabelProtection.cleanBrain(owner, (Brain<?>) (Object) this);
        }
    }

    @Inject(method = "setMemoryInternal", at = @At("HEAD"), cancellable = true)
    private void travail$memory(
            MemoryModuleType<?> type,
            Optional<? extends ExpirableValue<?>> value,
            CallbackInfo ci) {
        if (value.isPresent()
                && BabelProtection.memoryBlocked(travail$owner.get(), type, value.get().getValue()))
            ci.cancel();
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void travail$tick(ServerLevel level, LivingEntity owner, CallbackInfo ci) {
        travail$babelOwner(owner);
        BabelProtection.cleanBrain(owner, (Brain<?>) (Object) this);
    }
}
