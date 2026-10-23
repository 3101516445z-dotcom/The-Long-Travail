package com.thelongtravail.boundless;

import com.thelongtravail.network.PrinceFlightPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public final class PrinceFlightPermission {
    private Player owner;
    private ResourceLocation dimension;
    private boolean active;

    public void accept(Player player, PrinceFlightPacket packet) {
        tick(player);
        if (player == null
                || !player.isAlive()
                || player.getId() != packet.entityId()
                || !player.getUUID().equals(packet.playerId())
                || !player.level().dimension().location().equals(packet.dimension())) return;
        boolean previous = active;
        owner = player;
        dimension = packet.dimension();
        active = packet.active();
        // 客户端首次收到授权前，登录快照可能已带有旧的飞行权限。
        if ((previous || packet.revoke())
                && !active
                && !player.isCreative()
                && !player.isSpectator()) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
        }
        tick(player);
    }

    public void tick(Player player) {
        if (player == null
                || player != owner
                || !player.isAlive()
                || !player.level().dimension().location().equals(dimension)) {
            clear();
            return;
        }
        if (active && !player.isCreative() && !player.isSpectator())
            player.getAbilities().mayfly = true;
    }

    public void clear() {
        owner = null;
        dimension = null;
        active = false;
    }
}
