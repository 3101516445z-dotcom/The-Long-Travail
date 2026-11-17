package com.thelongtravail.compat;

import static org.objectweb.asm.Opcodes.ACC_ABSTRACT;
import static org.objectweb.asm.Opcodes.ACC_NATIVE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;

import org.objectweb.asm.tree.ClassNode;

final class ConfigScreenContract {
    // 创建页与保存回调共同组成适配契约，避免只启用其中一项注入。
    static boolean supported(ClassNode screen, ClassNode main) {
        return main.superName.equals(screen.name)
                && screen.methods.stream().filter(m -> m.name.equals("create")).count() == 1
                && screen.methods.stream()
                        .anyMatch(
                                m ->
                                        m.name.equals("create")
                                                && m.desc.equals(
                                                        "(Lnet/minecraft/client/gui/screens/Screen;Lnet/minecraft/network/chat/Component;Lnet/minecraftforge/fml/config/ModConfig;Ljava/util/Map;)Lfuzs/forgeconfigscreens/client/gui/screens/ConfigScreen;")
                                                && (m.access
                                                                & (ACC_PUBLIC
                                                                        | ACC_STATIC
                                                                        | ACC_NATIVE
                                                                        | ACC_ABSTRACT))
                                                        == (ACC_PUBLIC | ACC_STATIC))
                && main.methods.stream().filter(m -> m.name.equals("lambda$init$0")).count() == 1
                && main.methods.stream()
                        .anyMatch(
                                m ->
                                        m.name.equals("lambda$init$0")
                                                && m.desc.equals(
                                                        "(Lnet/minecraft/client/gui/components/Button;)V")
                                                && (m.access
                                                                & (ACC_STATIC
                                                                        | ACC_NATIVE
                                                                        | ACC_ABSTRACT))
                                                        == 0);
    }
}
