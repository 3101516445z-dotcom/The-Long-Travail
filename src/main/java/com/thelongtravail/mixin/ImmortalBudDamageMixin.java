package com.thelongtravail.mixin;

import com.thelongtravail.flourishing.ImmortalBud;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.extensions.IForgeItem;

import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = Item.class, remap = false)
public abstract class ImmortalBudDamageMixin implements IForgeItem {
    // Mixin 0.8.5 不支持接口内注入，改由 Item 转发接口默认实现。
    // 除受保护的损伤增加外，其余写入仍交给 Forge 处理。
    public void setDamage(ItemStack stack, int damage) {
        if (com.thelongtravail.item.ItemDurabilityProtection.protects(stack)
                && damage > stack.getDamageValue()) return;
        IForgeItem.super.setDamage(stack, damage);
    }
}
