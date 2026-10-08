package com.thelongtravail.mixin;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;
import java.util.List;
import java.util.Set;

/**
 * 可选客户端适配器仅在目标字节码结构匹配时启用；跳过仅影响覆盖层或通知音效。
 * 其他针对原版或 Forge 的 Mixin 无条件应用，确保游戏规则不受可选适配器影响。
 */
public final class OptionalCompatPlugin implements IMixinConfigPlugin {
    private Boolean soundsSupported;
    public void onLoad(String mixinPackage) {}
    public String getRefMapperConfig() { return null; }
    public boolean shouldApplyMixin(String target, String mixin) {
        if (mixin.endsWith("BookGravestoneMixin")) return gravestone(target);
        if (mixin.endsWith("IcarusCaelusApiMixin")) return caelus(target);
        if (mixin.endsWith("LanternEmbeddiumLightMixin")) {
            try {
                ClassNode node=MixinService.getService().getBytecodeProvider().getClassNode(target);
                boolean supported=node.methods.stream().anyMatch(m->m.name.equals("calculate") && m.desc.equals("(Lme/jellysquid/mods/sodium/client/model/quad/ModelQuadView;Lnet/minecraft/core/BlockPos;Lme/jellysquid/mods/sodium/client/model/light/data/QuadLightData;Lnet/minecraft/core/Direction;Lnet/minecraft/core/Direction;Z)V"));
                if(!supported)warn(target,"unsupported lantern lighting pipeline");
                return supported;
            } catch(ClassNotFoundException e){return false;} catch(java.io.IOException e){warn(target,e.getMessage());return false;}
        }
        boolean jei = mixin.endsWith("JeiReadingOverlayMixin");
        boolean sounds = mixin.endsWith("SoundsPotionCompatMixin") || mixin.endsWith("ConfiguredSoundInvoker");
        if (!jei && !sounds) return true;
        if (sounds && soundsSupported != null) return soundsSupported;
        try {
            ClassNode node = MixinService.getService().getBytecodeProvider().getClassNode(
                    sounds ? "dev.imb11.sounds.sound.events.PotionEventHelper" : target);
            boolean supported;
            if (jei) {
                supported = List.of("onDrawBackgroundPost", "onDrawForeground", "onDrawScreenPost").stream()
                        .allMatch(name -> node.methods.stream().anyMatch(m -> m.name.equals(name) && m.desc.endsWith(")V")));
            } else {
                supported = node.fields.stream().anyMatch(f -> f.name.equals("previousEffects")
                        && f.desc.equals("Ljava/util/concurrent/atomic/AtomicReference;")
                        && (f.access & Opcodes.ACC_STATIC) != 0)
                        && node.methods.stream().anyMatch(m -> m.name.equals("listenForEffectChanges")
                        && m.desc.endsWith(")V") && (m.access & Opcodes.ACC_STATIC) != 0 && soundLayout(m));
                ClassNode sound = MixinService.getService().getBytecodeProvider().getClassNode("dev.imb11.sounds.api.config.ConfiguredSound");
                supported &= sound.methods.stream().anyMatch(m -> m.name.equals("playSound") && m.desc.equals("()V")
                        && (m.access & Opcodes.ACC_PUBLIC) != 0 && (m.access & Opcodes.ACC_STATIC) == 0);
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
    private static boolean publicInstance(ClassNode node, String name, String descriptor) {
        return node.methods.stream().anyMatch(method -> method.name.equals(name) && method.desc.equals(descriptor)
                && (method.access & Opcodes.ACC_PUBLIC) != 0 && (method.access & Opcodes.ACC_STATIC) == 0);
    }
    private static boolean gravestone(String target) {
        ClassNode tile;
        try { tile = MixinService.getService().getBytecodeProvider().getClassNode(target); }
        catch (ClassNotFoundException absent) { return false; }
        catch (java.io.IOException failure) { throw new IllegalStateException("Cannot inspect Gravestone; item reconciliation cannot be verified", failure); }
        try {
            var death = MixinService.getService().getBytecodeProvider().getClassNode(com.thelongtravail.underworld.GravestoneAccess.DEATH);
            if (publicInstance(tile, com.thelongtravail.underworld.GravestoneAccess.GET_DEATH, com.thelongtravail.underworld.GravestoneAccess.GET_DEATH_DESC)
                    && publicInstance(tile, com.thelongtravail.underworld.GravestoneAccess.SET_DEATH, com.thelongtravail.underworld.GravestoneAccess.SET_DEATH_DESC)
                    && publicInstance(death, com.thelongtravail.underworld.GravestoneAccess.GET_ITEMS, com.thelongtravail.underworld.GravestoneAccess.GET_ITEMS_DESC)) return true;
        } catch (ClassNotFoundException | java.io.IOException failure) {
            throw new IllegalStateException("Incomplete Gravestone API; item reconciliation cannot be verified", failure);
        }
        throw new IllegalStateException("Unsupported Gravestone API: getDeath/setDeath/getAllItems signatures changed; refusing unsafe item reconciliation");
    }
    private static boolean caelus(String target) {
        ClassNode node;
        try { node = MixinService.getService().getBytecodeProvider().getClassNode(target); }
        catch (ClassNotFoundException absent) { return false; }
        catch (java.io.IOException failure) { throw new IllegalStateException("Cannot inspect Caelus flight API", failure); }
        try {
            var result = MixinService.getService().getBytecodeProvider().getClassNode("top.theillusivec4.caelus.api.CaelusApi$TriState");
            String descriptor = "Ltop/theillusivec4/caelus/api/CaelusApi$TriState;";
            boolean fields = List.of("DEFAULT", "ALLOW", "DENY").stream().allMatch(name -> result.fields.stream().anyMatch(field ->
                    field.name.equals(name) && field.desc.equals(descriptor) && (field.access & Opcodes.ACC_ENUM) != 0));
            if (fields && publicInstance(node, "canFallFly", "(Lnet/minecraft/world/entity/LivingEntity;)" + descriptor)) return true;
        } catch (ClassNotFoundException | java.io.IOException failure) { throw new IllegalStateException("Incomplete Caelus flight API", failure); }
        throw new IllegalStateException("Unsupported Caelus canFallFly/TriState API; Icarus flight adapter cannot be applied");
    }
    private static boolean soundLayout(org.objectweb.asm.tree.MethodNode method) {
        int lookups = 0, plays = 0;
        for (var insn : method.instructions) if (insn instanceof org.objectweb.asm.tree.MethodInsnNode call) {
            if (call.owner.equals("net/minecraft/core/Registry") && call.desc.equals("(Lnet/minecraft/resources/ResourceLocation;)Ljava/lang/Object;")) lookups++;
            if (call.owner.equals("dev/imb11/sounds/api/config/ConfiguredSound") && call.name.equals("playSound") && call.desc.equals("()V")) plays++;
        }
        return lookups == 2 && plays == 4;
    }
    private static void warn(String target, String reason) {
        org.slf4j.LoggerFactory.getLogger("the_long_travail.compat")
                .warn("Skipping optional compatibility for {}: {}. Its compatibility adapter is unavailable.", target, reason);
    }
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    public List<String> getMixins() { return null; }
    public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
    public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {
        if (mixin.endsWith("ConfiguredSoundInvoker")) {
            boolean bridge = node.interfaces.contains("com/thelongtravail/mixin/ConfiguredSoundInvoker")
                    && node.methods.stream().anyMatch(m -> m.name.equals("travail$playSound") && m.desc.equals("()V")
                    && (m.access & Opcodes.ACC_PUBLIC) != 0 && (m.access & (Opcodes.ACC_STATIC | Opcodes.ACC_ABSTRACT)) == 0);
            if (!bridge) throw new IllegalStateException("Sounds playSound invoker bridge was not generated for " + target);
            org.slf4j.LoggerFactory.getLogger("the_long_travail.compat").info("Sounds playSound invoker bridge verified");
            return;
        }
        if (!mixin.endsWith("SoundsPotionCompatMixin")) return;
        int captures = 0, plays = 0;
        for (var method : node.methods) if (method.name.equals("listenForEffectChanges"))
            for (var insn : method.instructions) if (insn instanceof org.objectweb.asm.tree.MethodInsnNode call) {
                if (call.name.contains("travail$removed") || call.name.contains("travail$added")) captures++;
                if (call.name.contains("travail$sound")) plays++;
            }
        boolean enabled = captures == 2 && plays == 4;
        System.setProperty("the_long_travail.soundsAdapterVerified", Boolean.toString(enabled));
        org.slf4j.LoggerFactory.getLogger("the_long_travail.compat").info(
                "Sounds targeted adapter verified={}: {} ID captures, {} sound redirects", enabled, captures, plays);
    }
}
