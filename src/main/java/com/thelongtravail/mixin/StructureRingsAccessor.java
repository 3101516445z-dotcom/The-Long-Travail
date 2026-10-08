package com.thelongtravail.mixin;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

// 原版公开的 getRingPositionsFor 会 join；仅在主线程轮询已启动的计算。
@Mixin(ChunkGeneratorStructureState.class)
public interface StructureRingsAccessor {
    @Accessor("ringPositions")
    Map<ConcentricRingsStructurePlacement, CompletableFuture<List<ChunkPos>>> travail$ringPositions();
}
