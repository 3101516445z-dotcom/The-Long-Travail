package com.thelongtravail.compat.fluid;

import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

// 旧调用方的转发入口。
public final class FluidCopyGraph {
    public static boolean hasReferences(Tag node) {
        return com.thelongtravail.abyss.copy.FluidCopyGraph.hasReferences(node);
    }

    public static void reserveIds(CompoundTag data) {
        com.thelongtravail.abyss.copy.FluidCopyGraph.reserveIds(data);
    }

    public static Prepared prepare(ServerLevel level, ItemStack source) throws Exception {
        return new Prepared(com.thelongtravail.abyss.copy.FluidCopyGraph.prepare(level, source));
    }

    public record Prepared(com.thelongtravail.abyss.copy.FluidCopyGraph.Prepared delegate) {
        public ItemStack output() {
            return delegate.output();
        }

        public void commit() throws Exception {
            delegate.commit();
        }

        public void rollback() throws Exception {
            delegate.rollback();
        }
    }

    private FluidCopyGraph() {}
}
