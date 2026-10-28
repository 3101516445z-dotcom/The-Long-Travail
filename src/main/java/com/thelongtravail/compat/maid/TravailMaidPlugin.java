package com.thelongtravail.compat.maid;

import com.github.tartaricacid.touhoulittlemaid.api.*;
import com.github.tartaricacid.touhoulittlemaid.api.bauble.IMaidBauble;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.item.bauble.BaubleManager;
import com.thelongtravail.registry.ModRegistry;

import net.minecraft.world.item.ItemStack;

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
                            return true;
                        }

                        @Override
                        public void onPutOn(EntityMaid maid, ItemStack stack) {
                            MaidAccessoryRuntime.equipmentChanged(maid, stack);
                        }

                        @Override
                        public void onTakeOff(EntityMaid maid, ItemStack stack) {
                            if (stack.is(ModRegistry.KAGOTSURUBE.get()))
                                com.thelongtravail.underworld.Kagotsurube.clear(maid);
                            if (stack.is(ModRegistry.EVERLASTING_FLOWER.get()))
                                com.thelongtravail.flourishing.EverlastingFlower.unequipped(maid);
                            MaidAccessoryRuntime.equipmentChanged(maid, stack);
                        }
                    });
    }
}
