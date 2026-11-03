package com.thelongtravail.compat.gravestone;

import com.thelongtravail.compat.OptionalCompatState;

public final class GravestoneAccess {
    public static final String TILE = "de.maxhenkel.gravestone.tileentity.GraveStoneTileEntity";
    public static final String DEATH = "de.maxhenkel.gravestone.corelib.death.Death";
    public static final String GET_DEATH = "getDeath",
            SET_DEATH = "setDeath",
            GET_ITEMS = "getAllItems";
    public static final String GET_DEATH_DESC = "()Lde/maxhenkel/gravestone/corelib/death/Death;";
    public static final String SET_DEATH_DESC = "(Lde/maxhenkel/gravestone/corelib/death/Death;)V";
    public static final String GET_ITEMS_DESC = "()Lnet/minecraft/core/NonNullList;";

    // OptionalCompatPlugin 在类加载前检查同一组公开方法签名。
    private static final class Direct {
        static Iterable<?> items(Object tile) {
            if (!(tile instanceof de.maxhenkel.gravestone.tileentity.GraveStoneTileEntity grave))
                throw new Unavailable("Unsupported Gravestone instance");
            try {
                var death = grave.getDeath();
                if (death == null) return null;
                var items = death.getAllItems();
                if (items == null)
                    throw new Unavailable("Gravestone getAllItems did not return an inventory");
                return items;
            } catch (Unavailable failure) {
                throw failure;
            } catch (Throwable failure) {
                // 原先 Method.invoke 将目标方法抛出的 Error 也交给此边界处理。
                throw new IllegalStateException(
                        "Gravestone method threw while reconciling items", failure);
            }
        }
    }

    private static final class Unavailable extends RuntimeException {
        Unavailable(String message) {
            super(message);
        }
    }

    public static Iterable<?> items(Object tile) {
        if (!OptionalCompatState.GRAVESTONE.enabled()) return null;
        try {
            return Direct.items(tile);
        } catch (Unavailable | LinkageError failure) {
            OptionalCompatState.GRAVESTONE.disable(failure.toString());
            return null;
        }
    }

    private GravestoneAccess() {}
}
