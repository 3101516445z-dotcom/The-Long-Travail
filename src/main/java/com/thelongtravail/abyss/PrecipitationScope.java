package com.thelongtravail.abyss;

import net.minecraft.world.level.Level;

// 群系查询没有维度参数，由持有世界对象的调用方临时设置上下文，并在调用后恢复。
public final class PrecipitationScope implements AutoCloseable {
    private static final ThreadLocal<Level> CURRENT = new ThreadLocal<>();
    private final Level previous;
    public PrecipitationScope(Level level) { previous = CURRENT.get(); CURRENT.set(level); }
    public static Level level() { return CURRENT.get(); }
    @Override public void close() { if (previous == null) CURRENT.remove(); else CURRENT.set(previous); }
}
