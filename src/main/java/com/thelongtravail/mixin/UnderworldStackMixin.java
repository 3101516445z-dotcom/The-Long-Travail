package com.thelongtravail.mixin;

import com.thelongtravail.underworld.UnderworldLedger;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class UnderworldStackMixin {
    @Inject(method="of",at=@At("RETURN"))
    private static void travail$restore(CompoundTag n,CallbackInfoReturnable<ItemStack> cir) {
        var server=ServerLifecycleHooks.getCurrentServer();
        if(server!=null&&server.isSameThread()&&server.overworld()!=null)UnderworldLedger.get(server).reconcile(cir.getReturnValue());
    }
}
