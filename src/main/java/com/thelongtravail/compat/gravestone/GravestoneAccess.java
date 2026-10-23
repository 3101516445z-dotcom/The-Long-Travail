package com.thelongtravail.compat.gravestone;

import com.thelongtravail.compat.OptionalCompatState;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public final class GravestoneAccess {
    public static final String TILE = "de.maxhenkel.gravestone.tileentity.GraveStoneTileEntity";
    public static final String DEATH = "de.maxhenkel.gravestone.corelib.death.Death";
    public static final String GET_DEATH = "getDeath",
            SET_DEATH = "setDeath",
            GET_ITEMS = "getAllItems";
    public static final String GET_DEATH_DESC = "()Lde/maxhenkel/gravestone/corelib/death/Death;";
    public static final String SET_DEATH_DESC = "(Lde/maxhenkel/gravestone/corelib/death/Death;)V";
    public static final String GET_ITEMS_DESC = "()Lnet/minecraft/core/NonNullList;";
    private static final ClassValue<Method> DEATH_METHOD =
            new ClassValue<>() {
                @Override
                protected Method computeValue(Class<?> type) {
                    Method getter = method(type, GET_DEATH, GET_DEATH_DESC);
                    try {
                        method(type, SET_DEATH, SET_DEATH_DESC, getter.getReturnType());
                    } catch (Unavailable failure) {
                        throw new Unavailable(
                                "Unsupported Gravestone API: " + type.getName(), failure);
                    }
                    method(getter.getReturnType(), GET_ITEMS, GET_ITEMS_DESC);
                    return getter;
                }
            };
    private static final ClassValue<Method> ITEMS_METHOD =
            new ClassValue<>() {
                @Override
                protected Method computeValue(Class<?> type) {
                    return method(type, GET_ITEMS, GET_ITEMS_DESC);
                }
            };

    private static Method method(
            Class<?> type, String name, String descriptor, Class<?>... parameters) {
        try {
            Method method = type.getMethod(name, parameters);
            if (Modifier.isStatic(method.getModifiers())
                    || !org.objectweb.asm.Type.getMethodDescriptor(method).equals(descriptor))
                throw new Unavailable(
                        "Unsupported Gravestone signature: " + type.getName() + "." + name);
            return method;
        } catch (NoSuchMethodException failure) {
            throw new Unavailable(
                    "Missing Gravestone API: " + type.getName() + "." + name, failure);
        }
    }

    private static Object invoke(Method method, Object target) {
        try {
            return method.invoke(target);
        } catch (InvocationTargetException failure) {
            throw new IllegalStateException(
                    "Gravestone method threw while reconciling items: " + method.getName(),
                    failure.getCause());
        } catch (IllegalAccessException failure) {
            throw new Unavailable(
                    "Gravestone API is inaccessible: " + method.getName(), failure);
        }
    }

    public static Iterable<?> items(Object tile) {
        if (!OptionalCompatState.GRAVESTONE.enabled()) return null;
        try {
            Object death = invoke(DEATH_METHOD.get(tile.getClass()), tile);
            if (death == null) return null;
            Object items = invoke(ITEMS_METHOD.get(death.getClass()), death);
            if (!(items instanceof Iterable<?> contents))
                throw new Unavailable("Gravestone getAllItems did not return an inventory");
            return contents;
        } catch (Unavailable | LinkageError failure) {
            OptionalCompatState.GRAVESTONE.disable(failure.toString());
            return null;
        }
    }

    private static final class Unavailable extends RuntimeException {
        Unavailable(String message) {
            super(message);
        }

        Unavailable(String message, Exception cause) {
            super(message, cause);
        }
    }

    private GravestoneAccess() {}
}
