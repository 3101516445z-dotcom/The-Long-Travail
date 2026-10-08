package com.thelongtravail.data;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.item.ItemStack;
import com.thelongtravail.network.TravailNetwork;
// 每秒调用一次；玩家静止时，世界观测缓存五秒后过期。
public final class JourneyQueries {
    private static final Map<ServerPlayer, State> STATES = new WeakHashMap<>();
    private static final class State {
        Object dimension; BlockPos pos; long nextRead; ResourceLocation biome, discovered;
        LongTravailData.RequirementIdentity diary; boolean biomeRequirements; Map<Structure, ResourceLocation> structures = Map.of();
    }
    public static void clear() { STATES.clear(); JourneyVisits.invalidate(); }
    public static void forget(ServerPlayer player) { STATES.remove(player); JourneyVisits.forget(player); }
    public static void update(ServerPlayer player, ItemStack stack) {
        State state = STATES.computeIfAbsent(player, p -> new State());
        var level = player.serverLevel(); long now = level.getGameTime();
        boolean worldChanged = state.dimension != level.dimension() || !player.blockPosition().equals(state.pos)
                || now >= state.nextRead || now < state.nextRead - 100;
        LongTravailData.RequirementIdentity identity = null;
        if (!stack.isEmpty()) { LongTravailData.initialize(stack, player); identity = LongTravailData.queryIdentity(stack); }
        boolean diaryChanged = !Objects.equals(identity, state.diary);
        ResourceLocation previousBiome = state.biome;
        if (worldChanged) {
            state.dimension = level.dimension(); state.pos = player.blockPosition().immutable(); state.nextRead = now + 100;
            state.biome = level.getBiome(state.pos).unwrapKey().map(ResourceKey::location).orElse(null);
            if (state.biome != null && !state.biome.equals(state.discovered)) {
                state.discovered = state.biome;
                if (PlayerJourneyData.discoverBiome(player, state.biome)) TravailNetwork.sendJourney(player);
            }
        }
        if (stack.isEmpty() || !LongTravailData.isInitialized(stack)) { state.diary = null; state.structures = Map.of(); state.biomeRequirements = false; return; }
        if (diaryChanged) refreshRequirements(state, stack, level);
        if (worldChanged || diaryChanged) {
            var before = LongTravailData.queryIdentity(stack);
            boolean progressChanged = false;
            if ((diaryChanged || !Objects.equals(previousBiome, state.biome)) && state.biome != null && state.biomeRequirements)
                progressChanged = LongTravailData.visitBiome(stack, state.biome);
            JourneyVisits.rebase(player, before, stack);
            before = LongTravailData.queryIdentity(stack);
            for (ResourceLocation id : JourneyVisits.observe(player, stack, state.structures))
                progressChanged |= LongTravailData.visitStructure(stack, id);
            JourneyVisits.rebase(player, before, stack);
            if (progressChanged) refreshRequirements(state, stack, level);
            if (diaryChanged || progressChanged) state.diary = LongTravailData.queryIdentity(stack);
        }
    }
    // 仅查询已加载的数据，未就绪的部分由 JourneyVisits 稍后确认。
    private static List<ResourceLocation> structuresAt(ServerLevel level, BlockPos pos, Map<Structure, ResourceLocation> targets) {
        return JourneyVisits.loadedAt(level, pos, targets);
    }
    private static void refreshRequirements(State state, ItemStack stack, ServerLevel level) {
        state.biomeRequirements = LongTravailData.hasRemainingBiomes(stack);
        var registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        Map<Structure, ResourceLocation> targets = new LinkedHashMap<>();
        for (var id : LongTravailData.remainingStructures(stack)) {
            var structure = registry.get(id);
            if (structure != null) targets.put(structure, id);
        }
        state.structures = targets;
    }

    private JourneyQueries() {}
}
