package com.thelongtravail.compat.maid;

import com.github.tartaricacid.touhoulittlemaid.api.*;
import com.github.tartaricacid.touhoulittlemaid.api.bauble.IMaidBauble;
import com.github.tartaricacid.touhoulittlemaid.item.bauble.BaubleManager;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.world.item.ItemStack;
import com.thelongtravail.registry.ModRegistry;

@LittleMaidExtension
public final class TravailMaidPlugin implements ILittleMaid {
    @Override
    public void bindMaidBauble(BaubleManager manager) {
        for (var item : MaidAccessoryRuntime.items())
            manager.bind(
                    item,
                    new IMaidBauble() {
                        @Override
                        public boolean syncClient(EntityMaid maid, ItemStack stack) {
                            return stack.is(ModRegistry.SWORD_AND_LANTERN.get());
                        }

                        @Override
                        public void onPutOn(EntityMaid maid, ItemStack stack) {
                            MaidAccessoryRuntime.equipmentChanged(maid, stack);
                        }

                        @Override
                        public void onTakeOff(EntityMaid maid, ItemStack stack) {
                            MaidAccessoryRuntime.equipmentChanged(maid, stack);
                        }
                    });
    }
}
