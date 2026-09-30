package com.thelongtravail.helper;

import com.thelongtravail.TheLongTravail;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;

public final class TrueDamage {
    public static final ResourceKey<DamageType> FLUID = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(TheLongTravail.MODID, "fluid_erosion"));

    public static void hurtFluid(ServerPlayer player, float amount) {
        DamageSource source = new DamageSource(player.serverLevel().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(FLUID));
        player.hurt(source, amount);
    }

    private TrueDamage() {}
}
