package com.thelongtravail.client;

import com.thelongtravail.network.LanternPacket;
import com.thelongtravail.valley.LanternSources;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

@Mod.EventBusSubscriber(modid = "the_long_travail", value = Dist.CLIENT)
public final class LanternLightingClient {
    private record Status(ResourceLocation dimension, long received) {}

    private static final Map<UUID, Status> ACTIVE = new HashMap<>();
    private static final Map<UUID, ItemEntity> droppedItems = new HashMap<>();
    private static final Map<UUID, net.minecraft.world.entity.Entity> maids = new HashMap<>();
    private static final List<ItemEntity> droppedLights = new ArrayList<>();
    private static final LanternRebuildScheduler REBUILDS = new LanternRebuildScheduler();
    private static final Map<UUID, LanternLighting.Source> current = new HashMap<>();
    private static volatile LanternLightSnapshot lights = LanternLightSnapshot.EMPTY;
    private static ClientLevel world;

    public static LanternLighting.Source[] snapshot() {
        return lights.sources();
    }

    public static LanternLightSnapshot lightingSnapshot() {
        return lights;
    }

    public static void receive(LanternPacket p) {
        if (p.active()) ACTIVE.put(p.player(), new Status(p.dimension(), System.nanoTime()));
        else ACTIVE.remove(p.player());
    }

    @SubscribeEvent
    public static void joined(net.minecraftforge.event.entity.EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide) return;
        var entity = event.getEntity();
        if (entity instanceof ItemEntity item) droppedItems.put(item.getUUID(), item);
        if (com.thelongtravail.compat.maid.MaidAccessories.isMaid(entity)) maids.put(entity.getUUID(), entity);
    }

    @SubscribeEvent
    public static void left(net.minecraftforge.event.entity.EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide && event.getEntity() instanceof ItemEntity item)
            droppedItems.remove(item.getUUID(), item);
        if (event.getLevel().isClientSide) maids.remove(event.getEntity().getUUID(), event.getEntity());
    }

    private static void collectDrops(ClientLevel level) {
        droppedLights.clear();
        droppedItems.values().removeIf(item -> item.isRemoved() || item.level() != level);
        for (var item : droppedItems.values()) if (LanternSources.dropped(item)) droppedLights.add(item);
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && Minecraft.getInstance().level != null)
            collectDrops(Minecraft.getInstance().level);
    }

    @SubscribeEvent
    public static void logout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        ACTIVE.clear();
        droppedItems.clear();
        maids.clear();
        droppedLights.clear();
        REBUILDS.clear();
        current.clear();
        lights = LanternLightSnapshot.EMPTY;
        world = null;
    }

    @SubscribeEvent
    public static void frame(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            droppedItems.clear();
            maids.clear();
            droppedLights.clear();
            lights = LanternLightSnapshot.EMPTY;
            REBUILDS.clear();
            current.clear();
            world = null;
            return;
        }
        if (world != mc.level) {
            world = mc.level;
            REBUILDS.clear();
            lights = LanternLightSnapshot.EMPTY;
            droppedItems.clear();
            maids.clear();
            for (var entity : world.entitiesForRendering()) {
                if (entity instanceof ItemEntity item) droppedItems.put(item.getUUID(), item);
                if (com.thelongtravail.compat.maid.MaidAccessories.isMaid(entity)) maids.put(entity.getUUID(), entity);
            }
            collectDrops(world);
        }
        long now = System.nanoTime();
        if (mc.isPaused()) ACTIVE.replaceAll((id, s) -> new Status(s.dimension, now));
        else ACTIVE.values().removeIf(s -> now - s.received > 10_000_000_000L);
        current.clear();
        float partial = mc.isPaused() ? mc.getFrameTime() : event.renderTickTime;
        for (var p : mc.level.players()) {
            Status status = ACTIVE.get(p.getUUID());
            boolean equipped = status != null && status.dimension.equals(mc.level.dimension().location());
            if (!p.isAlive() || p.isSpectator() || !equipped && !LanternSources.held(p)) continue;
            double x = Mth.lerp(partial, p.xo, p.getX()),
                    y = Mth.lerp(partial, p.yo, p.getY()) + p.getBbHeight() * .55,
                    z = Mth.lerp(partial, p.zo, p.getZ());
            current.put(p.getUUID(), new LanternLighting.Source(x, y, z));
        }
        maids.values().removeIf(e -> e.isRemoved() || e.level() != mc.level);
        for (var maid : maids.values()) {
            if (!com.thelongtravail.compat.maid.MaidAccessories.lantern(maid)) continue;
            current.put(
                    maid.getUUID(),
                    new LanternLighting.Source(
                            Mth.lerp(partial, maid.xo, maid.getX()),
                            Mth.lerp(partial, maid.yo, maid.getY()) + maid.getBbHeight() * .55,
                            Mth.lerp(partial, maid.zo, maid.getZ())));
        }
        for (var item : droppedLights) {
            if (item.level() != mc.level || !LanternSources.dropped(item)) continue;
            current.put(
                    item.getUUID(),
                    new LanternLighting.Source(
                            Mth.lerp(partial, item.xo, item.getX()),
                            Mth.lerp(partial, item.yo, item.getY()) + item.getBbHeight() * .5,
                            Mth.lerp(partial, item.zo, item.getZ())));
        }
        var batch = REBUILDS.update(now, current);
        if (batch == null) return;
        lights = batch.snapshot();
        for (long packed : batch.dirtySections()) {
            int x = SectionPos.x(packed), y = SectionPos.y(packed), z = SectionPos.z(packed);
            if (y >= mc.level.getMinSection() && y < mc.level.getMaxSection()) {
                com.thelongtravail.data.HotPathMetrics.Counter.LANTERN_DIRTY_SECTIONS.add(1);
                mc.levelRenderer.setSectionDirty(x, y, z);
            }
        }
    }

    public static int light(double x, double y, double z, int packed) {
        return lights.merge(x, y, z, packed);
    }

    private LanternLightingClient() {}
}
