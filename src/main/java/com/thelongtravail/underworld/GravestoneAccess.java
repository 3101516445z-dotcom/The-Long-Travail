package com.thelongtravail.underworld;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

// 启动字节码检查与运行时调用共享同一份接口契约，不静态链接可选模组。
public final class GravestoneAccess {
    public static final String TILE = "de.maxhenkel.gravestone.tileentity.GraveStoneTileEntity";
    public static final String DEATH = "de.maxhenkel.gravestone.corelib.death.Death";
    public static final String GET_DEATH = "getDeath", SET_DEATH = "setDeath", GET_ITEMS = "getAllItems";
    public static final String GET_DEATH_DESC = "()Lde/maxhenkel/gravestone/corelib/death/Death;";
    public static final String SET_DEATH_DESC = "(Lde/maxhenkel/gravestone/corelib/death/Death;)V";
    public static final String GET_ITEMS_DESC = "()Lnet/minecraft/core/NonNullList;";
    private static final ClassValue<Method> DEATH_METHOD = new ClassValue<>() {
        @Override protected Method computeValue(Class<?> type) {
            Method getter = method(type, GET_DEATH, GET_DEATH_DESC);
            try { method(type, SET_DEATH, SET_DEATH_DESC, getter.getReturnType()); }
            catch (RuntimeException failure) { throw new IllegalStateException("Unsupported Gravestone API: " + type.getName(), failure); }
            method(getter.getReturnType(), GET_ITEMS, GET_ITEMS_DESC);
            return getter;
        }
    };
    private static final ClassValue<Method> ITEMS_METHOD = new ClassValue<>() {
        @Override protected Method computeValue(Class<?> type) { return method(type, GET_ITEMS, GET_ITEMS_DESC); }
    };
    private static Method method(Class<?> type, String name, String descriptor, Class<?>... parameters) {
        try {
            Method method = type.getMethod(name, parameters);
            if (Modifier.isStatic(method.getModifiers()) || !org.objectweb.asm.Type.getMethodDescriptor(method).equals(descriptor))
                throw new IllegalStateException("Unsupported Gravestone signature: " + type.getName() + "." + name);
            return method;
        } catch (NoSuchMethodException failure) {
            throw new IllegalStateException("Missing Gravestone API: " + type.getName() + "." + name, failure);
        }
    }
    private static Object invoke(Method method, Object target) {
        try { return method.invoke(target); }
        catch (InvocationTargetException failure) {
            throw new IllegalStateException("Gravestone method threw while reconciling items: " + method.getName(), failure.getCause());
        } catch (IllegalAccessException failure) {
            throw new IllegalStateException("Gravestone API is inaccessible: " + method.getName(), failure);
        }
    }
    public static Iterable<?> items(Object tile) {
        Object death = invoke(DEATH_METHOD.get(tile.getClass()), tile);
        return death == null ? null : (Iterable<?>)invoke(ITEMS_METHOD.get(death.getClass()), death);
    }
    private GravestoneAccess() {}
}
