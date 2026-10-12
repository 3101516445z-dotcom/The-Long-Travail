package com.thelongtravail.mixin;

import com.thelongtravail.underworld.DroppedItemIndex;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntityLookup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// 只观察已完成的可见实体索引变化，不取消、不替换任何原版操作。
@Mixin(EntityLookup.class)
public abstract class DroppedItemIndexMixin {
    @Inject(method = "add", at = @At("RETURN"))
    private void travail$added(EntityAccess entity, CallbackInfo ci) {
        if (entity instanceof ItemEntity item) DroppedItemIndex.added(item);
    }

    @Inject(method = "remove", at = @At("RETURN"))
    private void travail$removed(EntityAccess entity, CallbackInfo ci) {
        if (entity instanceof ItemEntity item) DroppedItemIndex.removed(item);
    }
}
