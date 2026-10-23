package com.thelongtravail.abyss;

import com.thelongtravail.TravailAspect;
import com.thelongtravail.config.RainConfig;
import com.thelongtravail.data.LongTravailData;
import com.thelongtravail.helper.TravailCurios;
import com.thelongtravail.registry.ModRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import top.theillusivec4.curios.api.CuriosApi;
import java.util.*;

public final class RainState {
    public static final String COOLDOWN_KEY = "TravailTokaidoCooldown";

    public enum EkiState {
        INACTIVE,
        MALICE,
        WITNESS
    }

    private enum Area {
        INVENTORY,
        OFFHAND,
        ARMOR,
        ENDER,
        CURIOS
    }

    private record Location(Area area, String identifier, int slot) {}

    private static final Map<Player, Location> TOKAIDO_LOCATIONS = new WeakHashMap<>();
    public static boolean clientWeather, clientDry, clientSnow;
    public static float clientSpeed = 1;
    public static boolean clientFire;
    public static int clientPlayerId = Integer.MIN_VALUE;
    public static List<String> clientSlots;

    public static void resetClient() {
        clientWeather = clientDry = clientSnow = clientFire = false;
        clientSpeed = 1;
        clientSlots = null;
        clientPlayerId = Integer.MIN_VALUE;
    }

    public static void forget(ServerPlayer p) {
        com.thelongtravail.api.CarryingApi.forget(p);
        TOKAIDO_LOCATIONS.remove(p);
    }

    public static void clear() {
        com.thelongtravail.api.CarryingApi.clear();
        TOKAIDO_LOCATIONS.clear();
    }

    public static void inventoryTick(ServerPlayer player, int slot) {
        com.thelongtravail.api.CarryingApi.inventoryTick(player, ModRegistry.EKI.get(), slot);
    }

    public static boolean carrying(ServerPlayer player) {
        return com.thelongtravail.api.CarryingApi.isCarrying(player, ModRegistry.EKI.get());
    }

    public static boolean equipped(Player p) {
        return CuriosApi.getCuriosInventory(p)
                .resolve()
                .map(
                        h -> {
                            var hint = TOKAIDO_LOCATIONS.get(p);
                            if (hint != null && RainConfig.SLOTS.get().contains(hint.identifier)) {
                                var current = h.getStacksHandler(hint.identifier);
                                if (current.isPresent()
                                        && hint.slot >= 0
                                        && hint.slot < current.get().getStacks().getSlots()
                                        && current.get()
                                                .getStacks()
                                                .getStackInSlot(hint.slot)
                                                .is(ModRegistry.TOKAIDO.get())) return true;
                            }
                            TOKAIDO_LOCATIONS.remove(p);
                            for (String id : RainConfig.SLOTS.get()) {
                                var handler = h.getStacksHandler(id);
                                if (handler.isEmpty()) continue;
                                var stacks = handler.get().getStacks();
                                for (int i = 0; i < stacks.getSlots(); i++)
                                    if (stacks.getStackInSlot(i).is(ModRegistry.TOKAIDO.get())) {
                                        TOKAIDO_LOCATIONS.put(p, new Location(Area.CURIOS, id, i));
                                        return true;
                                    }
                            }
                            return false;
                        })
                .orElse(false);
    }

    public static boolean weather(ServerPlayer p, boolean local, boolean sky) {
        return weather((net.minecraft.world.entity.LivingEntity) p, local, sky);
    }

    public static boolean weather(
            net.minecraft.world.entity.LivingEntity p, boolean local, boolean sky) {
        var level = (net.minecraft.server.level.ServerLevel) p.level();
        if (!level.dimension().equals(Level.OVERWORLD)) return false;
        if (!level.getLevelData().isRaining() && !level.getLevelData().isThundering()) return false;
        if (sky && !level.canSeeSky(p.blockPosition())) return false;
        if (local)
            try (var scope = new PrecipitationScope(level)) {
                if (level.getBiome(p.blockPosition()).value().getPrecipitationAt(p.blockPosition())
                        == Biome.Precipitation.NONE) return false;
            }
        return true;
    }

    public static boolean ekiActive(ServerPlayer p) {
        return ekiState(p) != EkiState.INACTIVE;
    }

    public static EkiState ekiState(ServerPlayer p) {
        return ekiState(p, TravailCurios.stack(p));
    }

    public static EkiState ekiState(ServerPlayer p, ItemStack diary) {
        if (diary.isEmpty()
                || !weather(p, RainConfig.EKI_LOCAL.get(), RainConfig.EKI_SKY.get())
                || !carrying(p)) return EkiState.INACTIVE;
        return LongTravailData.hasWitness(diary, TravailAspect.ABYSS)
                ? EkiState.WITNESS
                : EkiState.MALICE;
    }

    public static boolean ekiMalice(ServerPlayer p) {
        return ekiState(p) == EkiState.MALICE;
    }

    public static boolean passive(ServerPlayer p) {
        return equipped(p)
                && weather(p, RainConfig.TOKAIDO_LOCAL.get(), RainConfig.TOKAIDO_SKY.get());
    }

    public static float speed(Player p) {
        return p instanceof ServerPlayer sp
                ? (ekiMalice(sp) ? (float) (1 + RainConfig.SPEED.get()) : 1)
                : p.getId() == clientPlayerId ? clientSpeed : 1;
    }

    public static boolean fire(Player p) {
        return p instanceof ServerPlayer sp
                ? RainConfig.FIRE.get() && passive(sp)
                : p.getId() == clientPlayerId && clientFire;
    }

    public static int cooldown(Player p) {
        return Math.max(0, p.getPersistentData().getInt(COOLDOWN_KEY));
    }

    private RainState() {}
}
