package com.thelongtravail.compat;

import com.thelongtravail.compat.gravestone.GravestoneAccess;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.util.List;

public final class OptionalCompatChecks {
    @FunctionalInterface
    public interface Classes {
        ClassNode read(String name) throws ClassNotFoundException, IOException;
    }

    private static boolean method(
            ClassNode node, String name, String descriptor, boolean isStatic) {
        return node.methods.stream()
                .anyMatch(
                        m ->
                                m.name.equals(name)
                                        && m.desc.equals(descriptor)
                                        && (m.access & Opcodes.ACC_PUBLIC) != 0
                                        && ((m.access & Opcodes.ACC_STATIC) != 0) == isStatic);
    }

    private static boolean concrete(ClassNode node, String name, String descriptor) {
        return method(node, name, descriptor, false)
                && node.methods.stream()
                        .anyMatch(
                                m ->
                                        m.name.equals(name)
                                                && m.desc.equals(descriptor)
                                                && (m.access
                                                                & (Opcodes.ACC_ABSTRACT
                                                                        | Opcodes.ACC_NATIVE))
                                                        == 0);
    }

    public static boolean gravestone(Classes classes) throws ClassNotFoundException, IOException {
        ClassNode tile = classes.read(GravestoneAccess.TILE);
        ClassNode death = classes.read(GravestoneAccess.DEATH);
        return method(tile, GravestoneAccess.GET_DEATH, GravestoneAccess.GET_DEATH_DESC, false)
                && concrete(tile, GravestoneAccess.SET_DEATH, GravestoneAccess.SET_DEATH_DESC)
                && method(
                        death, GravestoneAccess.GET_ITEMS, GravestoneAccess.GET_ITEMS_DESC, false);
    }

    public static boolean caelus(Classes classes) throws ClassNotFoundException, IOException {
        String api = "top.theillusivec4.caelus.api.CaelusApi";
        String descriptor = "Ltop/theillusivec4/caelus/api/CaelusApi$TriState;";
        ClassNode base = classes.read(api);
        ClassNode implementation = classes.read("top.theillusivec4.caelus.common.CaelusApiImpl");
        ClassNode result = classes.read(api + "$TriState");
        return method(base, "getInstance", "()Ltop/theillusivec4/caelus/api/CaelusApi;", true)
                && method(
                        base,
                        "canFallFly",
                        "(Lnet/minecraft/world/entity/LivingEntity;)" + descriptor,
                        false)
                && concrete(
                        implementation,
                        "canFallFly",
                        "(Lnet/minecraft/world/entity/LivingEntity;)" + descriptor)
                && List.of("DEFAULT", "ALLOW", "DENY").stream()
                        .allMatch(
                                name ->
                                        result.fields.stream()
                                                .anyMatch(
                                                        field ->
                                                                field.name.equals(name)
                                                                        && field.desc.equals(
                                                                                descriptor)
                                                                        && (field.access
                                                                                        & (Opcodes
                                                                                                        .ACC_PUBLIC
                                                                                                | Opcodes
                                                                                                        .ACC_STATIC
                                                                                                | Opcodes
                                                                                                        .ACC_ENUM))
                                                                                == (Opcodes
                                                                                                .ACC_PUBLIC
                                                                                        | Opcodes
                                                                                                .ACC_STATIC
                                                                                        | Opcodes
                                                                                                .ACC_ENUM)));
    }

    private OptionalCompatChecks() {}
}
