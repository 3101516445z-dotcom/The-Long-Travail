package travail.smoke;

import com.thelongtravail.config.TravailConfig;
import com.thelongtravail.data.RewardDelivery;
import com.thelongtravail.event.TravailEvents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import java.util.*;

public final class RewardDeliverySmoke {
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private static class Spawns {
        boolean cancel;
        boolean fail;
        List<ItemEntity> items = new ArrayList<>();
        @SubscribeEvent public void join(EntityJoinLevelEvent event) {
            if (event.getEntity() instanceof ItemEntity item) {
                if (fail) throw new IllegalStateException("reward fixture exception");
                if (cancel) event.setCanceled(true); else items.add(item);
            }
        }
    }
    public static void run(ServerLevel level) throws Exception {
        var settings = List.<net.minecraftforge.common.ForgeConfigSpec.ConfigValue<?>>of(TravailConfig.REWARD_QUEUE_CAPACITY,
                TravailConfig.REWARD_ENTITIES_PER_TICK, TravailConfig.REWARD_OVERFLOW, TravailConfig.REWARD_LIMIT, TravailConfig.PHANTOM_MAX_KILLS);
        var previous = settings.stream().map(v -> v.get()).toList();
        RewardDelivery original = RewardDelivery.get(level.getServer());
        Spawns spawns = new Spawns(); MinecraftForge.EVENT_BUS.register(spawns);
        List<Phantom> phantoms = new ArrayList<>();
        try {
            TravailConfig.REWARD_QUEUE_CAPACITY.set(8); TravailConfig.REWARD_ENTITIES_PER_TICK.set(2);
            TravailConfig.REWARD_OVERFLOW.set(TravailConfig.RewardOverflow.DEFER);
            Vec3 position = Vec3.atCenterOf(level.getSharedSpawnPos()); level.getChunkAt(net.minecraft.core.BlockPos.containing(position));
            RewardDelivery queue = new RewardDelivery();
            ItemStack named = new ItemStack(Items.DIAMOND_SWORD);
            named.setHoverName(net.minecraft.network.chat.Component.literal("saved reward"));
            try (var slot = queue.reserve()) { slot.submit(level, position, named, 5); }
            queue = RewardDelivery.load(queue.save(new CompoundTag()));
            check(queue.pendingItems() == 5 && queue.pendingRecords() == 1, "persistent count");
            queue.tick(level.getServer());
            check(queue.pendingItems() == 3 && spawns.items.size() == 2, "global per-tick nonstackable budget");
            check(spawns.items.get(0).getItem().getHoverName().getString().equals("saved reward"), "item NBT persisted");
            queue.tick(level.getServer()); check(queue.pendingItems() == 3, "duplicate tick does not spend twice");
            queue = RewardDelivery.load(queue.save(new CompoundTag())); spawns.cancel = true;
            queue.tick(level.getServer()); check(queue.pendingItems() == 3, "cancelled spawns retain rewards"); spawns.cancel = false;
            queue = RewardDelivery.load(queue.save(new CompoundTag())); spawns.fail = true;
            try { queue.tick(level.getServer()); throw new AssertionError("expected spawn exception"); }
            catch (IllegalStateException expected) { check(expected.getMessage().contains("fixture"), "expected listener exception"); }
            finally { spawns.fail = false; }
            check(queue.pendingItems() == 3, "exception retains in-flight reward");
            CompoundTag saved = queue.save(new CompoundTag());
            saved.getList("Jobs", 10).getCompound(0).putString("Dimension", "travail_smoke:missing");
            RewardDelivery missing = RewardDelivery.load(saved); missing.tick(level.getServer());
            check(missing.pendingItems() == 3, "missing dimension retained without loading");
            saved = queue.save(new CompoundTag()); saved.getList("Jobs", 10).getCompound(0).putDouble("X", 20_000_000);
            RewardDelivery unloaded = RewardDelivery.load(saved); unloaded.tick(level.getServer());
            check(unloaded.pendingItems() == 3, "unloaded chunk retained");
            while (queue.pendingItems() > 0) { queue = RewardDelivery.load(queue.save(new CompoundTag())); queue.tick(level.getServer()); }
            check(spawns.items.size() == 5, "all deferred items delivered exactly once in normal save/load");
            queue = new RewardDelivery();
            try (var slot = queue.reserve()) { slot.submit(level, position, new ItemStack(Items.APPLE), 130); }
            queue.tick(level.getServer()); check(queue.pendingItems() == 2, "stackable items use full stacks");
            TravailConfig.REWARD_OVERFLOW.set(TravailConfig.RewardOverflow.LIMIT); TravailConfig.REWARD_LIMIT.set(2);
            queue = new RewardDelivery(); try (var slot = queue.reserve()) { slot.submit(level, position, named, 100); }
            check(queue.pendingItems() == 2, "LIMIT policy caps stacks");
            TravailConfig.REWARD_QUEUE_CAPACITY.set(1);
            queue = new RewardDelivery();
            try (var slot = queue.reserve()) { check(slot != null && queue.reserve() == null, "reservation protects nested capacity"); }
            try (var slot = queue.reserve()) { check(slot != null, "unused reservation released"); slot.submit(level, position, named, 1); }
            check(queue.reserve() == null, "full queue refuses new reward");
            // 以满队列和仅可击杀一次的额度分别测试实际幻翼循环。
            level.getServer().overworld().getDataStorage().set("the_long_travail_rewards", queue);
            var player = new net.minecraftforge.common.util.FakePlayer(level,
                    new com.mojang.authlib.GameProfile(UUID.randomUUID(), "RewardSmoke"));
            player.setPos(position.x, position.y, position.z);
            for (int i=0;i<3;i++) { var phantom = new Phantom(EntityType.PHANTOM, level); phantom.setPos(position.x+i, position.y, position.z); level.addFreshEntity(phantom); phantoms.add(phantom); }
            var kill = TravailEvents.class.getDeclaredMethod("killNearbyPhantoms", net.minecraft.server.level.ServerPlayer.class); kill.setAccessible(true);
            kill.invoke(null, player); check(phantoms.stream().allMatch(Phantom::isAlive), "full queue pauses kills");
            TravailConfig.REWARD_QUEUE_CAPACITY.set(8); TravailConfig.PHANTOM_MAX_KILLS.set(1);
            level.getServer().overworld().getDataStorage().set("the_long_travail_rewards", new RewardDelivery());
            kill.invoke(null, player); check(phantoms.stream().filter(p -> !p.isAlive()).count() == 1, "one kill per scan");
            System.out.println("TRAVAIL_REWARDS_PASS: persisted NBT/count, tick budget, cancellations, missing/unloaded destinations, reservations, LIMIT, phantom backpressure/cap");
        } finally {
            MinecraftForge.EVENT_BUS.unregister(spawns);
            spawns.items.forEach(ItemEntity::discard); phantoms.forEach(Phantom::discard);
            level.getServer().overworld().getDataStorage().set("the_long_travail_rewards", original);
            for (int i=0;i<settings.size();i++) restore(settings.get(i), previous.get(i));
        }
    }
    @SuppressWarnings({"rawtypes", "unchecked"}) private static void restore(net.minecraftforge.common.ForgeConfigSpec.ConfigValue value, Object old) { value.set(old); }
}
