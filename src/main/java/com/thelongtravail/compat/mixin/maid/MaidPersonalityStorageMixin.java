package com.thelongtravail.compat.mixin.maid;

import com.thelongtravail.compat.maid.MaidPersonalityStorage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(
        targets = "cn.blockforge.touhou_little_maid_personality.emotion.MaidStorageFreeze",
        remap = false)
public abstract class MaidPersonalityStorageMixin {
    @Inject(
            method =
                    "stamp(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/nbt/CompoundTag;)V",
            at = @At("RETURN"),
            remap = false)
    private static void travail$snapshot(LivingEntity maid, CompoundTag data, CallbackInfo ci) {
        MaidPersonalityStorage.snapshot(maid, data);
    }

    @Inject(
            method =
                    "buildRequest(Lnet/minecraft/nbt/CompoundTag;J)Lnet/minecraft/nbt/CompoundTag;",
            at = @At("RETURN"),
            remap = false)
    private static void travail$request(
            CompoundTag snapshot, long time, CallbackInfoReturnable<CompoundTag> cir) {
        if (snapshot.getBoolean(MaidPersonalityStorage.MARKER))
            cir.getReturnValue().putBoolean(MaidPersonalityStorage.MARKER, true);
        if (snapshot.contains(MaidPersonalityStorage.BASE))
            cir.getReturnValue()
                    .putDouble(
                            MaidPersonalityStorage.BASE,
                            snapshot.getDouble(MaidPersonalityStorage.BASE));
    }

    @Inject(
            method =
                    "resolveRelease(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/nbt/CompoundTag;J)V",
            at = @At("HEAD"),
            remap = false)
    private static void travail$health(
            LivingEntity maid, CompoundTag request, long time, CallbackInfo ci) {
        MaidPersonalityStorage.capHealth(maid, request);
    }

    @Inject(
            method =
                    "resolveRelease(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/nbt/CompoundTag;J)V",
            at = @At("RETURN"),
            remap = false)
    private static void travail$floor(
            LivingEntity maid, CompoundTag request, long time, CallbackInfo ci) {
        MaidPersonalityStorage.rememberFloor(maid, request);
    }

    @Inject(
            method =
                    "superviseHealthFix(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/nbt/CompoundTag;)V",
            at = @At("HEAD"),
            remap = false)
    private static void travail$refresh(LivingEntity maid, CompoundTag data, CallbackInfo ci) {
        MaidPersonalityStorage.refreshFloor(maid, data);
    }
}
