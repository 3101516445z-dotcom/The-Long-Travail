package com.thelongtravail.compat;

import com.thelongtravail.compat.gravestone.GravestoneAccess;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

public final class OptionalCompatTest {
    private static int passed;

    private static void expect(boolean value) {
        if (!value) throw new AssertionError("Compatibility check " + (passed + 1));
        passed++;
    }

    private static ClassNode node(String name) {
        ClassNode node = new ClassNode();
        node.name = name.replace('.', '/');
        return node;
    }

    private static MethodNode method(ClassNode node, String name, String desc, int flags) {
        MethodNode method = new MethodNode(Opcodes.ACC_PUBLIC | flags, name, desc, null, null);
        node.methods.add(method);
        return method;
    }

    private static OptionalCompatChecks.Classes source(Map<String, ClassNode> nodes) {
        return name -> {
            ClassNode node = nodes.get(name);
            if (node == null) throw new ClassNotFoundException(name);
            return node;
        };
    }

    private static void gates() {
        AtomicInteger calls = new AtomicInteger();
        var absent = new OptionalCompatState.Gate("test absent");
        expect(
                !absent.check(
                        false,
                        () -> {
                            throw new AssertionError("Absent probe ran");
                        }));
        var enabled = new OptionalCompatState.Gate("test enabled");
        expect(
                enabled.check(
                        true,
                        () -> {
                            calls.incrementAndGet();
                            return true;
                        }));
        expect(
                enabled.check(
                        true,
                        () -> {
                            throw new AssertionError("Repeated probe");
                        }));
        expect(calls.get() == 1);
        enabled.disable("test runtime mismatch");
        enabled.disable("duplicate warning must not be logged");
        expect(!enabled.enabled());
        expect(
                !enabled.check(
                        true,
                        () -> {
                            throw new AssertionError("Disabled probe ran");
                        }));
        expect(!new OptionalCompatState.Gate("test mismatch").check(true, () -> false));
        expect(
                !new OptionalCompatState.Gate("test missing")
                        .check(
                                true,
                                () -> {
                                    throw new ClassNotFoundException();
                                }));
        expect(
                !new OptionalCompatState.Gate("test unreadable")
                        .check(
                                true,
                                () -> {
                                    throw new IOException();
                                }));
        expect(
                !new OptionalCompatState.Gate("test linkage")
                        .check(
                                true,
                                () -> {
                                    throw new NoSuchMethodError();
                                }));
        try {
            new OptionalCompatState.Gate("test programming error")
                    .check(
                            true,
                            () -> {
                                throw new IllegalArgumentException();
                            });
            throw new AssertionError("Programming error swallowed");
        } catch (IllegalArgumentException expected) {
            passed++;
        }
        try {
            new OptionalCompatState.Gate("test fatal error")
                    .check(
                            true,
                            () -> {
                                throw new OutOfMemoryError("fixture");
                            });
            throw new AssertionError("Fatal error swallowed");
        } catch (OutOfMemoryError expected) {
            passed++;
        }
    }

