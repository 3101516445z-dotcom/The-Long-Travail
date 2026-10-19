package com.thelongtravail.valley;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import cpw.mods.modlauncher.api.INameMappingService;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

final class VanillaExecution {
    private final MethodHandle death, removal;

    private VanillaExecution() throws ReflectiveOperationException {
        death = exact(LivingEntity.class, "m_6667_", DamageSource.class);
        removal = exact(Entity.class, "m_142467_", Entity.RemovalReason.class);
    }

    private static MethodHandle exact(Class<?> owner, String srg, Class<?> argument)
            throws ReflectiveOperationException {
        String name = ObfuscationReflectionHelper.remapName(INameMappingService.Domain.METHOD, srg);
        return MethodHandles.privateLookupIn(owner, MethodHandles.lookup())
                .findSpecial(owner, name, MethodType.methodType(void.class, argument), owner);
    }

    private static VanillaExecution instance;

    static synchronized VanillaExecution resolve() {
        if (instance == null) {
            try {
                instance = new VanillaExecution();
            } catch (ReflectiveOperationException ex) {
                throw new IllegalStateException("Cannot resolve vanilla execution bodies", ex);
            }
        }
        return instance;
    }

    void die(LivingEntity target, DamageSource source) {
        try {
            death.invokeExact(target, source);
        } catch (RuntimeException | Error ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new IllegalStateException("Vanilla death invocation failed", ex);
        }
    }

    void remove(Entity target) {
        try {
            removal.invokeExact(target, Entity.RemovalReason.KILLED);
        } catch (RuntimeException | Error ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new IllegalStateException("Vanilla removal invocation failed", ex);
        }
    }
}
