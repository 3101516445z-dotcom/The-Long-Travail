package travail.smoke;

import com.mojang.authlib.GameProfile;
import com.thelongtravail.TravailAspect;
import com.thelongtravail.config.TravailConfig;
import com.thelongtravail.data.*;
import com.thelongtravail.event.TravailEvents;
import com.thelongtravail.registry.ModRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ReviewRegressionSmoke {
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }

    public static void run(ServerLevel level) {
        var packets = new ArrayList<Integer>();
        var progressPackets = new ArrayList<Boolean>();
        var player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "ReviewSmoke"));
        java.util.function.Consumer<net.minecraft.network.protocol.Packet<?>> capture = packet -> {
            if (packet instanceof net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket payload
                    && payload.getIdentifier().toString().equals("the_long_travail:main")) {
                var data = new net.minecraft.network.FriendlyByteBuf(payload.getData().duplicate());
                if (data.readVarInt() == 4) {
                    packets.add(data.readVarInt()); data.readVarInt(); data.readBoolean();
                    progressPackets.add(data.readBoolean());
                }
            }
        };
        var wire = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND) {
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet) { capture.accept(packet); }
        };
        player.connection = new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(), wire, player) {
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet) { capture.accept(packet); }
        };
        var events = new TravailEvents();
        var diary = new ItemStack(ModRegistry.LONG_TRAVAIL.get());
        check(LongTravailData.tryInitialize(diary, player), "valid journey generation");
        VisualDeprivation.start(player, 100);
        check(!progressPackets.get(progressPackets.size() - 1), "start is distinct from progress");
        VisualDeprivation.tick(player);
        check(progressPackets.get(progressPackets.size() - 1) && packets.get(packets.size() - 1) == 100,
                "active server tick sends authoritative progress");
        events.onDiaryReset(player, diary);
        check(VisualDeprivation.remaining(player) == 0 && packets.get(packets.size() - 1) == 0,
                "online diary reset sends visual end packet");
        check(!player.hasEffect(ModRegistry.VISUAL_DEPRIVATION.get()), "reset removes presentation icon");
        int idlePackets = packets.size();
        VisualDeprivation.tick(player);
        check(packets.size() == idlePackets, "inactive players send no progress");
        int previous = packets.size();
        VisualDeprivation.clear(player);
        VisualDeprivation.clear(player);
        check(packets.size() == previous + 2 && packets.get(packets.size() - 1) == 0,
                "clear repairs stale client even without server tags");
        VisualDeprivation.start(player, 100);
        events.onLogout(new net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(player));
        check(VisualDeprivation.remaining(player) == 100, "logout preserves persisted curse deadline");
        VisualDeprivation.resend(player);
        check(packets.get(packets.size() - 1) == 100, "login resends remaining episode");
        player.getPersistentData().putLong("LongTravailVisualDeprivationUntil", level.getGameTime());
        VisualDeprivation.tick(player);
        check(packets.get(packets.size() - 1) == 0 && !player.hasEffect(ModRegistry.VISUAL_DEPRIVATION.get()),
                "natural expiry reconciles client and icon");

        var warnings = new ArrayList<String>();
        var disabled = RequirementPools.validate(List.of(), 3, id -> false, warnings::add);
        check(disabled.status() == RequirementPools.Status.DISABLED && warnings.isEmpty(), "empty pool intentionally disabled");
        var zero = RequirementPools.validate(List.of("missing:biome|1"), 0, id -> false, warnings::add);
        check(zero.status() == RequirementPools.Status.DISABLED, "zero draw intentionally disabled");
        var invalid = RequirementPools.validate(List.of("missing:biome|1"), 3, id -> false, warnings::add);
        check(invalid.status() == RequirementPools.Status.INVALID, "unavailable nonempty pool is invalid");
        var distinct = RequirementPools.validate(List.of("minecraft:plains|first|2", "minecraft:plains|second|9"),
                3, id -> true, warnings::add);
        check(distinct.entries().size() == 1 && distinct.count() == 1 && distinct.entries().get(0).weight() == 2,
                "deduplicate by ID, retain first weight, reduce draw count");

        var config = TravailConfig.BIOME_POOLS.get(TravailAspect.FLOURISHING);
        var original = config.get();
        try {
            config.set(List.of("review_missing:no_biome|1"));
            RequirementPools.invalidate();
            CompoundTag before = diary.getTag().copy();
            check(LongTravailData.refreshedCopy(diary, player).isEmpty(), "invalid generation returns failure");
            check(diary.getTag().equals(before), "failed refresh preserves complete original NBT");
            var fresh = new ItemStack(ModRegistry.LONG_TRAVAIL.get());
            check(!LongTravailData.tryInitialize(fresh, player) && !fresh.hasTag(), "failed first initialization writes nothing");
            check(!LongTravailData.visitBiome(fresh, new ResourceLocation("minecraft:plains")) && !fresh.hasTag(),
                    "visiting before valid generation cannot award empty requirements");
            var cached = RequirementPools.current(player);
            for (int i = 0; i < 100; i++) LongTravailData.initialize(fresh, player);
            check(RequirementPools.current(player) == cached, "invalid pool validation cached across ticks");

            var inventory = top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).resolve().orElseThrow();
            var slots = new top.theillusivec4.curios.common.inventory.CurioStacksHandler(inventory, "travel_diary", 1,
                    true, false, true, top.theillusivec4.curios.api.type.capability.ICurio.DropRule.DEFAULT);
            inventory.setCurios(new java.util.HashMap<>(java.util.Map.of("travel_diary", slots)));
            slots.getStacks().setStackInSlot(0, diary);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModRegistry.RENEWAL.get()));
            ModRegistry.RENEWAL.get().use(level, player, InteractionHand.MAIN_HAND);
            check(player.getMainHandItem().getCount() == 1 && slots.getStacks().getStackInSlot(0).getTag().equals(before),
                    "failed use preserves consumable and equipped diary");
            check(!player.getCooldowns().isOnCooldown(ModRegistry.RENEWAL.get()), "failed use adds no cooldown");

            var legacy = diary.copy();
            legacy.getTag().getCompound("LongTravail").putInt("Version", 1);
            legacy.getTag().getCompound("LongTravail").putInt("Witnesses", 63);
            for (var value : TravailAspect.values()) {
                var legacyAspect = legacy.getTag().getCompound("LongTravail").getCompound("Requirements").getCompound(value.id());
                legacyAspect.remove("BiomesStatus");
                legacyAspect.remove("StructuresStatus");
            }
            var legacyBefore = legacy.getTag().copy();
            check(LongTravailData.tryInitialize(legacy, player) && legacy.getTag().equals(legacyBefore),
                    "old journey remains byte-for-byte intact even with broken new config");
        } finally {
            config.set(original);
            RequirementPools.invalidate();
        }
        var recovered = new ItemStack(ModRegistry.LONG_TRAVAIL.get());
        check(LongTravailData.tryInitialize(recovered, player), "generation recovers after config correction");
        var root = recovered.getTag().getCompound("LongTravail");
        check(root.getInt("Version") == 2, "new journey uses validated schema");
        var all = root.getCompound("Requirements");
        var aspect = all.getCompound(TravailAspect.FLOURISHING.id());
        List<ResourceLocation> biomes = ids(aspect.getList("Biomes", Tag.TAG_STRING));
        List<ResourceLocation> structures = ids(aspect.getList("Structures", Tag.TAG_STRING));
        biomes.forEach(id -> LongTravailData.visitBiome(recovered, id));
        structures.forEach(id -> LongTravailData.visitStructure(recovered, id));
        check(LongTravailData.hasWitness(recovered, TravailAspect.FLOURISHING), "valid version 2 requirements can complete");
        var broken = recovered.copy();
        var brokenRoot = broken.getTag().getCompound("LongTravail");
        brokenRoot.putInt("Witnesses", 0);
        var brokenAspect = brokenRoot.getCompound("Requirements").getCompound(TravailAspect.FLOURISHING.id());
        brokenAspect.remove("BiomesStatus");
        var trigger = new ListTag(); trigger.add(net.minecraft.nbt.StringTag.valueOf("minecraft:plains"));
        brokenAspect.put("Biomes", trigger);
        LongTravailData.visitBiome(broken, new ResourceLocation("minecraft:plains"));
        check(!LongTravailData.hasWitness(broken, TravailAspect.FLOURISHING), "version 2 missing validation cannot auto-complete");

        int biomeDraws = TravailConfig.BIOME_DRAW_COUNT.get(), structureDraws = TravailConfig.STRUCTURE_DRAW_COUNT.get();
        try {
            TravailConfig.BIOME_DRAW_COUNT.set(0);
            TravailConfig.STRUCTURE_DRAW_COUNT.set(0);
            RequirementPools.invalidate();
            var intentional = new ItemStack(ModRegistry.LONG_TRAVAIL.get());
            check(LongTravailData.tryInitialize(intentional, player), "explicit zero-draw configuration generates successfully");
            for (var value : TravailAspect.values()) check(LongTravailData.hasWitness(intentional, value), "intentional empty journey completes");
        } finally {
            TravailConfig.BIOME_DRAW_COUNT.set(biomeDraws);
            TravailConfig.STRUCTURE_DRAW_COUNT.set(structureDraws);
            RequirementPools.invalidate();
        }
        System.out.println("TRAVAIL_REVIEW_PASS: visual lifecycle packets, expiry, logout/resend, pool failures/cache, transactional reset, legacy preservation, recovery/completion");
    }

    private static List<ResourceLocation> ids(ListTag list) {
        var result = new ArrayList<ResourceLocation>();
        for (int i = 0; i < list.size(); i++) result.add(new ResourceLocation(list.getString(i)));
        return result;
    }
}
