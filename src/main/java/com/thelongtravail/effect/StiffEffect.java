package com.thelongtravail.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.Attributes;
import java.util.UUID;

public class StiffEffect extends MobEffect {
    private static final UUID LEGACY_SPEED_MODIFIER = UUID.fromString("d2b21a50-a731-4f15-91ea-5cfcb76e818f");

    public StiffEffect() {
        super(MobEffectCategory.HARMFUL, 0x59636f);
    }

    @Override
    public void removeAttributeModifiers(LivingEntity entity, AttributeMap attributes, int amplifier) {
        super.removeAttributeModifiers(entity, attributes, amplifier);
        // 旧存档可能残留原先的永久效果修饰符；效果到期或被驱散时仅移除本模组的历史 UUID。
        var speed = attributes.getInstance(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.removeModifier(LEGACY_SPEED_MODIFIER);
    }
}
