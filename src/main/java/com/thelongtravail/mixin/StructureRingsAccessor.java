package com.thelongtravail.mixin;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

// 原版公开的 getRingPositionsFor 会调用 join 等待结果；此处仅在主线程检查已启动的计算是否完成。
@Mixin(ChunkGeneratorStructureState.class)
public interface StructureRingsAccessor {
    @Accessor("ringPositions")
    Map<ConcentricRingsStructurePlacement, CompletableFuture<List<ChunkPos>>>
            travail$ringPositions();
}
