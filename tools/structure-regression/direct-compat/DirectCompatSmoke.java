package travail.directtest;

import com.mojang.authlib.GameProfile;
import com.thelongtravail.compat.OptionalCompatState;
import com.thelongtravail.compat.attributeslib.PercentAttributes;
import com.thelongtravail.compat.ftbteams.FtbTeamsCompat;
import com.thelongtravail.compat.gravestone.GravestoneAccess;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

import java.nio.file.*;
import java.util.*;

@Mod("travail_smoke")
public final class DirectCompatSmoke {
    private static int checks;

    private static void check(boolean value, String label) {
        if (!value) throw new AssertionError(label);
        checks++;
    }

    public DirectCompatSmoke() {
        MinecraftForge.EVENT_BUS.addListener(this::run);
    }

    private void run(ServerStartedEvent event) {
        var server = event.getServer();
        long start = System.nanoTime();
        try {
            var level = server.overworld();
            var player =
                    new ServerPlayer(
                            server, level, new GameProfile(UUID.randomUUID(), "DirectProbe"));
            boolean attributes = ModList.get().isLoaded("attributeslib");
            for (int i = 0; i < 10000; i++) {
                var value = (RangedAttribute) PercentAttributes.create("probe", 32);
                check(
                        value.getDefaultValue() == 0
                                && value.getMinValue() == 0
                                && value.getMaxValue() == 32
                                && value.isClientSyncable(),
                        "attribute values");
                check(
                        value.getClass()
                                .getName()
                                .equals(
                                        attributes
                                                ? "dev.shadowsoffire.attributeslib.impl.PercentBasedAttribute"
                                                : RangedAttribute.class.getName()),
                        "attribute formatter type");
            }
            if (ModList.get().isLoaded("ftbteams")) Teams.run(player);
            else {
                for (int i = 0; i < 10000; i++) {
                    check(
                            FtbTeamsCompat.relationship(player, player)
                                    == FtbTeamsCompat.Relationship.DIFFERENT,
                            "absent team relation");
                    check(
                            FtbTeamsCompat.timeStopMembers(player).orElseThrow().isEmpty(),
                            "absent team members");
                }
            }
            if (ModList.get().isLoaded("rs_integration")) InvalidRsiReference.run(level);
            if (ModList.get().isLoaded("gravestone")) Graves.run(level, player);
            else check(GravestoneAccess.items(new Object()) == null, "absent gravestone");
            Files.writeString(
                    Path.of("direct-result.txt"),
                    "PASS checks="
                            + checks
                            + " elapsedMs="
                            + (System.nanoTime() - start) / 1000000);
        } catch (Throwable error) {
            error.printStackTrace();
            try {
                Files.writeString(Path.of("direct-result.txt"), "FAIL " + error);
            } catch (Exception ignored) {
            }
        } finally {
            server.halt(false);
        }
    }

    private static final class Teams {
        static void run(ServerPlayer player) throws Exception {
            var api = dev.ftb.mods.ftbteams.api.FTBTeamsAPI.api();
            var manager = api.getManager();
            UUID other = UUID.randomUUID();
            var second =
                    new ServerPlayer(
                            player.server,
                            player.serverLevel(),
                            new GameProfile(other, "OtherProbe"));
            // Real manager queries, including the no-player-team case; compare with the old
            // reflective contract.
            var sameMethod =
                    dev.ftb.mods.ftbteams.api.TeamManager.class.getMethod(
                            "arePlayersInSameTeam", UUID.class, UUID.class);
            for (int i = 0; i < 20000; i++) {
                var target = (i & 1) == 0 ? player : second;
                boolean expected =
                        (boolean) sameMethod.invoke(manager, player.getUUID(), target.getUUID());
                check(
                        FtbTeamsCompat.relationship(player, target)
                                == (expected
                                        ? FtbTeamsCompat.Relationship.SAME
                                        : FtbTeamsCompat.Relationship.DIFFERENT),
                        "team parity");
                var team = manager.getTeamForPlayer(player);
                check(
                        FtbTeamsCompat.timeStopMembers(player)
                                .orElseThrow()
                                .equals(
                                        team.isEmpty()
                                                ? Set.of()
                                                : new HashSet<>(team.get().getMembers())),
                        "member parity");
            }
            // Deterministic API fixtures cover teams and failure states without fabricating network
            // logins.
            var apiSlot = dev.ftb.mods.ftbteams.api.FTBTeamsAPI.class.getDeclaredField("instance");
            apiSlot.setAccessible(true);
            try {
                for (int scenario = 0; scenario < 4; scenario++) {
                    final int state = scenario;
                    var team =
                            (dev.ftb.mods.ftbteams.api.Team)
                                    java.lang.reflect.Proxy.newProxyInstance(
                                            DirectCompatSmoke.class.getClassLoader(),
                                            new Class<?>[] {dev.ftb.mods.ftbteams.api.Team.class},
                                            (proxy, method, args) -> {
                                                if (method.getName().equals("getMembers"))
                                                    return Set.of(player.getUUID(), other);
                                                throw new UnsupportedOperationException(
                                                        method.getName());
                                            });
                    var fixtureManager =
                            (dev.ftb.mods.ftbteams.api.TeamManager)
                                    java.lang.reflect.Proxy.newProxyInstance(
                                            DirectCompatSmoke.class.getClassLoader(),
                                            new Class<?>[] {
                                                dev.ftb.mods.ftbteams.api.TeamManager.class
                                            },
                                            (proxy, method, args) -> {
                                                if (state == 3)
                                                    throw new IllegalStateException(
                                                            "injected team query failure");
                                                return switch (method.getName()) {
                                                    case "arePlayersInSameTeam" -> state == 0;
                                                    case "getTeamForPlayer" -> Optional.of(team);
                                                    default ->
                                                            throw new UnsupportedOperationException(
                                                                    method.getName());
                                                };
                                            });
                    var fixtureApi =
                            (dev.ftb.mods.ftbteams.api.FTBTeamsAPI.API)
                                    java.lang.reflect.Proxy.newProxyInstance(
                                            DirectCompatSmoke.class.getClassLoader(),
                                            new Class<?>[] {
                                                dev.ftb.mods.ftbteams.api.FTBTeamsAPI.API.class
                                            },
                                            (proxy, method, args) ->
                                                    switch (method.getName()) {
                                                        case "isManagerLoaded" -> state != 2;
                                                        case "getManager" -> fixtureManager;
                                                        default ->
                                                                throw new UnsupportedOperationException(
                                                                        method.getName());
                                                    });
                    apiSlot.set(null, fixtureApi);
                    for (int i = 0; i < 5000; i++) {
                        check(
                                FtbTeamsCompat.relationship(player, second)
                                        == (state == 0
                                                ? FtbTeamsCompat.Relationship.SAME
                                                : state == 1
                                                        ? FtbTeamsCompat.Relationship.DIFFERENT
                                                        : FtbTeamsCompat.Relationship.UNAVAILABLE),
                                "controlled relation " + state);
                        var members = FtbTeamsCompat.timeStopMembers(player);
                        check(
                                state < 2
                                        ? members.orElseThrow()
                                                .equals(Set.of(player.getUUID(), other))
                                        : members.isEmpty(),
                                "controlled members " + state);
                    }
                }
            } finally {
                apiSlot.set(null, api);
            }
        }
    }

