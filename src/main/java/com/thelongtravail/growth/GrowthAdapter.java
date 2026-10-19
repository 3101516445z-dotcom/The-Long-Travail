package com.thelongtravail.growth;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

// 一次调用只推进一次原生生长机会；扩展不应在内部执行无界循环。
public interface GrowthAdapter {
    boolean supports(BlockState state);

    default boolean heavy(BlockState state) {
        return false;
    }

    default boolean ready(ServerLevel level, BlockPos pos, BlockState state) {
        return true;
    }

    void attempt(ServerLevel level, BlockPos pos, BlockState state);
}
