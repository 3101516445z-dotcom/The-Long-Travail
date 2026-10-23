package com.thelongtravail.compat.fluid;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

final class FluidReflection {
    private record Signature(String name, List<Class<?>> parameters) {}

    private static final Class<?>[] NO_TYPES = new Class<?>[0];
    private static final Map<String, Class<?>> TYPES = new ConcurrentHashMap<>();
    private static final ClassValue<Map<Signature, Method>> METHODS =
            new ClassValue<>() {
                @Override
                protected Map<Signature, Method> computeValue(Class<?> type) {
                    return new ConcurrentHashMap<>();
                }
            };
    private static final ClassValue<Map<String, Field>> FIELDS =
            new ClassValue<>() {
                @Override
                protected Map<String, Field> computeValue(Class<?> type) {
                    return new ConcurrentHashMap<>();
                }
            };

    static Class<?> type(String name) throws ReflectiveOperationException {
        Class<?> found = TYPES.get(name);
        if (found != null) return found;
        found = Class.forName(name);
        TYPES.putIfAbsent(name, found);
        return found;
    }

    static Object call(Object target, String name, Class<?>[] types, Object... args)
            throws ReflectiveOperationException {
        Class<?> owner = target instanceof Class<?> c ? c : target.getClass();
        // 调用方可能复用并修改参数数组，因此缓存前须复制方法签名。
        Signature key =
                new Signature(name, types == null ? List.of() : Arrays.asList(types.clone()));
        var methods = METHODS.get(owner);
        Method method = methods.get(key);
        if (method == null) {
            method = owner.getMethod(name, types);
            methods.putIfAbsent(key, method);
        }
        return method.invoke(target instanceof Class<?> ? null : target, args);
    }

    static Object call(Object target, String name) throws ReflectiveOperationException {
        return call(target, name, NO_TYPES);
    }

    static Field field(Object target, String name) throws ReflectiveOperationException {
        var fields = FIELDS.get(target.getClass());
        Field field = fields.get(name);
        if (field != null) return field;
        field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        fields.putIfAbsent(name, field);
        return field;
    }

    static Object get(Object target, String name) throws ReflectiveOperationException {
        return field(target, name).get(target);
    }

    private FluidReflection() {}
}
