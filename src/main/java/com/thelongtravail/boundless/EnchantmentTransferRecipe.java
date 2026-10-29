package com.thelongtravail.boundless;

import com.thelongtravail.common.RuntimeFailures;
import com.thelongtravail.registry.ModRegistry;

import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "the_long_travail")
public final class EnchantmentTransferRecipe extends CustomRecipe {
    private record Plan(int sourceSlot, int filterSlot, EnchantmentTransfer.Result result) {}

    public EnchantmentTransferRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    // 预览、领取和返还共用一次计算，输入或注册表变化后才重试。
    private record Cached(List<ItemStack> inputs, Plan plan, EnchantmentTransfer.Failure failure) {}

    private static final Map<CraftingContainer, Cached> CACHE = new WeakHashMap<>();

    private Plan plan(CraftingContainer grid) {
        return inspect(grid).plan();
    }

    private static synchronized Cached inspect(CraftingContainer grid) {
        List<ItemStack> inputs = new ArrayList<>();
        Cached cached;
        try {
            boolean tales = false;
            for (int i = 0; i < grid.getContainerSize(); i++)
                tales |= EnchantmentTransfer.isTales(grid.getItem(i));
            if (!tales) {
                CACHE.remove(grid);
                return new Cached(List.of(), null, null);
            }
            Cached old = CACHE.get(grid);
            if (old != null && old.inputs().size() == grid.getContainerSize()) {
                boolean same = true;
                for (int i = 0; i < old.inputs().size(); i++)
                    same &= ItemStack.matches(old.inputs().get(i), grid.getItem(i));
                if (same) return old;
            }
            for (int i = 0; i < grid.getContainerSize(); i++) inputs.add(grid.getItem(i).copy());
            cached = new Cached(List.copyOf(inputs), compute(grid), null);
        } catch (TransferFailure failure) {
            cached = new Cached(List.copyOf(inputs), null, failure.detail);
        } catch (RuntimeException failure) {
            RuntimeFailures.report("enchantment recipe calculation", failure);
            cached =
                    new Cached(
                            List.copyOf(inputs),
                            null,
                            new EnchantmentTransfer.Failure(null, "recipe"));
        }
        CACHE.put(grid, cached);
        return cached;
    }

    @SubscribeEvent
    public static synchronized void reload(TagsUpdatedEvent event) {
        CACHE.clear();
    }

    public static EnchantmentTransfer.Failure failure(CraftingContainer grid) {
        return inspect(grid).failure();
    }

    private static final class TransferFailure extends RuntimeException {
        final EnchantmentTransfer.Failure detail;

        TransferFailure(EnchantmentTransfer.Failure detail) {
            this.detail = detail;
        }
    }

    private static Plan compute(CraftingContainer grid) {
        int book = -1;
        List<Integer> other = new ArrayList<>();
        for (int i = 0; i < grid.getContainerSize(); i++) {
            ItemStack stack = grid.getItem(i);
            if (stack.isEmpty()) continue;
            if (EnchantmentTransfer.isTales(stack)) {
                if (book >= 0) return null;
                book = i;
            } else other.add(i);
        }
        if (book < 0 || other.size() < 2 || other.size() > 3) return null;
        Plan found = null;
        // 枚举材料角色，使带附魔的材料物品也能作为转移目标。
        for (int catalyst : other) {
            ItemStack material = grid.getItem(catalyst);
            if (!material.is(Items.QUARTZ) && !material.is(Items.LAPIS_LAZULI)) continue;
            for (int target : other) {
                if (target == catalyst) continue;
                int filterSlot = -1;
                for (int slot : other) if (slot != catalyst && slot != target) filterSlot = slot;
                EnchantmentTransfer.Filter filter = EnchantmentTransfer.Filter.ALL;
                if (filterSlot >= 0) {
                    ItemStack extra = grid.getItem(filterSlot);
                    if (extra.is(Items.HONEY_BOTTLE)) filter = EnchantmentTransfer.Filter.NON_CURSE;
                    else if (extra.is(Items.FERMENTED_SPIDER_EYE))
                        filter = EnchantmentTransfer.Filter.CURSE;
                    else continue;
                }
                int source = material.is(Items.QUARTZ) ? target : book;
                int destination = material.is(Items.QUARTZ) ? book : target;
                var result =
                        EnchantmentTransfer.evaluate(
                                grid.getItem(source), grid.getItem(destination), filter);
                if (result.failure() != null) throw new TransferFailure(result.failure());
                if (result.result() == null) continue;
                if (found != null) return null;
                found = new Plan(source, filterSlot, result.result());
            }
        }
        return found;
    }

    @Override
    public boolean matches(CraftingContainer grid, Level level) {
        return plan(grid) != null;
    }

    @Override
    public ItemStack assemble(CraftingContainer grid, RegistryAccess access) {
        Plan plan = plan(grid);
        return plan == null ? ItemStack.EMPTY : plan.result.target().copy();
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingContainer grid) {
        var remaining = NonNullList.withSize(grid.getContainerSize(), ItemStack.EMPTY);
        Plan plan = plan(grid);
        if (plan != null) {
            remaining.set(plan.sourceSlot, plan.result.source().copy());
            if (plan.filterSlot >= 0)
                remaining.set(
                        plan.filterSlot,
                        grid.getItem(plan.filterSlot).is(Items.HONEY_BOTTLE)
                                ? new ItemStack(Items.GLASS_BOTTLE)
                                : ItemStack.EMPTY);
        }
        return remaining;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 3;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRegistry.ENCHANTMENT_TRANSFER.get();
    }
}
