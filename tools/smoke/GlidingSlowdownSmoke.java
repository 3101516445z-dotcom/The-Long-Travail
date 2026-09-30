package travail.smoke;

import com.mojang.authlib.GameProfile;
import com.thelongtravail.TravailAspect;
import com.thelongtravail.config.TravailConfig;
import com.thelongtravail.data.AltitudePenalty;
import com.thelongtravail.data.LongTravailData;
import com.thelongtravail.registry.ModRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.common.inventory.CurioStacksHandler;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Exercises the transformed vanilla travel method, not a copy of its flight equations. */
public final class GlidingSlowdownSmoke {
    private static final class Flyer extends FakePlayer {
        Vec3 requested = Vec3.ZERO;
        boolean wall;
        boolean realMovement;
        float impact;
        Flyer(ServerLevel level) { super(level, new GameProfile(UUID.randomUUID(), "GlideTest")); }
        @Override public boolean isControlledByLocalInstance() { return true; }
        @Override public void move(MoverType type, Vec3 motion) {
            requested = motion;
            if (realMovement) { super.move(type, motion); return; }
            // Isolate flight physics from terrain; optionally emulate a fully blocked X axis.
            setPos(position().add(wall ? new Vec3(0, motion.y, motion.z) : motion));
            horizontalCollision = wall && motion.x != 0;
            if (horizontalCollision) setDeltaMovement(getDeltaMovement().multiply(0, 1, 1));
        }
        @Override public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
            impact = amount;
            return true;
        }
    }
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    private static void near(Vec3 actual, Vec3 expected, String message) {
        check(actual.distanceTo(expected) < 0.00001, message + ": " + actual + " != " + expected);
    }
    private static ItemStack equip(Flyer player, boolean witness) {
        var diary = new ItemStack(ModRegistry.LONG_TRAVAIL.get());
        LongTravailData.initialize(diary, player);
        LongTravailData.setWitness(diary, TravailAspect.BOUNDLESS, witness);
        var inventory = CuriosApi.getCuriosInventory(player).resolve().orElseThrow();
        var slots = new CurioStacksHandler(inventory, "travel_diary", 1, true, false, true, ICurio.DropRule.DEFAULT);
        inventory.setCurios(new HashMap<>(Map.of("travel_diary", slots)));
        slots.getStacks().setStackInSlot(0, diary);
        return diary;
    }
    private static void step(Flyer player) {
        player.tickCount++;
        player.travel(Vec3.ZERO);
    }
    public static void run(ServerLevel level) {
        double oldReduction = TravailConfig.BOUNDLESS_SPEED_REDUCTION.get();
        double oldHeight = TravailConfig.BOUNDLESS_HEIGHT_THRESHOLD.get();
        try {
            TravailConfig.BOUNDLESS_HEIGHT_THRESHOLD.set(64.0D);
            TravailConfig.BOUNDLESS_SPEED_REDUCTION.set(0.8D);
            AltitudePenalty.configReloaded();
            var normal = new Flyer(level);
            var slow = new Flyer(level);
            equip(normal, true);
            var diary = equip(slow, false);
            var normalRocket = new net.minecraft.world.entity.projectile.FireworkRocketEntity(level,
                    new ItemStack(net.minecraft.world.item.Items.FIREWORK_ROCKET), normal);
            var slowRocket = new net.minecraft.world.entity.projectile.FireworkRocketEntity(level,
                    new ItemStack(net.minecraft.world.item.Items.FIREWORK_ROCKET), slow);
            for (var player : new Flyer[]{normal, slow}) {
                player.setPos(0, 300, 0);
                player.startFallFlying();
                player.setDeltaMovement(1.5, -0.1, 0.5);
            }
            for (int i = 0; i < 80; i++) {
                if (i >= 20 && i < 30) {
                    normalRocket.tick();
                    slowRocket.tick();
                }
                for (var player : new Flyer[]{normal, slow}) {
                    player.setXRot(i < 40 ? -15 : 25);
                    if (i == 20) player.setDeltaMovement(player.getDeltaMovement().add(1, 0.2, 0.5));
                    step(player);
                }
                near(slow.requested, normal.requested.scale(0.2), "stable per-frame displacement " + i);
                near(slow.getDeltaMovement(), normal.getDeltaMovement(), "no compounded damping " + i);
            }
            for (double reduction : new double[]{0, 0.5, 1}) {
                TravailConfig.BOUNDLESS_SPEED_REDUCTION.set(reduction);
                AltitudePenalty.configReloaded();
                step(slow);
                near(slow.requested, slow.getDeltaMovement().scale(1 - reduction), "config " + reduction);
            }
            TravailConfig.BOUNDLESS_SPEED_REDUCTION.set(0.8D);
            AltitudePenalty.configReloaded();
            slow.setPos(0, 64, 0);
            step(slow);
            near(slow.requested, slow.getDeltaMovement(), "threshold excludes equality");
            slow.setPos(0, 300, 0);
            LongTravailData.setWitness(diary, TravailAspect.BOUNDLESS, true);
            step(slow);
            near(slow.requested, slow.getDeltaMovement(), "witness disables slowdown");
            LongTravailData.setWitness(diary, TravailAspect.BOUNDLESS, false);
            for (var player : new Flyer[]{normal, slow}) {
                player.setPos(0, 300, 0);
                player.setXRot(0);
                player.setDeltaMovement(3, 0, 0);
                player.wall = true;
                step(player);
                check(player.getDeltaMovement().x == 0, "blocked velocity stays cleared");
            }
            check(Math.abs(slow.impact - ((normal.impact + 3) * 0.2 - 3)) < 0.0001,
                    "wall damage scales speed loss before damage threshold");
            var wallBase = new net.minecraft.core.BlockPos(1, 300, 0);
            var oldBase = level.getBlockState(wallBase);
            var oldTop = level.getBlockState(wallBase.above());
            try {
                level.setBlockAndUpdate(wallBase, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
                level.setBlockAndUpdate(wallBase.above(), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
                slow.realMovement = true;
                slow.setPos(0.5, 300, 0.5);
                slow.setDeltaMovement(3, 0, 0);
                step(slow);
                check(slow.horizontalCollision && slow.getX() < 1 && slow.getDeltaMovement().x == 0,
                        "real Entity.move resolves stone collision without restoring blocked velocity");
            } finally {
                level.setBlockAndUpdate(wallBase, oldBase);
                level.setBlockAndUpdate(wallBase.above(), oldTop);
            }
            slow.realMovement = false;
            slow.wall = false;
            CuriosApi.getCuriosInventory(slow).resolve().orElseThrow().getStacksHandler("travel_diary")
                    .orElseThrow().getStacks().setStackInSlot(0, ItemStack.EMPTY);
            slow.setPos(0, 300, 0);
            slow.startFallFlying();
            step(slow);
            near(slow.requested, slow.getDeltaMovement(), "unequipped flight remains vanilla");
            System.out.println("TRAVAIL_GLIDING_PASS: 80 travel frames, pitch, real rockets, impulse, 0/50/80/100% reduction, height, witness, unequip, impact scaling, real stone collision");
        } finally {
            TravailConfig.BOUNDLESS_SPEED_REDUCTION.set(oldReduction);
            TravailConfig.BOUNDLESS_HEIGHT_THRESHOLD.set(oldHeight);
            AltitudePenalty.configReloaded();
        }
    }
}