    private static final class InvalidRsiReference {
        static void run(ServerLevel level) throws Exception {
            var item =
                    net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(
                            new net.minecraft.resources.ResourceLocation(
                                    "rs_integration:unified_storage_disk"));
            check(item != null && item != Items.AIR, "new RSI disk registered");
            var stack = new ItemStack(item);
            var tag = stack.getOrCreateTag();
            tag.putUUID("Id", UUID.randomUUID());
            tag.putUUID("World", UUID.randomUUID());
            tag.putInt("Format", 1);
            var before = stack.save(new net.minecraft.nbt.CompoundTag());
            for (int i = 0; i < 1000; i++) {
                boolean rejected = false;
                try {
                    com.thelongtravail.abyss.copy.FluidCopyGraph.prepare(level, stack);
                } catch (Exception expected) {
                    rejected = true;
                }
                check(rejected, "invalid RSI safely rejects");
                check(
                        stack.save(new net.minecraft.nbt.CompoundTag()).equals(before),
                        "invalid RSI source untouched");
            }
        }
    }

    private static final class Graves {
        static void run(ServerLevel level, ServerPlayer player) {
            check(OptionalCompatState.GRAVESTONE.enabled(), "gravestone gate");
            player.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 37));
            var death = de.maxhenkel.gravestone.corelib.death.Death.fromPlayer(player);
            var tile =
                    new de.maxhenkel.gravestone.tileentity.GraveStoneTileEntity(
                            BlockPos.ZERO, Blocks.STONE.defaultBlockState());
            check(
                    ((java.util.Collection<?>) GravestoneAccess.items(tile)).isEmpty(),
                    "empty grave");
            tile.setDeath(death);
            var nullTile =
                    new de.maxhenkel.gravestone.tileentity.GraveStoneTileEntity(
                            BlockPos.ZERO, Blocks.STONE.defaultBlockState()) {
                        @Override
                        public de.maxhenkel.gravestone.corelib.death.Death getDeath() {
                            return null;
                        }
                    };
            check(GravestoneAccess.items(nullTile) == null, "null death");
            var sentinel = new AssertionError("grave target failure");
            var broken =
                    new de.maxhenkel.gravestone.tileentity.GraveStoneTileEntity(
                            BlockPos.ZERO, Blocks.STONE.defaultBlockState()) {
                        @Override
                        public de.maxhenkel.gravestone.corelib.death.Death getDeath() {
                            throw sentinel;
                        }
                    };
            try {
                GravestoneAccess.items(broken);
                throw new AssertionError("grave failure swallowed");
            } catch (IllegalStateException expected) {
                check(expected.getCause() == sentinel, "grave error boundary");
            }
            for (int i = 0; i < 20000; i++) {
                var expected = death.getAllItems();
                var actual = (List<?>) GravestoneAccess.items(tile);
                check(actual.size() == expected.size(), "grave item count");
                for (int j = 0; j < expected.size(); j++)
                    check(
                            ItemStack.matches((ItemStack) actual.get(j), expected.get(j)),
                            "grave item parity");
            }
            check(
                    death.getAllItems().stream()
                            .anyMatch(s -> s.is(Items.DIAMOND) && s.getCount() == 37),
                    "source grave unchanged");
        }
    }
}
