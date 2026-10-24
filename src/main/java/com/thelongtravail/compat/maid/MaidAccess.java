package com.thelongtravail.compat.maid;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Native maid access shared by gameplay adapters. Load only behind the optional-mod boundary. */
public final class MaidAccess {
    static final String IDENTITY_KEY = "TravailMaidIdentity";

    public static ItemStack equipped(EntityMaid m, Item item) {
        if (!m.isAlive() || m.isRemoved()) return ItemStack.EMPTY;
        var slots = m.getMaidBauble();
        for (int i = 0; i < slots.getSlots(); i++) {
            var stack = slots.getStackInSlot(i);
            if (stack.is(item)) return stack;
        }
        return ItemStack.EMPTY;
    }

    public static ServerPlayer owner(EntityMaid m) {
        var server = m.getServer();
        var id = m.getOwnerUUID();
        var p = server == null || id == null ? null : server.getPlayerList().getPlayer(id);
        return p == null || p.hasDisconnected() ? null : p;
    }

    /** Returns the persistent compatibility identity, creating it only when absent. */
    public static UUID identity(LivingEntity e) {
        var tag = e.getPersistentData();
        if (!tag.hasUUID(IDENTITY_KEY)) tag.putUUID(IDENTITY_KEY, UUID.randomUUID());
        return tag.getUUID(IDENTITY_KEY);
    }

    private MaidAccess() {}
}
