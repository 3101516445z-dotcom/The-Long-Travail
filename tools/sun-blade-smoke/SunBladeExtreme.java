package travail.smoke;

import com.thelongtravail.TravailAspect;
import com.thelongtravail.combat.*;
import com.thelongtravail.data.LongTravailData;
import com.thelongtravail.farreach.SunChaser;
import com.thelongtravail.registry.ModRegistry;
import com.thelongtravail.underworld.Kagotsurube;

import net.minecraft.server.level.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;

import top.theillusivec4.curios.common.inventory.CurioStacksHandler;

import java.nio.file.*;
import java.util.*;
import java.util.function.BiConsumer;

/** 测试专用：记录真实 hurt 入口与返回，独立核对预期伤害和次数。 */
final class SunBladeExtreme {
    final ServerLevel level;
    final ServerPlayer player;
    final ItemStack diary;
    final CurioStacksHandler belt;
    final BiConsumer<Boolean, String> check;
    final List<String> trace = new ArrayList<>();

    SunBladeExtreme(
            ServerLevel l,
            ServerPlayer p,
            ItemStack d,
            CurioStacksHandler b,
            BiConsumer<Boolean, String> c) {
        level = l;
        player = p;
        diary = d;
        belt = b;
        check = c;
    }

    final class Target extends Cow {
        int calls, extras, depth, maxDepth;
        float lastExtraLoss;

        Target() {
            super(EntityType.COW, level);
            setPos(8, 100, 0);
            setNoAi(true);
            setNoGravity(true);
            getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
            setHealth(1000);
        }

        @Override
        public boolean hurt(DamageSource source, float amount) {
            int id = ++calls;
            depth++;
            maxDepth = Math.max(depth, maxDepth);
            boolean extra = SunChaser.strike(source);
            if (extra) extras++;
            float hp = getHealth(), abs = getAbsorptionAmount();
            try {
                return super.hurt(source, amount);
            } finally {
                if (calls < 50)
                    trace.add(
                            id
                                    + ",depth="
                                    + depth
                                    + ",type="
                                    + source.getMsgId()
                                    + ",extra="
                                    + extra
                                    + ",input="
                                    + amount
                                    + ",hpLoss="
                                    + (hp - getHealth())
                                    + ",absLoss="
                                    + (abs - getAbsorptionAmount()));
                if (extra) lastExtraLoss = hp - getHealth();
                depth--;
            }
        }
    }

