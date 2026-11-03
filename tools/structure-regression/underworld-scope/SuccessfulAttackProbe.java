package travail.underworldscope;

import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.effect.MobEffects;
import com.thelongtravail.underworld.UnderworldItems;

// 目标沿用真实伤害处理，统计成功命中后进入的附魔回调，避免仅验证 hurt 输入。
final class SuccessfulAttackProbe {
    static void check(boolean ok, String label) {
        if (!ok) throw new AssertionError(label);
    }

    static void run(
            MinecraftServer server,
            ServerPlayer player,
            ItemStack callbackWeapon,
            List<String> lines)
            throws Exception {
        var zombie = EntityType.ZOMBIE.create(server.overworld());
        try {
            verify(player, zombie, callbackWeapon, false, 100, "ordinary mob", lines);
            if (net.minecraftforge.fml.ModList.get().isLoaded("touhou_little_maid"))
                MaidAttackScopeProbe.run(server, player, callbackWeapon, lines);
        } finally {
            zombie.discard();
        }
    }

    static void verify(
            ServerPlayer player,
            Mob attacker,
            ItemStack callbackWeapon,
            boolean undead,
            int count,
            String label,
            List<String> lines) {
        var target = new Spider(EntityType.SPIDER, player.serverLevel());
        target.setNoAi(true);
        target.setPos(player.getX() + 2, player.getY(), player.getZ());
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(10000);
        var weapon = callbackWeapon.copy();
        weapon.enchant(Enchantments.BANE_OF_ARTHROPODS, 5);
        var saved = player.getMainHandItem();
        attacker.setItemSlot(EquipmentSlot.MAINHAND, weapon);
        player.setItemInHand(InteractionHand.MAIN_HAND, callbackWeapon);
        int[] callbacks = {0};
        Runnable inner =
                () -> {
                    check(
                            target.getMobType() == (undead ? MobType.UNDEAD : MobType.ARTHROPOD),
                            label + " own callback type");
                    callbacks[0]++;
                };
        Runnable outer =
                () -> {
                    check(target.getMobType() == MobType.UNDEAD, label + " outer view");
                    UnderworldScopeProbe.attackProbe = inner;
                    try {
                        check(attacker.doHurtTarget(target), label + " successful hurt return");
                    } finally {
                        UnderworldScopeProbe.attackProbe = null;
                    }
                    check(target.getMobType() == MobType.UNDEAD, label + " restored outer view");
                };
        try {
            check(UnderworldItems.seesUndead(attacker) == undead, label + " actual eligibility");
            for (int n = 0; n < count; n++) {
                target.setHealth(target.getMaxHealth());
                target.invulnerableTime = 0;
                target.removeAllEffects();
                target.clearFire();
                int before = callbacks[0];
                UnderworldScopeProbe.attackProbe = outer;
                EnchantmentHelper.doPostDamageEffects(player, target);
                check(callbacks[0] == before + 1, label + " exactly one inner callback");
                check(
                        target.getHealth() < target.getMaxHealth() && target.isAlive(),
                        label + " actual health loss");
                check(
                        target.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) != undead,
                        label + " bane follows own type");
                check(target.getMobType() == MobType.ARTHROPOD, label + " final real type");
            }
            lines.add(
                    "PASS successful nested attack "
                            + label
                            + " iterations="
                            + count
                            + " callbacks="
                            + callbacks[0]);
        } finally {
            UnderworldScopeProbe.attackProbe = null;
            player.setItemInHand(InteractionHand.MAIN_HAND, saved);
            target.discard();
        }
    }
}
