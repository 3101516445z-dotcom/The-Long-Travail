package com.thelongtravail.client;

import com.thelongtravail.network.InsertedEffectPacket;
import com.thelongtravail.runtime.effects.EffectInsertion;

import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

final class InsertedEffectClient {
    static void receive(InsertedEffectPacket packet) {
        var level = Minecraft.getInstance().level;
        if (level == null || !level.dimension().location().equals(packet.dimension())) return;
        var entity = level.getEntity(packet.id());
        if (!(entity instanceof LivingEntity target) || !target.getUUID().equals(packet.uuid()))
            return;
        var effect = MobEffectInstance.load(packet.effect());
        if (effect != null && EffectInsertion.writeVerified(target, effect))
            EffectInsertion.synchronize(target, effect);
    }

    private InsertedEffectClient() {}
}
