package travail.smoke;

import com.thelongtravail.data.JourneyQueries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.structures.DesertPyramidPiece;
import it.unimi.dsi.fastutil.longs.*;
import java.util.*;

// 用跨区块的两个分离部件，逐点对照原版查询；结束后恢复世界结构元数据。
public final class JourneyQuerySmoke {
    public static void run(ServerLevel level) throws Exception {
        var registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        Map<Structure, ResourceLocation> targets = new LinkedHashMap<>();
        for (String name : List.of("desert_pyramid", "village_plains", "stronghold")) {
            var id = new ResourceLocation("minecraft", name); targets.put(registry.get(id), id);
        }
        var method = JourneyQueries.class.getDeclaredMethod("structuresAt", ServerLevel.class, BlockPos.class, Map.class);
        method.setAccessible(true);
        // 先完成生成，防止后续区块阶段覆盖测试注入的起点。
        for (int x = 0; x <= 5; x++) level.getChunk(x, 0);
        var startChunk = level.getChunk(0, 0);
        var oldStarts = new HashMap<>(startChunk.getAllStarts());
        Map<ChunkAccess, Map<Structure, LongSet>> oldReferences = new HashMap<>();
        try {
            List<StructurePiece> pieces = new ArrayList<>();
            for (int x : new int[]{12, 60}) {
                var piece = new DesertPyramidPiece(RandomSource.create(123), 0, 0);
                var box = piece.getBoundingBox(); piece.move(x - box.minX(), 70 - box.minY(), -box.minZ());
                pieces.add(piece);
            }
            for (var type : targets.keySet()) {
                startChunk.setStartForStructure(type, type == registry.get(new ResourceLocation("minecraft", "stronghold"))
                        ? StructureStart.INVALID_START : new StructureStart(type, new ChunkPos(0, 0), 0, new PiecesContainer(pieces)));
            }
            for (int x = 0; x <= 5; x++) {
                var chunk = level.getChunk(x, 0);
                oldReferences.put(chunk, new HashMap<>(chunk.getAllReferences()));
                var refs = new HashMap<>(chunk.getAllReferences());
                for (var type : targets.keySet()) refs.put(type, new LongOpenHashSet(new long[]{ChunkPos.asLong(0, 0)}));
                chunk.setAllReferences(refs);
            }
            int checks = 0;
            for (int x = 0; x < 96; x++) for (int y : new int[]{69, 70, 75, 90, 120}) {
                var pos = new BlockPos(x, y, 5);
                List<ResourceLocation> expected = new ArrayList<>();
                for (var entry : targets.entrySet()) {
                    var start = level.structureManager().getStructureWithPieceAt(pos, entry.getKey());
                    if (start != null && start.isValid()) expected.add(entry.getValue());
                }
                if (!expected.equals(method.invoke(null, level, pos, targets))) throw new AssertionError("structure mismatch at " + pos);
                checks++;
            }
            if (!((List<?>) method.invoke(null, level, new BlockPos(40, 75, 5), targets)).isEmpty())
                throw new AssertionError("gap inside whole structure bounds must not count");
            if (((List<?>) method.invoke(null, level, new BlockPos(17, 75, 5), targets)).size() != 2)
                throw new AssertionError("cross-chunk piece must match both targets: "
                        + method.invoke(null, level, new BlockPos(17, 75, 5), targets) + "; boxes="
                        + pieces.stream().map(StructurePiece::getBoundingBox).toList() + "; starts=" + startChunk.getAllStarts()
                        + "; references=" + level.structureManager().getAllStructuresAt(new BlockPos(17, 75, 5)));
            System.out.println("JOURNEY_QUERY_PASS: " + checks + " vanilla-equivalent positions, cross-chunk pieces, height, gaps, invalid starts, simultaneous targets");
        } finally {
            startChunk.setAllStarts(oldStarts);
            oldReferences.forEach(ChunkAccess::setAllReferences);
        }
    }
}
