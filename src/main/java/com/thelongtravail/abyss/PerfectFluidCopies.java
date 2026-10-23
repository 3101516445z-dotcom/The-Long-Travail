package com.thelongtravail.abyss;

import com.thelongtravail.compat.fluid.FluidCopyGraph;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;

public final class PerfectFluidCopies {
    private PerfectFluidCopies() {}

    public static boolean isProtected(String id) {
        return id.equals("the_long_travail:perfect_fluid")
                || id.equals("the_long_travail:the_long_travail")
                || blacklist().contains(id);
    }

    private static java.util.List<? extends String> blacklist() {
        var configured = com.thelongtravail.config.AbyssItemsConfig.BLACKLIST.get();
        return net.minecraftforge.fml.util.thread.EffectiveSide.get().isClient()
                ? com.thelongtravail.network.TooltipConfigSync.pool(
                        "abyss.fluidBlacklist", configured)
                : configured;
    }

    public static ItemStack copy(ItemStack source) {
        CompoundTag data = source.save(new CompoundTag());
        FluidCopyGraph.reserveIds(data);
        // 只复制一件；避免模组提供的大堆叠数量在原版字节 Count 中截断为零。
        data.putByte("Count", (byte) 1);
        return ItemStack.of(data);
    }

    public static boolean returnsOriginal(ItemStack source) {
        if (!source.isStackable()) return true;
        CompoundTag data = source.save(new CompoundTag());
        return FluidCopyGraph.hasReferences(data) || clearFluid(data);
    }

    public static boolean clearFluid(Tag node) {
        boolean changed = false;
        if (node instanceof CompoundTag compound) {
            if (isProtected(compound.getString("id"))
                    && compound.contains("Count", Tag.TAG_ANY_NUMERIC)) {
                // 只清空内容，不删除列表元素，以免改变依赖固定索引的容器槽位。
                for (String key : new ArrayList<>(compound.getAllKeys())) {
                    if (!key.equals("Slot")) compound.remove(key);
                }
                compound.putString("id", "minecraft:air");
                compound.putByte("Count", (byte) 0);
                return true;
            }
            for (String key : compound.getAllKeys()) changed |= clearFluid(compound.get(key));
        } else if (node instanceof ListTag list) {
            for (Tag child : list) changed |= clearFluid(child);
        }
        return changed;
    }
}
