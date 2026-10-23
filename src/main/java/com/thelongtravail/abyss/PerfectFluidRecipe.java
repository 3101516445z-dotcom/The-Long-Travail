package com.thelongtravail.abyss;

import com.thelongtravail.config.AbyssItemsConfig;
import com.thelongtravail.compat.fluid.FluidCopyGraph;
import com.thelongtravail.compat.fluid.FluidCrafting;
import net.minecraft.nbt.CompoundTag;
import com.thelongtravail.network.TooltipConfigSync;
import com.thelongtravail.registry.ModRegistry;
import net.minecraft.core.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

public final class PerfectFluidRecipe extends CustomRecipe {
    private record Input(ItemStack target, int slot) {}

    public PerfectFluidRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    private Input input(CraftingContainer c) {
        int first = -1, second = -1;
        for (int i = 0; i < c.getContainerSize(); i++) {
            if (c.getItem(i).isEmpty()) continue;
            if (first < 0) first = i;
            else if (second < 0) second = i;
            else return null;
        }
        if (second < 0) return null;
        int target;
        if (c.getItem(first).is(ModRegistry.PERFECT_FLUID.get())) target = second;
        else if (c.getItem(second).is(ModRegistry.PERFECT_FLUID.get())) target = first;
        else return null;
        if (PerfectFluidCopies.isProtected(
                ForgeRegistries.ITEMS.getKey(c.getItem(target).getItem()).toString())) return null;
        return new Input(c.getItem(target), target);
    }

    private static boolean supported(CraftingContainer c, ItemStack target) {
        return !FluidCopyGraph.hasReferences(target.save(new CompoundTag()))
                || FluidCrafting.supported(c);
    }

    @Override
    public boolean matches(CraftingContainer c, Level level) {
        try {
            var in = input(c);
            if (in == null || !supported(c, in.target)) return false;
            String id = ForgeRegistries.ITEMS.getKey(in.target.getItem()).toString();
            return !(level.isClientSide
                            ? TooltipConfigSync.pool(
                                    "abyss.fluidBlacklist", AbyssItemsConfig.BLACKLIST.get())
                            : AbyssItemsConfig.BLACKLIST.get())
                    .contains(id);
        } catch (RuntimeException failure) {
            com.thelongtravail.compat.fluid.FluidCopyFailures.report("matching", failure);
            return false;
        }
    }

    @Override
    public ItemStack assemble(CraftingContainer c, RegistryAccess access) {
        try {
            var in = input(c);
            if (in == null || !supported(c, in.target)) return ItemStack.EMPTY;
            var out = PerfectFluidCopies.copy(in.target);
            out.setCount(PerfectFluidCopies.returnsOriginal(in.target) ? 1 : 2);
            return out;
        } catch (RuntimeException unsupported) {
            // 预览超出限制或遇到不支持的容器时，须取消复制，避免合成界面崩溃或生成不完整的副本。
            com.thelongtravail.compat.fluid.FluidCopyFailures.report("preview", unsupported);
            return ItemStack.EMPTY;
        }
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingContainer c) {
        var out = NonNullList.withSize(c.getContainerSize(), ItemStack.EMPTY);
        var in = input(c);
        if (in != null && PerfectFluidCopies.returnsOriginal(in.target)) {
            var original = in.target.copy();
            original.setCount(1);
            out.set(in.slot, original);
        }
        return out;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRegistry.PERFECT_FLUID_COPY.get();
    }
}
