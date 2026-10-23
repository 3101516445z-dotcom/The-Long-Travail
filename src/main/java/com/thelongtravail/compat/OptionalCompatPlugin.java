package com.thelongtravail.compat;

import java.util.List;
import java.util.Set;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;

public final class OptionalCompatPlugin implements IMixinConfigPlugin {
    private Boolean soundsSupported;

    public void onLoad(String mixinPackage) {}

    public String getRefMapperConfig() {
        return null;
    }

    public boolean shouldApplyMixin(String target, String mixin) {
        if (mixin.endsWith("AffixNameMixin") || mixin.endsWith("DiaryRuneRecipeMixin")) {
            String mod = mixin.endsWith("AffixNameMixin") ? "apotheosis" : "quark";
            return net.minecraftforge.fml.loading.FMLLoader.getLoadingModList().getMods().stream()
                    .anyMatch(info -> info.getModId().equals(mod));
        }
        if (mixin.endsWith("DummyDamageMixin")) {
            try {
                ClassNode node =
                        MixinService.getService().getBytecodeProvider().getClassNode(target);
                var method =
                        node.methods.stream()
                                .filter(
                                        m ->
                                                m.name.equals(
                                                                "onActuallyDamagedOrTrueDamageDetected")
                                                        && m.desc.equals(
                                                                "(FLnet/minecraft/world/damagesource/DamageSource;)V"))
                                .findFirst()
                                .orElse(null);
                int writes = 0;
                if (method != null)
                    for (var instruction : method.instructions) {
                        if (instruction instanceof org.objectweb.asm.tree.FieldInsnNode field
                                && field.getOpcode() == Opcodes.PUTFIELD
                                && field.owner.equals(target.replace('.', '/'))
                                && field.name.equals("totalDamageTakenInCombat")
                                && field.desc.equals("F")) writes++;
                    }
                if (writes != 1)
                    warn(target, "unsupported dummy damage accounting; damage adapter disabled");
                return writes == 1;
            } catch (ClassNotFoundException absent) {
                return false;
            } catch (java.io.IOException failure) {
                warn(target, failure.getMessage());
                return false;
            }
        }
        if (mixin.endsWith("MaidPersonalityStorageMixin")) {
            try {
                ClassNode node =
                        MixinService.getService().getBytecodeProvider().getClassNode(target);
                var contracts =
                        java.util.Map.of(
                                "stamp",
                                        "(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/nbt/CompoundTag;)V",
                                "buildRequest",
                                        "(Lnet/minecraft/nbt/CompoundTag;J)Lnet/minecraft/nbt/CompoundTag;",
                                "resolveRelease",
                                        "(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/nbt/CompoundTag;J)V",
                                "superviseHealthFix",
                                        "(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/nbt/CompoundTag;)V");
                boolean supported =
                        contracts.entrySet().stream()
                                .allMatch(
                                        contract ->
                                                node.methods.stream()
                                                        .anyMatch(
                                                                method ->
                                                                        method.name.equals(
                                                                                        contract
                                                                                                .getKey())
                                                                                && method.desc
                                                                                        .equals(
                                                                                                contract
                                                                                                        .getValue())
                                                                                && (method.access
                                                                                                & Opcodes
                                                                                                        .ACC_STATIC)
                                                                                        != 0));
                if (!supported)
                    warn(
                            target,
                            "unsupported Personality storage API; dynamic-health adapter disabled");
                return supported;
            } catch (ClassNotFoundException absent) {
                return false;
            } catch (java.io.IOException failure) {
                warn(target, failure.getMessage());
                return false;
            }
        }
        if (mixin.endsWith("MaidSwimMixin") || mixin.endsWith("MaidRelicSlotMixin")) {
            try {
                MixinService.getService().getBytecodeProvider().getClassNode(target);
                return true;
            } catch (ClassNotFoundException absent) {
                return false;
            } catch (java.io.IOException failure) {
                throw new IllegalStateException("Cannot inspect maid swimming API", failure);
            }
        }
        if (mixin.endsWith("BookGravestoneMixin") || mixin.endsWith("BookBlockEntityMixin"))
            return OptionalCompatState.GRAVESTONE.check(
                    installed("gravestone"),
                    () ->
                            OptionalCompatChecks.gravestone(
                                    name ->
                                            MixinService.getService()
                                                    .getBytecodeProvider()
                                                    .getClassNode(name)));
        if (mixin.endsWith("IcarusCaelusApiMixin"))
            return OptionalCompatState.CAELUS.check(
                    installed("caelus"),
                    () ->
                            OptionalCompatChecks.caelus(
                                    name ->
                                            MixinService.getService()
                                                    .getBytecodeProvider()
                                                    .getClassNode(name)));
        if (mixin.endsWith("LanternEmbeddiumLightMixin")) {
            try {
                ClassNode node =
                        MixinService.getService().getBytecodeProvider().getClassNode(target);
                boolean supported =
                        node.methods.stream()
                                .anyMatch(
                                        m ->
                                                m.name.equals("calculate")
                                                        && m.desc.equals(
                                                                "(Lme/jellysquid/mods/sodium/client/model/quad/ModelQuadView;Lnet/minecraft/core/BlockPos;Lme/jellysquid/mods/sodium/client/model/light/data/QuadLightData;Lnet/minecraft/core/Direction;Lnet/minecraft/core/Direction;Z)V"));
                if (!supported) warn(target, "unsupported lantern lighting pipeline");
                return supported;
            } catch (ClassNotFoundException e) {
                return false;
            } catch (java.io.IOException e) {
                warn(target, e.getMessage());
                return false;
            }
        }
        boolean jei = mixin.endsWith("JeiReadingOverlayMixin");
        boolean sounds =
                mixin.endsWith("SoundsPotionCompatMixin")
                        || mixin.endsWith("ConfiguredSoundInvoker");
        if (!jei && !sounds) return true;
        if (sounds && soundsSupported != null) return soundsSupported;
        try {
            ClassNode node =
                    MixinService.getService()
                            .getBytecodeProvider()
                            .getClassNode(
                                    sounds
                                            ? "dev.imb11.sounds.sound.events.PotionEventHelper"
                                            : target);
            boolean supported;
            if (jei) {
                supported =
                        List.of("onDrawBackgroundPost", "onDrawForeground", "onDrawScreenPost")
                                .stream()
                                .allMatch(
                                        name ->
                                                node.methods.stream()
                                                        .anyMatch(
                                                                m ->
                                                                        m.name.equals(name)
                                                                                && m.desc.endsWith(
                                                                                        ")V")));
            } else {
                supported =
                        node.fields.stream()
                                        .anyMatch(
                                                f ->
                                                        f.name.equals("previousEffects")
                                                                && f.desc.equals(
                                                                        "Ljava/util/concurrent/atomic/AtomicReference;")
                                                                && (f.access & Opcodes.ACC_STATIC)
                                                                        != 0)
                                && node.methods.stream()
                                        .anyMatch(
                                                m ->
                                                        m.name.equals("listenForEffectChanges")
                                                                && m.desc.endsWith(")V")
                                                                && (m.access & Opcodes.ACC_STATIC)
                                                                        != 0
                                                                && soundLayout(m));
                ClassNode sound =
                        MixinService.getService()
                                .getBytecodeProvider()
                                .getClassNode("dev.imb11.sounds.api.config.ConfiguredSound");
                supported &=
                        sound.methods.stream()
                                .anyMatch(
                                        m ->
                                                m.name.equals("playSound")
                                                        && m.desc.equals("()V")
                                                        && (m.access & Opcodes.ACC_PUBLIC) != 0
                                                        && (m.access & Opcodes.ACC_STATIC) == 0);
            }
            if (sounds) soundsSupported = supported;
            if (!supported) warn(target, "unsupported method/field layout");
            return supported;
        } catch (ClassNotFoundException absent) {
            if (sounds) soundsSupported = false;
            return false;
        } catch (java.io.IOException unreadable) {
            if (sounds) soundsSupported = false;
            warn(target, "unable to inspect bytecode: " + unreadable.getMessage());
            return false;
        }
    }

