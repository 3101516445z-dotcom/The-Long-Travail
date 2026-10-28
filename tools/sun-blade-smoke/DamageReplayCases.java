package travail.smoke;

import com.thelongtravail.config.FlourishingItemsConfig;
import com.thelongtravail.farreach.SunChaser;
import com.thelongtravail.flourishing.EverlastingFlower;
import com.thelongtravail.registry.ModRegistry;

import net.minecraft.server.level.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.ItemStack;

import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICurio.DropRule;
import top.theillusivec4.curios.common.inventory.CurioStacksHandler;

import java.util.HashMap;
import java.util.function.BiConsumer;

/** 自然服务器 tick 下使用真实无穷花入队、续接和卸装失效。 */
final class DamageReplayCases {
    private final ServerLevel level;
    private final ServerPlayer actor;
    private final BiConsumer<Boolean, String> check;
    private final net.minecraftforge.common.ForgeConfigSpec.DoubleValue chance;
    private final double oldChance;
    private CurioStacksHandler slot;
    private Target target;
    private int start, roots, extras, previousTick = -1, rootsThisTick, stoppedAt = -1;
    private boolean finished;

    private final class Target extends Cow {
        Target() {
            super(EntityType.COW, level);
            setPos(4, 100, 0);
            setNoAi(true);
            setNoGravity(true);
            getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
            setHealth(1000);
        }

        @Override
        public boolean isOnFire() {
            return true;
        }

        @Override
        public boolean hurt(DamageSource source, float amount) {
            if (source.getEntity() == actor) {
                if (SunChaser.strike(source)) extras++;
                else {
                    roots++;
                    int tick = level.getServer().getTickCount();
                    if (previousTick != tick) {
                        previousTick = tick;
                        rootsThisTick = 0;
                    }
                    check.accept(++rootsThisTick == 1, "replay processes one root per tick");
                }
                check.accept(roots <= 20 && extras <= 20, "bounded replay fixture");
            }
            return super.hurt(source, amount);
        }
    }

    DamageReplayCases(ServerLevel level, ServerPlayer actor, BiConsumer<Boolean, String> check) {
        this.level = level;
        this.actor = actor;
        this.check = check;
        chance = FlourishingItemsConfig.NUMBERS.get("everlasting_flower.witness.repeatChance");
        oldChance = chance.get();
    }

    void start() {
        chance.set(1d);
        var inv = CuriosApi.getCuriosInventory(actor).resolve().orElseThrow();
        var slots = new HashMap<>(inv.getCurios());
        slot = new CurioStacksHandler(inv, "chain_test", 1, true, false, true, DropRule.DEFAULT);
        slots.put("chain_test", slot);
        inv.setCurios(slots);
        slot.getStacks().setStackInSlot(0, new ItemStack(ModRegistry.EVERLASTING_FLOWER.get()));
        check.accept(EverlastingFlower.phase(actor) == 2, "actual flower witness active");
        target = new Target();
        level.addFreshEntity(target);
        start = level.getServer().getTickCount();
        target.hurt(actor.damageSources().playerAttack(actor), 10);
    }

    void tick() {
        if (target == null || finished) return;
        int elapsed = level.getServer().getTickCount() - start;
        if (stoppedAt < 0 && roots >= 8) {
            check.accept(roots == extras, "each replay gets one fresh Sun claim");
            slot.getStacks().setStackInSlot(0, ItemStack.EMPTY);
            stoppedAt = roots;
        }
        if (elapsed >= 20) {
            try {
                check.accept(
                        stoppedAt >= 8 && roots == stoppedAt,
                        "unequip invalidates guaranteed continuation");
                check.accept(roots == extras, "no ordinary re-enqueue from Sun strike");
            } finally {
                EverlastingFlower.unequipped(actor);
                chance.set(oldChance);
                target.discard();
                finished = true;
            }
        }
    }

    void verify() {
        check.accept(finished, "natural replay chain completed");
    }
}
