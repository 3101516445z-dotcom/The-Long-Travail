package com.thelongtravail.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.thelongtravail.valley.ExecutionLoot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class ExecutionDropSourceMixin {
    @WrapOperation(
            method =
                    "spawnAtLocation(Lnet/minecraft/world/item/ItemStack;F)Lnet/minecraft/world/entity/item/ItemEntity;",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean travail$sourceBeforeAdmission(
            Level level, Entity item, Operation<Boolean> original) {
        if (item instanceof ItemEntity drop) ExecutionLoot.source((Entity) (Object) this, drop);
        return original.call(level, item);
    }

    // Forge 的 captureDrops 会绕过实体加入世界的流程，须保留来源直到捕获列表释放。
    @Inject(
            method =
                    "spawnAtLocation(Lnet/minecraft/world/item/ItemStack;F)Lnet/minecraft/world/entity/item/ItemEntity;",
            at = @At("RETURN"))
    private void travail$capturedSource(
            ItemStack stack, float offset, CallbackInfoReturnable<ItemEntity> cir) {
        ExecutionLoot.source((Entity) (Object) this, cir.getReturnValue());
    }
}
