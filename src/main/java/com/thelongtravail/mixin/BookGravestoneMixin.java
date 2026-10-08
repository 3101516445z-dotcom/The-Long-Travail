package com.thelongtravail.mixin;

import com.thelongtravail.underworld.UnderworldStorage;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets="de.maxhenkel.gravestone.tileentity.GraveStoneTileEntity",remap=false)
public abstract class BookGravestoneMixin {
    @Inject(method="setDeath",at=@At("TAIL"),remap=false)
    private void travail$grave(@Coerce Object death,CallbackInfo ci){UnderworldStorage.grave((BlockEntity)(Object)this);}
}
