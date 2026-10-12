package com.thelongtravail.client;

import com.thelongtravail.network.TimeStopCuePacket;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

public final class TimeStopCue {
    private static final Set<Particle> PARTICLES = Collections.newSetFromMap(new WeakHashMap<>());

    public static void play(TimeStopCuePacket packet) {
        var minecraft = Minecraft.getInstance();
        var level = minecraft.level;
        if (level == null || !level.dimension().location().equals(packet.dimension())) {
            return;
        }
        Vec3 position = packet.position();
        for (int i = 0; i < 24; i++) {
            double angle = Math.PI * 2 * i / 24;
            double x = Math.cos(angle);
            double z = Math.sin(angle);
            add(position.add(x * 0.8, 0, z * 0.8), x * 0.025, 0.01, z * 0.025);
        }
        for (int i = 0; i < 6; i++) {
            double angle = Math.PI * 2 * i / 6;
            add(
                    position.add(Math.cos(angle) * 0.3, 0, Math.sin(angle) * 0.3),
                    0,
                    0.04 + i * 0.004,
                    0);
        }
        level.playLocalSound(
                position.x,
                position.y,
                position.z,
                SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS,
                0.55F,
                0.8F,
                false);
    }

    private static void add(Vec3 position, double dx, double dy, double dz) {
        Particle particle =
                Minecraft.getInstance()
                        .particleEngine
                        .createParticle(
                                ParticleTypes.END_ROD,
                                position.x,
                                position.y,
                                position.z,
                                dx,
                                dy,
                                dz);
        if (particle != null) {
            particle.setLifetime(12);
            PARTICLES.add(particle);
        }
    }

    public static boolean exempt(Particle particle) {
        return PARTICLES.contains(particle);
    }

    public static void clear() {
        PARTICLES.clear();
    }

    private TimeStopCue() {}
}
