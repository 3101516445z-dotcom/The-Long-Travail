package com.thelongtravail.mixin;

import com.thelongtravail.flourishing.EverlastingFlower;
import com.thelongtravail.registry.ModRegistry;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.common.inventory.DynamicStackHandler;
import java.util.function.Function;

// 在实际写入槽位时取消旧链，涵盖同一游戏刻内卸下后重新佩戴的情况。
@Mixin(value = DynamicStackHandler.class, remap = false)
public abstract class CurioFlowerRemovalMixin extends ItemStackHandler {
    @Shadow protected Function<Integer, SlotContext> ctxBuilder;

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        boolean removed =
                getStackInSlot(slot).is(ModRegistry.EVERLASTING_FLOWER.get())
                        && !stack.is(ModRegistry.EVERLASTING_FLOWER.get());
        super.setStackInSlot(slot, stack);
        if (removed) travail$cancelFlower(slot);
    }

    @Inject(method = "extractItem", at = @At("RETURN"))
    private void travail$extracted(
            int slot, int amount, boolean simulate, CallbackInfoReturnable<ItemStack> cir) {
        if (!simulate
                && cir.getReturnValue().is(ModRegistry.EVERLASTING_FLOWER.get())
                && !getStackInSlot(slot).is(ModRegistry.EVERLASTING_FLOWER.get()))
            travail$cancelFlower(slot);
    }

    @Unique
    private void travail$cancelFlower(int slot) {
        SlotContext context = ctxBuilder.apply(slot);
        if (!context.cosmetic()) EverlastingFlower.unequipped(context.entity());
    }
}
