package com.thelongtravail.compat.farmersdelight;

import com.thelongtravail.common.RuntimeFailures;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

// 复用农夫乐事的食品效果排版，缺席或发生可恢复异常时由调用方补充。
public final class FarmersDelightTooltips {
    private FarmersDelightTooltips() {}

    private static final Method FARMERS_EFFECTS = farmersEffects();

    private static Method farmersEffects() {
        if (!ModList.get().isLoaded("farmersdelight")) return null;
        return resolve("vectorwing.farmersdelight.common.utility.TextUtils");
    }

    private static Method resolve(String className) {
        try {
            var method =
                    Class.forName(className)
                            .getMethod(
                                    "addFoodEffectTooltip",
                                    ItemStack.class,
                                    List.class,
                                    float.class);
            if (!Modifier.isStatic(method.getModifiers()))
                throw new NoSuchMethodException("Food tooltip method must be static");
            return method;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            RuntimeFailures.report(
                    "Farmer's Delight food tooltip resolution",
                    new IllegalStateException("Cannot resolve optional food tooltip", e));
            return null;
        }
    }

    public static boolean append(ItemStack stack, List<Component> lines) {
        return append(FARMERS_EFFECTS, stack, lines);
    }

    private static boolean append(Method method, ItemStack stack, List<Component> lines) {
        if (method == null) return false;
        // 保留外部排版读取既有文字的行为；只有完整成功才提交修改。
        var pending = new ArrayList<>(lines);
        try {
            method.invoke(null, stack, pending, 1f);
        } catch (InvocationTargetException e) {
            var cause = e.getCause();
            if (cause instanceof Error error && !(error instanceof LinkageError)) throw error;
            failed(cause);
            return false;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            failed(e);
            return false;
        }
        lines.clear();
        lines.addAll(pending);
        return true;
    }

    private static void failed(Throwable failure) {
        RuntimeFailures.report(
                "Farmer's Delight food tooltip rendering",
                new IllegalStateException("Cannot render optional food tooltip", failure));
    }
}
