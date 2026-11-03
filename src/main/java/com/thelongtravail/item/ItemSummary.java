package com.thelongtravail.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.BiConsumer;

// 仅客户端安装说明生成器；专用服务端不引用或加载客户端类。
public final class ItemSummary {
    public static BiConsumer<ItemStack, List<Component>> append = (stack, lines) -> {};

    private ItemSummary() {}
}
