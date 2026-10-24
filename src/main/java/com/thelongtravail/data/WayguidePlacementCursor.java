package com.thelongtravail.data;

import com.thelongtravail.mixin.StructureRingsAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.placement.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;

// 枚举结构候选区块；只读取已完成的环形布局计算，不申请区块票据。
// 预算由搜索调度器提供，候选顺序按距离及原始顺序保持稳定。
final class WayguidePlacementCursor {
    final ServerLevel level;
    final StructurePlacement placement;
    final List<Holder<Structure>> structures;
    final WayguideGeometry.Spiral grid;
    final int baseX, baseZ, spacing;
    List<ChunkPos> rings;

    record Ring(ChunkPos pos, int order, double distance) {}

    PriorityQueue<Ring> sortedRings;
    final CompletableFuture<List<ChunkPos>> ringFuture;
    final BlockPos origin;
    int ringIndex;

    WayguidePlacementCursor(
            ServerLevel level,
            BlockPos origin,
            int radius,
            StructurePlacement placement,
            List<Holder<Structure>> structures) {
        this.level = level;
        this.placement = placement;
        this.structures = List.copyOf(structures);
        this.origin = origin;
        int cx = origin.getX() >> 4, cz = origin.getZ() >> 4;
        spacing =
                placement instanceof RandomSpreadStructurePlacement random
                        ? random.spacing()
                        : 1;
        baseX = Math.floorDiv(cx, spacing);
        baseZ = Math.floorDiv(cz, spacing);
        grid = new WayguideGeometry.Spiral((radius + 15) / 16 / spacing + 2);
        if (placement instanceof ConcentricRingsStructurePlacement concentric) {
            ringFuture =
                    ((StructureRingsAccessor) level.getChunkSource().getGeneratorState())
                            .travail$ringPositions()
                            .get(concentric);
            if (ringFuture == null) rings = List.of();
        } else ringFuture = null;
    }

    boolean ready(BooleanSupplier takeBudget) {
        if (ringFuture == null) return true;
        if (!ringFuture.isDone()) return false;
        if (rings == null) {
            rings = ringFuture.getNow(List.of());
            sortedRings =
                    new PriorityQueue<>(
                            Comparator.comparingDouble(Ring::distance)
                                    .thenComparingInt(Ring::order));
        }
        for (int i = 0; i < 32 && ringIndex < rings.size() && takeBudget.getAsBoolean(); i++) {
            var pos = rings.get(ringIndex);
            sortedRings.add(
                    new Ring(
                            pos,
                            ringIndex++,
                            pos.getMiddleBlockPosition(origin.getY()).distSqr(origin)));
        }
        return ringIndex == rings.size();
    }

    ChunkPos next() {
        if (rings != null)
            return sortedRings == null || sortedRings.isEmpty()
                    ? null
                    : sortedRings.remove().pos();
        if (grid.finished()) return null;
        int x = (baseX + grid.x()) * spacing, z = (baseZ + grid.z()) * spacing;
        grid.advance();
        return placement instanceof RandomSpreadStructurePlacement random
                ? random.getPotentialStructureChunk(level.getSeed(), x, z)
                : new ChunkPos(x, z);
    }
}
