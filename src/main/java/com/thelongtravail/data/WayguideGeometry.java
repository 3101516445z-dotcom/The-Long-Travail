package com.thelongtravail.data;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public final class WayguideGeometry {
    public static Vec3 destination(Vec3 feet, BlockPos target) {
        double dx = target.getX() + 0.5 - feet.x, dz = target.getZ() + 0.5 - feet.z;
        double distance = Math.hypot(dx, dz);
        double scale = distance < 1.0 ? 0.0 : Math.min(12.0, distance) / distance;
        return new Vec3(feet.x + dx * scale, feet.y + 3.0, feet.z + dz * scale);
    }

    public static final class Spiral {
        private final int radius;
        private int x, z, dx, dz = -1;
        private boolean finished;

        public Spiral(int radius) {
            this.radius = Math.max(0, radius);
        }

        public int x() {
            return x;
        }

        public int z() {
            return z;
        }

        public boolean finished() {
            return finished;
        }

        public void advance() {
            if (x == radius && z == -radius) {
                finished = true;
                return;
            }
            if (x == z || (x < 0 && x == -z) || (x > 0 && x == 1 - z)) {
                int previousDx = dx;
                dx = -dz;
                dz = previousDx;
            }
            x += dx;
            z += dz;
        }
    }

    private WayguideGeometry() {}
}
