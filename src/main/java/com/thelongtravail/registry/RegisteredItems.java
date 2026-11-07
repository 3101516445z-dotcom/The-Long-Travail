package com.thelongtravail.registry;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.RegistryObject;

// 未注册的可选物品不匹配任何物品栈，包括空栈。
public final class RegisteredItems {
    public static boolean is(ItemStack stack, RegistryObject<Item> item) {
        return item.isPresent() && stack.is(item.get());
    }

    public static boolean is(Item value, RegistryObject<Item> item) {
        return item.isPresent() && value == item.get();
    }

    private RegisteredItems() {}
}