    private static void contracts() throws Exception {
        Map<String, ClassNode> nodes = new HashMap<>();
        ClassNode tile = node(GravestoneAccess.TILE), death = node(GravestoneAccess.DEATH);
        nodes.put(GravestoneAccess.TILE, tile);
        nodes.put(GravestoneAccess.DEATH, death);
        method(tile, GravestoneAccess.GET_DEATH, GravestoneAccess.GET_DEATH_DESC, 0);
        MethodNode setter =
                method(tile, GravestoneAccess.SET_DEATH, GravestoneAccess.SET_DEATH_DESC, 0);
        method(death, GravestoneAccess.GET_ITEMS, GravestoneAccess.GET_ITEMS_DESC, 0);
        expect(OptionalCompatChecks.gravestone(source(nodes)));
        setter.desc = "()V";
        expect(!OptionalCompatChecks.gravestone(source(nodes)));
        setter.desc = GravestoneAccess.SET_DEATH_DESC;
        setter.access |= Opcodes.ACC_STATIC;
        expect(!OptionalCompatChecks.gravestone(source(nodes)));
        setter.access = Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT;
        expect(!OptionalCompatChecks.gravestone(source(nodes)));
        String apiName = "top.theillusivec4.caelus.api.CaelusApi";
        String implName = "top.theillusivec4.caelus.common.CaelusApiImpl";
        ClassNode api = node(apiName), impl = node(implName), tri = node(apiName + "$TriState");
        nodes.put(apiName, api);
        nodes.put(implName, impl);
        nodes.put(apiName + "$TriState", tri);
        MethodNode factory =
                method(
                        api,
                        "getInstance",
                        "()Ltop/theillusivec4/caelus/api/CaelusApi;",
                        Opcodes.ACC_STATIC);
        String result = "Ltop/theillusivec4/caelus/api/CaelusApi$TriState;";
        method(
                api,
                "canFallFly",
                "(Lnet/minecraft/world/entity/LivingEntity;)" + result,
                Opcodes.ACC_ABSTRACT);
        method(impl, "canFallFly", "(Lnet/minecraft/world/entity/LivingEntity;)" + result, 0);
        for (String name : new String[] {"DEFAULT", "ALLOW", "DENY"})
            tri.fields.add(
                    new FieldNode(
                            Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_ENUM,
                            name,
                            result,
                            null,
                            null));
        expect(OptionalCompatChecks.caelus(source(nodes)));
        factory.access &= ~Opcodes.ACC_STATIC;
        expect(!OptionalCompatChecks.caelus(source(nodes)));
        factory.access |= Opcodes.ACC_STATIC;
        tri.fields.remove(2);
        expect(!OptionalCompatChecks.caelus(source(nodes)));
        ClassNode anvil = node("fixture.Anvil");
        MethodNode limit = method(anvil, "apoth_removeLevelCap", "(I)I", 0);
        limit.instructions.add(new LdcInsnNode(Integer.MAX_VALUE));
        limit.instructions.add(new InsnNode(Opcodes.IRETURN));
        expect(OptionalCompatChecks.anvil(anvil));
        ((LdcInsnNode) limit.instructions.getFirst()).cst = 40;
        expect(!OptionalCompatChecks.anvil(anvil));
        ((LdcInsnNode) limit.instructions.getFirst()).cst = Integer.MAX_VALUE;
        limit.instructions.insert(new VarInsnNode(Opcodes.ILOAD, 1));
        expect(!OptionalCompatChecks.anvil(anvil));
    }

    private static void artifact(String path) throws Exception {
        try (JarFile jar = new JarFile(path)) {
            OptionalCompatChecks.Classes classes =
                    name -> {
                        var entry = jar.getJarEntry(name.replace('.', '/') + ".class");
                        if (entry == null) throw new ClassNotFoundException(name);
                        try (var input = jar.getInputStream(entry)) {
                            ClassNode node = new ClassNode();
                            new ClassReader(input).accept(node, 0);
                            return node;
                        }
                    };
            if (jar.getJarEntry(GravestoneAccess.TILE.replace('.', '/') + ".class") != null)
                expect(OptionalCompatChecks.gravestone(classes));
            else if (jar.getJarEntry("top/theillusivec4/caelus/api/CaelusApi.class") != null)
                expect(OptionalCompatChecks.caelus(classes));
            else {
                expect(
                        OptionalCompatChecks.anvil(
                                classes.read("dev.shadowsoffire.apotheosis.mixin.AnvilMenuMixin")));
                expect(
                        OptionalCompatChecks.anvil(
                                classes.read(
                                        "dev.shadowsoffire.apotheosis.mixin.client.AnvilScreenMixin")));
            }
            System.out.println("PASS artifact: " + jar.getName());
        }
    }

    public static void main(String[] args) throws Exception {
        gates();
        contracts();
        expect(OptionalCompatState.GRAVESTONE.check(true, () -> true));
        expect(GravestoneAccess.items(new Object()) == null);
        expect(!OptionalCompatState.GRAVESTONE.enabled());
        expect(GravestoneAccess.items(new Object()) == null);
        for (String path : args) artifact(path);
        System.out.println("PASS: " + passed + " optional compatibility checks");
    }
}
