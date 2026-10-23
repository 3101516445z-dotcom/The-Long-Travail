package com.thelongtravail.compat.mixin.maid;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.thelongtravail.item.UnderworldRelicItem;
import com.thelongtravail.compat.maid.MaidAccessoryRuntime;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.spongepowered.asm.mixin.*;

@Mixin(
        targets =
                "com.github.tartaricacid.touhoulittlemaid.inventory.container.backpack.BaubleContainer$BaubleSlot")
public abstract class MaidRelicSlotMixin extends SlotItemHandler {
    @Shadow(remap = false)
    @Final
    private EntityMaid maid;

    private MaidRelicSlotMixin(IItemHandler handler, int slot, int x, int y) {
        super(handler, slot, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        if (stack.getItem() instanceof UnderworldRelicItem) {
            // 客户端无法确认主人的日记状态，装备资格由服务端校验。
            if (!maid.level().isClientSide && MaidAccessoryRuntime.relicPhase(maid) == 0)
                return false;
            var inventory = maid.getMaidBauble();
            for (int i = 0; i < inventory.getSlots(); i++)
                if (i != getSlotIndex() && inventory.getStackInSlot(i).is(stack.getItem()))
                    return false;
        }
        return super.mayPlace(stack);
    }
}
