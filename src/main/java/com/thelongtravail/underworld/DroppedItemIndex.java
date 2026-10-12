package com.thelongtravail.underworld;

import com.thelongtravail.TheLongTravail;
import com.thelongtravail.data.HotPathMetrics.Counter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

// 仅在服务端主线程维护；索引实体身份，校正时始终读取当前物品。
@Mod.EventBusSubscriber(modid = TheLongTravail.MODID)
public final class DroppedItemIndex {
    private static final Map<ServerLevel, State> WORLDS = new IdentityHashMap<>();

    private static final class State {
        final Set<ItemEntity> items = Collections.newSetFromMap(new IdentityHashMap<>());
        boolean seeded;
    }

    private static boolean present(ServerLevel level, ItemEntity item) {
        return !item.isRemoved()
                && item.level() == level
                && level.getEntity(item.getUUID()) == item;
    }

    public static List<ItemEntity> snapshot(ServerLevel level) {
        State state = WORLDS.computeIfAbsent(level, ignored -> new State());
        if (!state.seeded) {
            for (var entity : level.getAllEntities()) {
                Counter.STORAGE_INDEX_SEED_VISITS.add(1);
                if (entity instanceof ItemEntity item) state.items.add(item);
            }
            state.seeded = true;
        }
        state.items.removeIf(item -> !present(level, item));
        return List.copyOf(state.items);
    }

    // 在原版完成可见实体索引的增删后更新，兼顾区块隐藏和重新可见的情况，不只依赖实体首次加入事件。
    public static void added(ItemEntity item) {
        if (item.level() instanceof ServerLevel level && present(level, item))
            WORLDS.computeIfAbsent(level, ignored -> new State()).items.add(item);
    }

    public static void removed(ItemEntity item) {
        if (!(item.level() instanceof ServerLevel level) || level.getEntity(item.getUUID()) == item)
            return;
        State state = WORLDS.get(level);
        if (state != null) state.items.remove(item);
    }

    @SubscribeEvent
    public static void unloaded(LevelEvent.Unload event) {
        WORLDS.remove(event.getLevel());
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        WORLDS.clear();
    }

    private DroppedItemIndex() {}
}
