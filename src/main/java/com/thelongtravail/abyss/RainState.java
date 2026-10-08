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
    private static final Map<ServerPlayer, Long> EXTERNAL = new WeakHashMap<>();
    public enum EkiState { INACTIVE, MALICE, WITNESS }
    private enum Area { INVENTORY, OFFHAND, ARMOR, ENDER, CURIOS }
    // 仅缓存位置，每次重新读取当前容器，不缓存旧物品或未找到物品的结果。
    private record Location(Area area, String identifier, int slot) {}
    private static final Map<ServerPlayer, Location> EKI_LOCATIONS = new WeakHashMap<>();
    private static final Map<Player, Location> TOKAIDO_LOCATIONS = new WeakHashMap<>();
    public static boolean clientWeather, clientDry, clientSnow;
    public static float clientSpeed = 1;
    public static boolean clientFire;
    public static int clientPlayerId = Integer.MIN_VALUE;
    public static List<String> clientSlots;
    public static void resetClient() { clientWeather = clientDry = clientSnow = clientFire = false; clientSpeed = 1; clientSlots=null; clientPlayerId=Integer.MIN_VALUE; }
    public static void forget(ServerPlayer p) { EXTERNAL.remove(p); EKI_LOCATIONS.remove(p); TOKAIDO_LOCATIONS.remove(p); }
    public static void clear() { EXTERNAL.clear(); EKI_LOCATIONS.clear(); TOKAIDO_LOCATIONS.clear(); }
    public static void inventoryTick(ServerPlayer player, int slot) {
        // 原版传入实际槽位索引，外部模拟调用（包括 RSI）传入 -1。
        // 原版背包更新也必须遵守携带位置的禁用配置。
        if (slot < 0 && RainConfig.EXTERNAL_TICK.get()) EXTERNAL.put(player, player.level().getGameTime());
    }
    private static boolean eki(ItemStack stack) { return stack.is(ModRegistry.EKI.get()); }
    public static boolean carrying(ServerPlayer p) {
        Location hint = EKI_LOCATIONS.get(p);
        if (hint != null && validEkiLocation(p, hint)) return true;
        EKI_LOCATIONS.remove(p);
        var inv = p.getInventory();
        if (RainConfig.INVENTORY.get() && findEki(p, inv.items, Area.INVENTORY)) return true;
        if (RainConfig.OFFHAND.get() && findEki(p, inv.offhand, Area.OFFHAND)) return true;
        if (RainConfig.ARMOR.get() && findEki(p, inv.armor, Area.ARMOR)) return true;
        if (RainConfig.ENDER.get()) for (int i = 0; i < p.getEnderChestInventory().getContainerSize(); i++)
            if (eki(p.getEnderChestInventory().getItem(i))) {
                EKI_LOCATIONS.put(p, new Location(Area.ENDER, null, i)); return true;
            }
        if (RainConfig.CURIOS.get()) {
            var found = CuriosApi.getCuriosInventory(p).resolve().flatMap(h -> h.findFirstCurio(ModRegistry.EKI.get()));
            if (found.isPresent()) {
                var context = found.get().slotContext();
                EKI_LOCATIONS.put(p, new Location(Area.CURIOS, context.identifier(), context.index())); return true;
            }
        }
        Long last = EXTERNAL.get(p);
        long now = p.level().getGameTime();
        return RainConfig.EXTERNAL_TICK.get() && last != null && now >= last && now - last <= 2;
    }
    private static boolean findEki(ServerPlayer p, List<ItemStack> stacks, Area area) {
        for (int i = 0; i < stacks.size(); i++) if (eki(stacks.get(i))) {
            EKI_LOCATIONS.put(p, new Location(area, null, i)); return true;
        }
        return false;
    }
    private static boolean ekiAt(List<ItemStack> stacks, int slot) {
        return slot >= 0 && slot < stacks.size() && eki(stacks.get(slot));
    }
    private static boolean validEkiLocation(ServerPlayer p, Location hint) {
        return switch (hint.area) {
            case INVENTORY -> RainConfig.INVENTORY.get() && ekiAt(p.getInventory().items, hint.slot);
            case OFFHAND -> RainConfig.OFFHAND.get() && ekiAt(p.getInventory().offhand, hint.slot);
            case ARMOR -> RainConfig.ARMOR.get() && ekiAt(p.getInventory().armor, hint.slot);
            case ENDER -> RainConfig.ENDER.get() && hint.slot >= 0 && hint.slot < p.getEnderChestInventory().getContainerSize()
                    && eki(p.getEnderChestInventory().getItem(hint.slot));
            case CURIOS -> RainConfig.CURIOS.get() && CuriosApi.getCuriosInventory(p).resolve()
                    .flatMap(h -> h.getStacksHandler(hint.identifier))
                    .map(h -> hint.slot >= 0 && hint.slot < h.getStacks().getSlots() && eki(h.getStacks().getStackInSlot(hint.slot))).orElse(false);
        };
    }
    public static boolean equipped(Player p) {
        return CuriosApi.getCuriosInventory(p).resolve().map(h -> {
            var hint = TOKAIDO_LOCATIONS.get(p);
            if (hint != null && RainConfig.SLOTS.get().contains(hint.identifier)) {
                var current = h.getStacksHandler(hint.identifier);
                if (current.isPresent() && hint.slot >= 0 && hint.slot < current.get().getStacks().getSlots()
                        && current.get().getStacks().getStackInSlot(hint.slot).is(ModRegistry.TOKAIDO.get())) return true;
            }
            TOKAIDO_LOCATIONS.remove(p);
            for (String id : RainConfig.SLOTS.get()) {
                var handler = h.getStacksHandler(id);
                if (handler.isEmpty()) continue;
                var stacks = handler.get().getStacks();
                for (int i = 0; i < stacks.getSlots(); i++) if (stacks.getStackInSlot(i).is(ModRegistry.TOKAIDO.get())) {
                    TOKAIDO_LOCATIONS.put(p, new Location(Area.CURIOS, id, i)); return true;
                }
            }
            return false;
        }).orElse(false);
    }
    public static boolean weather(ServerPlayer p, RainConfig.Weather mode, boolean local, boolean sky) {
        var level = p.serverLevel();
        if (!level.dimension().equals(Level.OVERWORLD)) return false;
        // 玩法判定以天气标志为准，不受视觉渐变强度影响。
        if (mode == RainConfig.Weather.THUNDER_ONLY && !level.getLevelData().isThundering()) return false;
        if (mode == RainConfig.Weather.RAIN_OR_THUNDER && !level.getLevelData().isRaining() && !level.getLevelData().isThundering()) return false;
        if (sky && !level.canSeeSky(p.blockPosition())) return false;
        if (local) try (var scope = new PrecipitationScope(level)) {
            if (level.getBiome(p.blockPosition()).value().getPrecipitationAt(p.blockPosition()) == Biome.Precipitation.NONE) return false;
        }
        return true;
    }
    public static boolean ekiActive(ServerPlayer p) {
        return ekiState(p) != EkiState.INACTIVE;
    }
    public static EkiState ekiState(ServerPlayer p) { return ekiState(p, TravailCurios.stack(p)); }
    public static EkiState ekiState(ServerPlayer p, ItemStack diary) {
        if (diary.isEmpty() || !weather(p, RainConfig.EKI_WEATHER.get(), RainConfig.EKI_LOCAL.get(), RainConfig.EKI_SKY.get()) || !carrying(p))
            return EkiState.INACTIVE;
        return LongTravailData.hasWitness(diary, TravailAspect.ABYSS) ? EkiState.WITNESS : EkiState.MALICE;
    }
    public static boolean ekiMalice(ServerPlayer p) {
        return ekiState(p) == EkiState.MALICE;
    }
    public static boolean passive(ServerPlayer p) {
        return equipped(p) && weather(p, RainConfig.TOKAIDO_WEATHER.get(), RainConfig.TOKAIDO_LOCAL.get(), RainConfig.TOKAIDO_SKY.get());
    }
    public static float speed(Player p) {
        return p instanceof ServerPlayer sp ? (ekiMalice(sp) ? (float)(1 + RainConfig.SPEED.get()) : 1) : p.getId()==clientPlayerId ? clientSpeed : 1;
    }
    public static boolean fire(Player p) {
        return p instanceof ServerPlayer sp ? RainConfig.FIRE.get() && passive(sp) : p.getId()==clientPlayerId && clientFire;
    }
    public static int cooldown(Player p) { return Math.max(0, p.getPersistentData().getInt(COOLDOWN_KEY)); }
    private RainState() {}
}
