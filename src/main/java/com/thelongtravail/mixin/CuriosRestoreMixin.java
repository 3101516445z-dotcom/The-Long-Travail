package com.thelongtravail.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.thelongtravail.helper.AccessoryRestore;
import net.minecraft.nbt.Tag;
import org.spongepowered.asm.mixin.Mixin;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;

@Mixin(
        targets = "top.theillusivec4.curios.common.capability.CurioInventoryCapability$CurioInventoryWrapper",
        remap = false)
public abstract class CuriosRestoreMixin {
    @WrapMethod(method = "readTag", remap = false)
    private void travail$restore(Tag n, Operation<Void> original) {
        try (var scope = new AccessoryRestore(((ICuriosItemHandler) (Object) this).getWearer(), n)) {
            original.call(n);
        }
    }
}