    private static boolean installed(String mod) {
        return net.minecraftforge.fml.loading.FMLLoader.getLoadingModList().getMods().stream()
                .anyMatch(info -> info.getModId().equals(mod));
    }

    private static boolean soundLayout(org.objectweb.asm.tree.MethodNode method) {
        int lookups = 0, plays = 0;
        for (var insn : method.instructions)
            if (insn instanceof org.objectweb.asm.tree.MethodInsnNode call) {
                if (call.owner.equals("net/minecraft/core/Registry")
                        && call.desc.equals(
                                "(Lnet/minecraft/resources/ResourceLocation;)Ljava/lang/Object;"))
                    lookups++;
                if (call.owner.equals("dev/imb11/sounds/api/config/ConfiguredSound")
                        && call.name.equals("playSound")
                        && call.desc.equals("()V")) plays++;
            }
        return lookups == 2 && plays == 4;
    }

    private static void warn(String target, String reason) {
        org.slf4j.LoggerFactory.getLogger("the_long_travail.compat")
                .warn(
                        "Skipping optional compatibility for {}: {}. Its compatibility adapter is unavailable.",
                        target,
                        reason);
    }

    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    public List<String> getMixins() {
        return null;
    }

    public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {}

    public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {
        if (mixin.endsWith("ConfiguredSoundInvoker")) {
            boolean bridge =
                    node.interfaces.contains(
                                    "com/thelongtravail/compat/mixin/sounds/ConfiguredSoundInvoker")
                            && node.methods.stream()
                                    .anyMatch(
                                            m ->
                                                    m.name.equals("travail$playSound")
                                                            && m.desc.equals("()V")
                                                            && (m.access & Opcodes.ACC_PUBLIC) != 0
                                                            && (m.access
                                                                            & (Opcodes.ACC_STATIC
                                                                                    | Opcodes
                                                                                            .ACC_ABSTRACT))
                                                                    == 0);
            if (!bridge)
                throw new IllegalStateException(
                        "Sounds playSound invoker bridge was not generated for " + target);
            org.slf4j.LoggerFactory.getLogger("the_long_travail.compat")
                    .info("Sounds playSound invoker bridge verified");
            return;
        }
        if (!mixin.endsWith("SoundsPotionCompatMixin")) return;
        int captures = 0, plays = 0;
        for (var method : node.methods)
            if (method.name.equals("listenForEffectChanges"))
                for (var insn : method.instructions)
                    if (insn instanceof org.objectweb.asm.tree.MethodInsnNode call) {
                        if (call.name.contains("travail$removed")
                                || call.name.contains("travail$added")) captures++;
                        if (call.name.contains("travail$sound")) plays++;
                    }
        boolean enabled = captures == 2 && plays == 4;
        System.setProperty("the_long_travail.soundsAdapterVerified", Boolean.toString(enabled));
        org.slf4j.LoggerFactory.getLogger("the_long_travail.compat")
                .info(
                        "Sounds targeted adapter verified={}: {} ID captures, {} sound redirects",
                        enabled,
                        captures,
                        plays);
    }
}
