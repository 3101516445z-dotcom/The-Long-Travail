package com.thelongtravail.client;

import com.thelongtravail.network.ExecutionTerminationPacket;
import com.thelongtravail.valley.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;

@Mod.EventBusSubscriber(modid = "the_long_travail", value = Dist.CLIENT)
public final class ExecutionTerminationClient {
    private static final ExecutionReceipts RECEIPTS = new ExecutionReceipts();

    public static boolean suppress(Entity entity) {
        return suppressIncoming(entity, entity.getId());
    }

    public static boolean suppressIncoming(Entity entity, int incomingId) {
        if (!(entity instanceof LivingEntity living) || entity instanceof Player) return false;
        if (ExecutionCleanup.quarantined(entity)) return true;
        var lease = RECEIPTS.lease(entity.level(), incomingId, entity.getUUID());
        if (lease == null) return false;
        lease.restored();
        var body = (ExecutionBody) living;
        body.travail$executionLease(lease);
        body.travail$executionLocked(true);
        body.travail$executionTerminated(true);
        ExecutionWatch.track(living);
        return true;
    }

    public static void receive(ExecutionTerminationPacket packet) {
        var level = Minecraft.getInstance().level;
        if (level == null || !level.dimension().location().equals(packet.dimension())) return;
        RECEIPTS.remember(level, packet);
        var lease = RECEIPTS.lease(level, packet.id(), packet.uuid());
        if (lease == null) return;
        var entity = level.getEntity(packet.id());
        if (!(entity instanceof LivingEntity living)
                || entity instanceof Player
                || !entity.getUUID().equals(packet.uuid())) return;
        var body = (ExecutionBody) living;
        body.travail$executionLease(lease);
        body.travail$executionLocked(true);
        body.travail$cleanupPending(true);
        ExecutionWatch.track(living);
        boolean notify = !body.travail$executionTerminated() && living.isAddedToWorld();
        try {
            ExecutionCleanup.rawDetach(living);
            if (notify) ExecutionCleanup.notifyRemoval(living);
            body.travail$executionTerminated(true);
            body.travail$cleanupPending(false);
        } catch (RuntimeException ex) {
            com.thelongtravail.TheLongTravail.LOGGER.warn(
                    "Client execution cleanup deferred for " + living.getUUID(), ex);
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        RECEIPTS.probe(level, ExecutionTerminationClient::suppress);
        ExecutionWatch.poll(level);
    }

    @SubscribeEvent
    public static void unload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) RECEIPTS.forget(event.getLevel());
    }

    private ExecutionTerminationClient() {}
}
