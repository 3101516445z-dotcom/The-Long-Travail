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
    public void onLoad(String mixinPackage) {}
    public String getRefMapperConfig() { return null; }
    public boolean shouldApplyMixin(String target, String mixin) {
        boolean jei = mixin.endsWith("JeiReadingOverlayMixin");
        boolean sounds = mixin.endsWith("SoundsPotionCompatMixin");
        if (!jei && !sounds) return true;
        try {
            ClassNode node = MixinService.getService().getBytecodeProvider().getClassNode(target);
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
                supported &= sound.methods.stream().anyMatch(m -> m.name.equals("playSound") && m.desc.equals("()V") && (m.access & Opcodes.ACC_PUBLIC) != 0);
            }
            if (!supported) warn(target, "unsupported method/field layout");
            return supported;
        } catch (ClassNotFoundException absent) {
            return false;
        } catch (java.io.IOException unreadable) {
            warn(target, "unable to inspect bytecode: " + unreadable.getMessage());
            return false;
        }
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