    void run() throws Exception {
        player.getInventory().setItem(0, new ItemStack(ModRegistry.SUN_CHASER.get()));
        belt.getStacks().setStackInSlot(0, new ItemStack(ModRegistry.KAGOTSURUBE.get()));
        LongTravailData.setWitness(diary, TravailAspect.FAR_REACH, true);
        LongTravailData.setWitness(diary, TravailAspect.UNDERWORLD, true);
        Kagotsurube.refresh(player);
        try {
            var crit =
                    net.minecraftforge.registries.ForgeRegistries.ATTRIBUTES.getValue(
                            new net.minecraft.resources.ResourceLocation(
                                    "attributeslib", "crit_chance"));
            if (crit != null && player.getAttribute(crit) != null) {
                var attribute = player.getAttribute(crit);
                double original = attribute.getBaseValue();
                try {
                    for (double chance : new double[] {0, 1}) {
                        attribute.setBaseValue(chance);
                        Target probe = new Target();
                        probe.setSecondsOnFire(100);
                        probe.hurt(player.damageSources().playerAttack(player), 1);
                        String observation =
                                "APOTH_CRIT_CONTROL chance="
                                        + attribute.getValue()
                                        + " extraInput=25 extraHealthLoss="
                                        + probe.lastExtraLoss;
                        trace.add(observation);
                        System.out.println(observation);
                        check.accept(
                                Math.abs(probe.lastExtraLoss - 25) < .001,
                                "external crit must not amplify sun " + observation);
                    }
                } finally {
                    attribute.setBaseValue(original);
                }
            }
            for (int count : new int[] {1, 10, 100, 1000}) {
                Target t = new Target();
                t.setSecondsOnFire(100);
                long start = System.nanoTime();
                for (int i = 0; i < count; i++) {
                    t.setHealth(1000);
                    t.hurt(player.damageSources().playerAttack(player), 1);
                    check.accept(
                            Math.abs(t.getHealth() - 974) < .001,
                            "independent root exact damage " + count + ":" + i);
                }
                check.accept(
                        t.calls == count * 2 && t.extras == count && t.maxDepth == 2,
                        "bounded actual call tree " + count);
                check.accept(
                        t.getEffect(MobEffects.WITHER).getDuration() == count * 200,
                        "exact cumulative wither " + count);
                trace.add("burst=" + count + ",ns=" + (System.nanoTime() - start));
            }
            Random random = new Random(20261028L);
            for (int i = 0; i < 2000; i++) {
                Target t = new Target();
                boolean burning = random.nextBoolean();
                if (burning) t.setSecondsOnFire(100);
                float amount = 1 + random.nextInt(50), absorption = random.nextInt(101);
                t.setAbsorptionAmount(absorption);
                DamageSource source =
                        switch (i % 4) {
                            case 0 -> player.damageSources().playerAttack(player);
                            case 1 ->
                                    player.damageSources().arrow(new Arrow(level, player), player);
                            case 2 -> player.damageSources().indirectMagic(player, player);
                            default -> player.damageSources().thorns(player);
                        };
                DamageSource converted = Kagotsurube.convert(t, source);
                check.accept(
                        converted.is(DirectStrikeDamage.TYPE)
                                && DamageIdentity.original(converted) == source,
                        "source identity matrix " + i);
                t.hurt(source, amount);
                float sum = amount + (burning ? 25 : 0);
                check.accept(
                        Math.abs(t.getHealth() - (1000 - Math.max(0, sum - absorption))) < .001,
                        "independent absorption oracle " + i);
                check.accept(
                        Math.abs(t.getAbsorptionAmount() - Math.max(0, absorption - sum)) < .001,
                        "absorption remainder " + i);
                check.accept(
                        t.calls == (burning ? 2 : 1) && t.extras == (burning ? 1 : 0),
                        "source matrix finite counts " + i);
                check.accept(
                        t.getEffect(MobEffects.WITHER).getDuration() == (burning ? 200 : 100),
                        "source matrix wither confirmations " + i);
            }
            Target edge = new Target();
            edge.addEffect(new MobEffectInstance(MobEffects.WITHER, Integer.MAX_VALUE - 50, 4));
            edge.hurt(player.damageSources().playerAttack(player), 1);
            check.accept(
                    edge.getEffect(MobEffects.WITHER).getDuration() == Integer.MAX_VALUE,
                    "wither saturates without overflow");
            for (float invalid :
                    new float[] {
                        0, -1, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY
                    }) {
                Target t = new Target();
                t.setSecondsOnFire(100);
                t.hurt(player.damageSources().playerAttack(player), invalid);
                check.accept(
                        t.getHealth() == 1000 && t.extras == 0 && !t.hasEffect(MobEffects.WITHER),
                        "invalid damage no side effects " + invalid);
            }
            Target timing = new Target();
            timing.hurt(player.damageSources().playerAttack(player), 1);
            long now = level.getGameTime();
            for (int offset : new int[] {1, 0, -1}) {
                timing.getPersistentData()
                        .putLong("the_long_travail:sun_suppression_until", now + offset);
                check.accept(
                        SunChaser.suppressed(timing) == (offset > 0),
                        "suppression deadline " + offset);
            }
            for (int i = 0; i < 1000; i++) {
                LongTravailData.setWitness(diary, TravailAspect.UNDERWORLD, (i & 1) == 0);
                Kagotsurube.refresh(player);
                check.accept(
                        CombatAttributes.insight(player) == ((i & 1) == 0 ? 0 : 15),
                        "phase transition no modifier accumulation " + i);
            }
            LongTravailData.setWitness(diary, TravailAspect.UNDERWORLD, true);
            Kagotsurube.refresh(player);
            // 直接检查真实队列的容量和溢出，不替换队列实现。
            var field = DamageReplay.class.getDeclaredField("QUEUES");
            field.setAccessible(true);
            Map<?, ?> queues = (Map<?, ?>) field.get(null);
            Target queued = new Target();
            level.addFreshEntity(queued);
            DamageReplay.Policy policy =
                    new DamageReplay.Policy() {
                        public boolean valid(LivingEntity a, LivingEntity b) {
                            return true;
                        }

                        public boolean continueChain(LivingEntity a, LivingEntity b) {
                            return false;
                        }
                    };
            for (int i = 1; i <= 4097; i++) {
                DamageReplay.enqueue(
                        player, queued, player.damageSources().playerAttack(player), 1, policy);
                if (i >= 4095)
                    check.accept(
                            ((ArrayDeque<?>) queues.get(level.getServer())).size()
                                    == Math.min(i, 4096),
                            "queue boundary " + i);
            }
            DamageReplay.cancel(player, policy);
            check.accept(
                    ((ArrayDeque<?>) queues.get(level.getServer())).isEmpty(),
                    "cancel clears capacity queue");
            queued.discard();
            // 通过真实箭矢运动及 Forge 命中事件验证反弹。
            LongTravailData.setWitness(diary, TravailAspect.FAR_REACH, false);
            player.setPos(.5, 100, .5);
            player.setNoGravity(true);
            player.setHealth(20);
            for (double speed : new double[] {1, 5, 30}) {
                Target shooter = new Target();
                level.addFreshEntity(shooter);
                Arrow arrow = new Arrow(level, shooter);
                arrow.setPos(-2, 101, .5);
                arrow.setNoGravity(true);
                arrow.setDeltaMovement(speed, 0, 0);
                level.addFreshEntity(arrow);
                for (int tick = 0; tick < 5 && arrow.getOwner() != player; tick++) arrow.tick();
                check.accept(
                        arrow.getOwner() == player, "real arrow impact ownership speed=" + speed);
                check.accept(
                        arrow.getDeltaMovement().x < 0,
                        "real arrow impact reverses speed=" + speed);
                check.accept(
                        player.getHealth() == 20,
                        "real impact prevents player damage speed=" + speed);
                arrow.discard();
                shooter.discard();
            }
            LongTravailData.setWitness(diary, TravailAspect.FAR_REACH, true);
        } finally {
            Files.write(Path.of("sun-blade-extreme-trace.csv"), trace);
        }
    }
}
