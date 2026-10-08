package travail.smoke;

import com.mojang.authlib.GameProfile;
import com.thelongtravail.TravailAspect;
import com.thelongtravail.config.RainConfig;
import com.thelongtravail.data.LongTravailData;
import com.thelongtravail.network.RainPacket;
import com.thelongtravail.abyss.*;
import com.thelongtravail.registry.ModRegistry;
import net.minecraft.server.level.*;
import net.minecraft.world.item.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.common.inventory.CurioStacksHandler;
import top.theillusivec4.curios.api.type.capability.ICurio.DropRule;
import java.util.*;

// 验证即时状态语义与实际查询次数，不用运行时间断言代替正确性。
public final class RainStateOptimizationSmoke {
    private static final class CountedList extends AbstractList<ItemStack> {
        final List<ItemStack> delegate;
        int reads;
        CountedList(List<ItemStack> delegate) { this.delegate = delegate; }
        @Override public ItemStack get(int index) { if (index == 0) reads++; return delegate.get(index); }
        @Override public ItemStack set(int index, ItemStack value) { return delegate.set(index, value); }
        @Override public int size() { return delegate.size(); }
    }
    @SuppressWarnings("unchecked")
    private static CountedList countReads(Object stacks) throws Exception {
        var f = net.minecraft.core.NonNullList.class.getDeclaredField("list"); f.setAccessible(true);
        var counted = new CountedList((List<ItemStack>)f.get(stacks)); f.set(stacks, counted); return counted;
    }
    private static final class Player extends FakePlayer {
        int rainPackets;
        Player(ServerLevel level) {
            super(level, new GameProfile(UUID.randomUUID(), "RainStateProbe"));
            var wire = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
            connection = new ServerGamePacketListenerImplStub(level, wire, this);
        }
    }
    private static final class ServerGamePacketListenerImplStub extends net.minecraft.server.network.ServerGamePacketListenerImpl {
        private final Player owner;
        ServerGamePacketListenerImplStub(ServerLevel level, net.minecraft.network.Connection wire, Player p) {
            super(level.getServer(), wire, p); owner = p;
        }
        @Override public void send(net.minecraft.network.protocol.Packet<?> packet) {
            if (packet instanceof net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket payload
                    && payload.getIdentifier().toString().equals("the_long_travail:main")) {
                var data = new net.minecraft.network.FriendlyByteBuf(payload.getData().duplicate());
                if (data.readVarInt() == 5) owner.rainPackets++;
            }
        }
    }
    private static final class Healing {
        Player player; Runnable action;
        @SubscribeEvent public void heal(LivingHealEvent event) { if (event.getEntity() == player && action != null) action.run(); }
    }
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError("RAIN_OPT: " + message); }
    private static Object field(Class<?> type, Object instance, String name) throws Exception {
        var f = type.getDeclaredField(name); f.setAccessible(true); return f.get(instance);
    }
    private static RainPacket last(Player p) throws Exception {
        var states = (Map<?, ?>)field(RainEvents.class, null, "PLAYERS"); var progress = states.get(p);
        return (RainPacket)field(progress.getClass(), progress, "last");
    }
    private static void tick(Player p) { RainEvents.tick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, p)); }
    private static ItemStack equip(Player p) {
        var h = CuriosApi.getCuriosInventory(p).resolve().orElseThrow();
        var diary = new CurioStacksHandler(h, "travel_diary", 1, true, false, true, DropRule.DEFAULT);
        var feet = new CurioStacksHandler(h, "feet", 3, true, false, true, DropRule.DEFAULT);
        var curio = new CurioStacksHandler(h, "curio", 1, true, false, true, DropRule.DEFAULT);
        h.setCurios(new HashMap<>(Map.of("travel_diary", diary, "feet", feet, "curio", curio)));
        var stack = new ItemStack(ModRegistry.LONG_TRAVAIL.get());
        check(LongTravailData.tryInitialize(stack, p), "initialize");
        for (var aspect : TravailAspect.values()) LongTravailData.setWitness(stack, aspect, true);
        LongTravailData.setWitness(stack, TravailAspect.ABYSS, false);
        diary.getStacks().setStackInSlot(0, stack); return stack;
    }
    public static void run(ServerLevel level) throws Exception {
        var values = new LinkedHashMap<net.minecraftforge.common.ForgeConfigSpec.ConfigValue<?>, Object>();
        for (var f : RainConfig.class.getFields()) if (f.get(null) instanceof net.minecraftforge.common.ForgeConfigSpec.ConfigValue<?> v) values.put(v, v.get());
        var weather = (net.minecraft.world.level.storage.ServerLevelData)level.getLevelData();
        int clear = weather.getClearWeatherTime(), rain = weather.getRainTime(), thunder = weather.getThunderTime();
        boolean raining = weather.isRaining(), thundering = weather.isThundering();
        var p = new Player(level); var other = new Player(level); var healing = new Healing(); healing.player = p;
        MinecraftForge.EVENT_BUS.register(healing);
        try {
            RainConfig.INVENTORY.set(true); RainConfig.OFFHAND.set(true); RainConfig.ENDER.set(true); RainConfig.ARMOR.set(true); RainConfig.CURIOS.set(true);
            RainConfig.EXTERNAL_TICK.set(true); RainConfig.EKI_LOCAL.set(false); RainConfig.EKI_SKY.set(false);
            RainConfig.TOKAIDO_LOCAL.set(false); RainConfig.TOKAIDO_SKY.set(false);
            RainConfig.EKI_WEATHER.set(RainConfig.Weather.RAIN_OR_THUNDER); RainConfig.TOKAIDO_WEATHER.set(RainConfig.Weather.RAIN_OR_THUNDER);
            RainConfig.SLOTS.set(List.of("feet")); RainConfig.FIRE.set(true);
            RainConfig.HEAL_SECONDS.set(1D); RainConfig.FOOD_SECONDS.set(5D);
            level.setWeatherParameters(0, 1000, true, false);
            var diary = equip(p); equip(other);
            var marker = countReads(p.getInventory().items);
            p.getInventory().items.set(35, new ItemStack(ModRegistry.EKI.get()));
            check(RainState.ekiState(p) == RainState.EkiState.MALICE && marker.reads > 0, "initial full scan");
            marker.reads = 0;
            for (int i = 0; i < 1000; i++) check(RainState.speed(p) > 1, "positive position hit");
            check(marker.reads == 0, "1000 speed reads skip preceding inventory slots");
            p.getInventory().items.set(35, ItemStack.EMPTY);
            check(RainState.speed(p) == 1, "same-tick removal invalidates hint");
            p.getInventory().items.set(18, new ItemStack(ModRegistry.EKI.get()));
            check(RainState.speed(p) > 1, "negative result not cached; same-tick direct insertion");
            RainConfig.INVENTORY.set(false); check(RainState.speed(p) == 1, "location flag immediately checked"); RainConfig.INVENTORY.set(true);
            LongTravailData.setWitness(diary, TravailAspect.ABYSS, true); check(RainState.ekiState(p) == RainState.EkiState.WITNESS && RainState.speed(p) == 1, "same-tick witness change");
            LongTravailData.setWitness(diary, TravailAspect.ABYSS, false);
            level.setWeatherParameters(200, 0, false, false); check(RainState.speed(p) == 1, "weather flags immediately checked");
            level.setWeatherParameters(0, 1000, true, false);
            p.getInventory().items.set(18, ItemStack.EMPTY);
            RainState.inventoryTick(p, -1); check(RainState.carrying(p), "external observation live");
            @SuppressWarnings("unchecked") var external = (Map<ServerPlayer, Long>)field(RainState.class, null, "EXTERNAL");
            external.put(p, level.getGameTime() - 3); check(!RainState.carrying(p), "external observation expires without cached positive");
            RainState.forget(p);

            var h = CuriosApi.getCuriosInventory(p).resolve().orElseThrow();
            var feet = h.getStacksHandler("feet").orElseThrow().getStacks();
            var footMarker = countReads(field(net.minecraftforge.items.ItemStackHandler.class, feet, "stacks"));
            feet.setStackInSlot(2, new ItemStack(ModRegistry.TOKAIDO.get()));
            check(RainState.equipped(p), "find boots"); footMarker.reads = 0;
            for (int i = 0; i < 1000; i++) check(RainState.fire(p), "live fire hint");
            check(footMarker.reads == 0, "1000 equipped reads skip preceding slots");
            RainConfig.SLOTS.set(List.of("curio")); check(!RainState.fire(p), "slot whitelist live"); RainConfig.SLOTS.set(List.of("feet"));
            feet.setStackInSlot(2, ItemStack.EMPTY); check(!RainState.fire(p), "same-tick unequip");
            feet.setStackInSlot(1, new ItemStack(ModRegistry.TOKAIDO.get())); check(RainState.fire(p), "same-tick changed slot");
            var ekiCurio = h.getStacksHandler("curio").orElseThrow().getStacks();
            ekiCurio.setStackInSlot(0, new ItemStack(ModRegistry.EKI.get())); check(RainState.carrying(p), "curio hint seeded");
            equip(p); check(!RainState.fire(p) && !RainState.carrying(p), "replaced capability containers do not reuse old stacks");

            feet = h.getStacksHandler("feet").orElseThrow().getStacks(); feet.setStackInSlot(2, new ItemStack(ModRegistry.TOKAIDO.get()));
            RainEvents.configReloaded(level.getServer()); tick(p); tick(other);
            var packet = last(p);
            check(packet.eki() == last(other).eki() && packet.tokaido() == last(other).tokaido(), "players share description lists");
            int sent = p.rainPackets;
            for (int i = 0; i < 10; i++) tick(p);
            check(last(p) == packet && p.rainPackets == sent, "unchanged sync constructs and sends no new packet");
            var snapshot = RainTooltips.class.getDeclaredMethod("serverSnapshot", int.class); snapshot.setAccessible(true);
            int clock = level.getServer().getTickCount(); Object first = snapshot.invoke(null, clock);
            check(snapshot.invoke(null, clock + 20) == first, "unchanged periodic config check does not rebuild text");
            RainConfig.DAMAGE.set(.75D); Object changed = snapshot.invoke(null, clock + 40);
            check(changed != first && snapshot.invoke(null, clock + 40) == changed, "direct config set rebuilds once for all players");
            RainConfig.SLOTS.set(List.of("feet", "curio")); RainEvents.configReloaded(level.getServer()); tick(p);
            check(last(p).slots().equals(List.of("feet", "curio")) && last(p) != packet, "reload refreshes snapshot immediately");

            RainConfig.HEAL_SECONDS.set(.05D); RainConfig.HEAL.set(1D); p.setHealth(10);
            final var currentFeet = feet;
            healing.action = () -> currentFeet.setStackInSlot(2, ItemStack.EMPTY);
            tick(p); check(!last(p).fire() && !RainState.fire(p), "heal callback unequip rechecked before sync and fire damage");
            currentFeet.setStackInSlot(2, new ItemStack(ModRegistry.TOKAIDO.get()));
            healing.action = () -> level.setWeatherParameters(200, 0, false, false);
            tick(p); check(!last(p).fire(), "heal callback weather change rechecked");
            System.out.println("RAIN_STATE_OPT_PASS: 1000 inventory/Curios hint hits, same-tick removal/insertion/config/witness/weather, external expiry, container replacement, shared text, unchanged packet identity/count, reload/direct set, heal callback boundaries");
        } finally {
            MinecraftForge.EVENT_BUS.unregister(healing);
            for (var entry : values.entrySet()) restore(entry.getKey(), entry.getValue());
            weather.setClearWeatherTime(clear); weather.setRainTime(rain); weather.setThunderTime(thunder); weather.setRaining(raining); weather.setThundering(thundering);
            RainEvents.logout(new net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(p));
            RainEvents.logout(new net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(other));
            RainEvents.configReloaded(level.getServer());
        }
    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void restore(net.minecraftforge.common.ForgeConfigSpec.ConfigValue value, Object previous) { value.set(previous); }
}
