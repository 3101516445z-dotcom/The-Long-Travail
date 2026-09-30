package com.thelongtravail.mixin;
import com.thelongtravail.client.EffectSounds;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Sounds 2.2.1: capture each diff ID, then filter that exact notification. No duplicate snapshots. */
@Pseudo
@Mixin(targets = "dev.imb11.sounds.sound.events.PotionEventHelper", remap = false)
public abstract class SoundsPotionCompatMixin {
    @Unique private static ResourceLocation travail$effect;
    @Unique private static boolean travail$gain;
    @Inject(method = "listenForEffectChanges", at = @At("HEAD"), remap = false, require = 0)
    private static void travail$begin(CallbackInfo ci) { travail$effect = null; }
    @Redirect(method = "listenForEffectChanges", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/Registry;get(Lnet/minecraft/resources/ResourceLocation;)Ljava/lang/Object;", ordinal = 0, remap = true), remap = false, require = 0)
    private static Object travail$removed(Registry<?> registry, ResourceLocation id) {
        travail$effect = id; travail$gain = false; return registry.get(id);
    }
    @Redirect(method = "listenForEffectChanges", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/Registry;get(Lnet/minecraft/resources/ResourceLocation;)Ljava/lang/Object;", ordinal = 1, remap = true), remap = false, require = 0)
    private static Object travail$added(Registry<?> registry, ResourceLocation id) {
        travail$effect = id; travail$gain = true; return registry.get(id);
    }
    @Redirect(method = "listenForEffectChanges", at = @At(value = "INVOKE", target = "Ldev/imb11/sounds/api/config/ConfiguredSound;playSound()V", remap = false), remap = false, require = 0)
    private static void travail$sound(@Coerce Object sound) { EffectSounds.notification(travail$effect, travail$gain, sound); }
}
