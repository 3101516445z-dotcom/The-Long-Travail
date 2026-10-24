package com.thelongtravail.compat.maid;

import static com.thelongtravail.compat.maid.MaidAccess.identity;
import static com.thelongtravail.compat.maid.MaidAccessoryRules.crown;
import static com.thelongtravail.compat.maid.MaidAccessoryRules.eki;
import static com.thelongtravail.compat.maid.MaidAccessoryRules.has;
import static com.thelongtravail.compat.maid.MaidAccessoryRules.sunny;
import static com.thelongtravail.compat.maid.MaidAccessoryRules.tokaido;
import static com.thelongtravail.compat.maid.MaidRuntimeState.state;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.thelongtravail.boundless.TimeStopManager;
import com.thelongtravail.compat.maid.MaidAccessoryRuntime.Phase;
import com.thelongtravail.config.FlourishingItemsConfig;
import com.thelongtravail.config.RainConfig;
import com.thelongtravail.flourishing.FloralCombat;
import com.thelongtravail.flourishing.Flower;
import com.thelongtravail.flourishing.FlowerData;
import com.thelongtravail.registry.ModRegistry;
import java.util.UUID;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.common.ForgeMod;

// 按固定顺序维护每个女仆的属性、治疗与冷却。
final class MaidPeriodicEffects {
    private static final String ID = MaidAccess.IDENTITY_KEY;
    private static final UUID HEALTH = UUID.fromString("193ab8d0-46cc-4179-bb32-f0317ed65401"),
            ATTACK = UUID.fromString("193ab8d0-46cc-4179-bb32-f0317ed65402"),
            SWIM = UUID.fromString("193ab8d0-46cc-4179-bb32-f0317ed65403"),
            SPEED = UUID.fromString("193ab8d0-46cc-4179-bb32-f0317ed65404"),
            EKI_SPEED = UUID.fromString("193ab8d0-46cc-4179-bb32-f0317ed65405");

    private static void attribute(EntityMaid m, Attribute attribute, UUID id, double value) {
        var instance = m.getAttribute(attribute);
        if (instance == null) return;
        var old = instance.getModifier(id);
        if (old != null && Double.compare(old.getAmount(), value) == 0) return;
        if (old != null) instance.removeModifier(id);
        if (value > 0)
            instance.addTransientModifier(
                    new AttributeModifier(
                            id,
                            "Travail maid bauble",
                            value,
                            AttributeModifier.Operation.MULTIPLY_TOTAL));
        if (attribute == Attributes.MAX_HEALTH && old != null)
            com.thelongtravail.compat.maidspell.MaidSpellHealth.clampAfterModifierRemoval(m);
    }

    public static void tick(EntityMaid m) {
        float phosphophylliteMaxBefore = m.getMaxHealth();
        com.thelongtravail.valley.Phosphophyllite.tick(m);
        if (m.getMaxHealth() < phosphophylliteMaxBefore)
            com.thelongtravail.compat.maidspell.MaidSpellHealth.clampAfterModifierRemoval(m);
        com.thelongtravail.abyss.MidnightLafite.clear(m);
        var s = state(m);
        com.thelongtravail.farreach.DonQuixoteState.tick(m);
        var crown = crown(m);
        com.thelongtravail.flourishing.FloralPassives.tick(m, crown);
        attribute(m, Attributes.MAX_HEALTH, HEALTH, FloralCombat.value(crown, Flower.PEONY));
        attribute(
                m, Attributes.ATTACK_SPEED, ATTACK, FloralCombat.value(crown, Flower.ORANGE_TULIP));
        attribute(
                m, ForgeMod.SWIM_SPEED.get(), SWIM, FloralCombat.value(crown, Flower.BLUE_ORCHID));
        attribute(
                m,
                Attributes.MOVEMENT_SPEED,
                SPEED,
                FloralCombat.value(crown, Flower.PINK_PETALS)
                        + (FlowerData.active(crown, Flower.SUNFLOWER) && sunny(m)
                                ? FlourishingItemsConfig.extra(Flower.SUNFLOWER, "speedBonus")
                                : 0));
        attribute(
                m,
                Attributes.MOVEMENT_SPEED,
                EKI_SPEED,
                eki(m) == Phase.MALICE ? RainConfig.SPEED.get() : 0);
        if (!has(m, ModRegistry.SWORD_AND_LANTERN.get())) MaidLanternCombat.forget(m);
        else
            for (var effect :
                    new MobEffect[] {
                        MobEffects.BLINDNESS, MobEffects.DARKNESS, MobEffects.CONFUSION
                    }) if (m.hasEffect(effect)) m.removeEffect(effect);
        if (TimeStopManager.frozen(m)) return;
        if (FlowerData.active(crown, Flower.OXEYE_DAISY)) {
            if (++s.heal
                    >= FlourishingItemsConfig.ticks(
                            FlourishingItemsConfig.extra(Flower.OXEYE_DAISY, "intervalSeconds"))) {
                s.heal = 0;
                if (m.getHealth() < m.getMaxHealth())
                    m.heal((float) FlourishingItemsConfig.value(Flower.OXEYE_DAISY));
            }
        } else s.heal = 0;
        if (tokaido(m)) {
            if (++s.rainHeal >= RainConfig.ticks(RainConfig.HEAL_SECONDS.get())) {
                s.rainHeal = 0;
                if (m.getHealth() < m.getMaxHealth() && RainConfig.HEAL.get() > 0)
                    m.heal(RainConfig.HEAL.get().floatValue());
            }
        } else s.rainHeal = 0;
        var persistent = m.getPersistentData();
        if (!persistent.hasUUID(ID)) return;
        var cooldownLedger = MaidAccessoryLedger.get(m.getServer());
        var data = cooldownLedger.find(persistent.getUUID(ID));
        if (data == null) return;
        boolean dirty = false;
        if (data.getInt("StarCooldown") > 0 && !TimeStopManager.locked(identity(m))) {
            data.putInt("StarCooldown", data.getInt("StarCooldown") - 1);
            dirty = true;
        }
        if (data.getInt("StarInterval") > 0) {
            data.putInt("StarInterval", data.getInt("StarInterval") - 1);
            dirty = true;
        }
        if (data.getInt("StarReceivedInterval") > 0) {
            data.putInt("StarReceivedInterval", data.getInt("StarReceivedInterval") - 1);
            dirty = true;
        }
        if (dirty) cooldownLedger.setDirty();
    }

    private MaidPeriodicEffects() {}
}
