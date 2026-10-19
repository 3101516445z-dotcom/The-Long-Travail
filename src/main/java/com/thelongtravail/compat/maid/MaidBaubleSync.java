package com.thelongtravail.compat.maid;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.network.NetworkHandler;
import com.github.tartaricacid.touhoulittlemaid.network.message.SyncBaubleMessage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import java.util.*;

// 所有苦旅女仆饰品均按槽位同步；不能将筛选后的列表作为 fullSync 发送。
public final class MaidBaubleSync {
    private static final Map<EntityMaid, CompoundTag> PREVIOUS = new WeakHashMap<>();

    public static CompoundTag snapshot(EntityMaid maid) {
        CompoundTag out = new CompoundTag();
        var slots = maid.getMaidBauble();
        for (int i = 0; i < slots.getSlots(); i++) {
            ItemStack s = slots.getStackInSlot(i);
            if (MaidAccessories.items().contains(s.getItem())) out.put(Integer.toString(i), s.save(new CompoundTag()));
        }
        return out;
    }

    public static List<SyncBaubleMessage> changes(EntityMaid maid, CompoundTag previous, CompoundTag current) {
        Set<String> keys = new TreeSet<>();
        if (previous != null) keys.addAll(previous.getAllKeys());
        if (current != null) keys.addAll(current.getAllKeys());
        List<SyncBaubleMessage> out = new ArrayList<>();
        var slots = maid.getMaidBauble();
        for (String key : keys) {
            if (previous != null && current != null && Objects.equals(previous.get(key), current.get(key))) continue;
            int slot = Integer.parseInt(key);
            if (slot >= 0 && slot < slots.getSlots())
                out.add(SyncBaubleMessage.partialSync(maid.getId(), slot, slots.getStackInSlot(slot).copy()));
        }
        return out;
    }

    public static void watch(EntityMaid maid) {
        if (!maid.level().isClientSide) PREVIOUS.putIfAbsent(maid, new CompoundTag());
    }

    @SubscribeEvent
    public void end(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        for (var maid : new ArrayList<>(PREVIOUS.keySet())) {
            if (maid.isRemoved()) {
                PREVIOUS.remove(maid);
                continue;
            }
            CompoundTag current = snapshot(maid);
            for (var message : changes(maid, PREVIOUS.get(maid), current))
                NetworkHandler.sendToTrackingEntity(message, maid);
            PREVIOUS.put(maid, current);
        }
    }

    @SubscribeEvent
    public void tracking(PlayerEvent.StartTracking e) {
        if (e.getTarget() instanceof EntityMaid maid
                && e.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            for (var message : changes(maid, null, snapshot(maid)))
                NetworkHandler.CHANNEL.send(
                        net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player), message);
            watch(maid);
        }
    }

    @SubscribeEvent
    public void stop(net.minecraftforge.event.server.ServerStoppedEvent e) {
        PREVIOUS.clear();
    }
}
