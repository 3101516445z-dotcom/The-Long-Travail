package com.thelongtravail.boundless;

import net.minecraft.world.Container;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class FrozenContainers {
    // 方块工作台菜单通常持有 ContainerLevelAccess 而非 BlockEntity 容器。
    private static final ClassValue<java.util.List<java.lang.reflect.Field>> SOURCES = new ClassValue<>() {
        @Override protected java.util.List<java.lang.reflect.Field> computeValue(Class<?> type) {
            var out = new java.util.ArrayList<java.lang.reflect.Field>();
            for (Class<?> c = type; c != null && AbstractContainerMenu.class.isAssignableFrom(c); c = c.getSuperclass())
                for (var f : c.getDeclaredFields()) if (!java.lang.reflect.Modifier.isStatic(f.getModifiers())
                        && f.getType() == net.minecraft.world.inventory.ContainerLevelAccess.class) {
                    if (f.trySetAccessible()) out.add(f);
                }
            return java.util.List.copyOf(out);
        }
    };
    public static boolean frozen(Container c) {
        if (c instanceof BlockEntity b) return b.getLevel() != null && TimeStopManager.frozen(b.getLevel(), b.getBlockPos());
        if (c instanceof Entity e) return TimeStopManager.frozen(e);
        if (c instanceof CompoundContainer d) {
            var a = (com.thelongtravail.mixin.TimeStopCompoundAccessor)(Object)d;
            return frozen(a.travail$first()) || frozen(a.travail$second());
        }
        return false;
    }
    public static boolean frozen(AbstractContainerMenu menu) {
        for (var slot : menu.slots) if (frozen(slot.container)) return true;
        for (var field : SOURCES.get(menu.getClass())) try {
            var access = (net.minecraft.world.inventory.ContainerLevelAccess)field.get(menu);
            if (access != null && access.evaluate(TimeStopManager::frozen, false)) return true;
        } catch (IllegalAccessException error) { throw new IllegalStateException("Cannot inspect menu origin", error); }
        return false;
    }
    private FrozenContainers() {}
}
