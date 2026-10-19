package com.thelongtravail.valley;

import com.thelongtravail.TheLongTravail;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.Vec3;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = TheLongTravail.MODID)
public final class ExecutionLoot {
    private static final ThreadLocal<Scope> CURRENT = new ThreadLocal<>();

    public static final class State {
        boolean observed, handled;
        ResourceLocation table;
        net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension;
        Vec3 position;
    }

    public static final class Scope implements AutoCloseable {
        final Scope previous;
        final LivingEntity target;
        final State state;
        final java.util.Map<ItemEntity, Entity> sources;

        private Scope(LivingEntity target) {
            previous = CURRENT.get();
            sources = previous == null ? new java.util.IdentityHashMap<>() : previous.sources;
            this.target = target;
            State retained = ((ExecutionBody) target).travail$executionLoot();
            state =
                    previous != null && previous.target == target
                            ? previous.state
                            : retained != null ? retained : new State();
            CURRENT.set(this);
        }

        public void close() {
            if (previous == null) CURRENT.remove();
            else CURRENT.set(previous);
        }
    }

    public static Scope observe(LivingEntity target) {
        return new Scope(target);
    }

    public static void source(Entity owner, ItemEntity item) {
        Scope scope = CURRENT.get();
        if (scope != null && item != null) scope.sources.putIfAbsent(item, owner);
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent(
            priority = net.minecraftforge.eventbus.api.EventPriority.LOWEST,
            receiveCanceled = true)
    public static void drops(net.minecraftforge.event.entity.living.LivingDropsEvent event) {
        if (CURRENT.get() == null) return;
        for (ItemEntity item : event.getDrops()) source(event.getEntity(), item);
    }

    // 只统计实际成功加入世界的掉落；仅生成或捕获到列表中的掉落仍可能被取消。
    public static void admitted(Entity entity) {
        Scope scope = CURRENT.get();
        if (scope == null || !(entity instanceof ItemEntity item) || item.getItem().isEmpty()) return;
        Entity owner = scope.sources.get(item);
        if (owner != null) {
            while (scope != null && scope.target != owner) scope = scope.previous;
        }
        if (scope != null && !scope.state.handled && entity.level() == scope.target.level())
            scope.state.observed = true;
    }

    public static void arm(LivingEntity target) {
        if (target instanceof Player) return;
        var body = (ExecutionBody) target;
        if (body.travail$executionLoot() != null) return;
        Scope scope = CURRENT.get();
        State state = scope != null && scope.target == target ? scope.state : new State();
        body.travail$executionLoot(state);
        try {
            state.position = target.position();
            state.dimension = target.level().dimension();
            state.table = target.getLootTable();
        } catch (RuntimeException ex) {
            state.handled = true;
            TheLongTravail.LOGGER.warn("Execution loot table lookup failed for {}", target.getUUID(), ex);
        }
    }

    public static void abort(LivingEntity target) {
        State state = ((ExecutionBody) target).travail$executionLoot();
        if (state != null) state.handled = true;
    }

    public static void finish(LivingEntity target) {
        if (target instanceof Player
                || !(target.level() instanceof ServerLevel level)
                || !level.getServer().isSameThread()
                || !FinalExecution.terminated(target)) return;
        State state = ((ExecutionBody) target).travail$executionLoot();
        if (state == null || state.handled) return;
        // 解析和生成掉落前先提交标记，防止部分失败或回调重入造成重复掉落。
        state.handled = true;
        if (state.observed
                || state.table == null
                || state.position == null
                || !level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) return;
        try {
            var dropLevel = level.getServer().getLevel(state.dimension);
            if (dropLevel == null || !dropLevel.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) return;
            var result = ExecutionLootTable.collect(state.table, level.getServer().getLootData()::getLootTable);
            int spawned = 0;
            for (Item item : result.items()) {
                var drop =
                        new ItemEntity(
                                dropLevel,
                                state.position.x,
                                state.position.y,
                                state.position.z,
                                new ItemStack(item, 1));
                drop.setDefaultPickUpDelay();
                if (dropLevel.addFreshEntity(drop)) spawned++;
            }
            TheLongTravail.LOGGER.info(
                    "Execution loot fallback uuid={} table={} items={} limited={} skipped={}",
                    target.getUUID(),
                    state.table,
                    spawned,
                    result.limited(),
                    result.skipped());
        } catch (RuntimeException ex) {
            TheLongTravail.LOGGER.warn("Execution loot fallback failed; not retried for {}", target.getUUID(), ex);
        }
    }

    private ExecutionLoot() {}
}
