package com.thelongtravail.abyss;

import net.minecraft.world.level.Level;

public final class PrecipitationScope implements AutoCloseable {
    private static final ThreadLocal<Level> CURRENT = new ThreadLocal<>();
    private final Level previous;

    public PrecipitationScope(Level level) {
        previous = CURRENT.get();
        CURRENT.set(level);
    }

    public static Level level() {
        return CURRENT.get();
    }

    @Override
    public void close() {
        if (previous == null) CURRENT.remove();
        else CURRENT.set(previous);
    }
}
