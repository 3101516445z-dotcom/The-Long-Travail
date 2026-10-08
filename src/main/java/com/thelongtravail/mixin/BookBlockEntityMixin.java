package com.thelongtravail.mixin;

import com.thelongtravail.underworld.UnderworldStorage;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntity.class)
public abstract class BookBlockEntityMixin {
    @Inject(method="setLevel",at=@At("TAIL"))
    private void travail$loaded(net.minecraft.world.level.Level level,CallbackInfo ci) {
        BlockEntity be=(BlockEntity)(Object)this;
        if(!level.isClientSide&&be.getClass().getName().equals("de.maxhenkel.gravestone.tileentity.GraveStoneTileEntity"))
            UnderworldStorage.loaded(be);
    }
}
