package com.thelongtravail.compat;

import java.lang.reflect.InvocationTargetException;

// 保留原先反射调用的异常边界；此类不查找成员，也不执行反射调用。
public final class DirectCalls {
    @FunctionalInterface
    public interface Call<T> {
        T run() throws Throwable;
    }

    public static <T> T call(Call<T> action) throws ReflectiveOperationException {
        try {
            return action.run();
        } catch (Throwable failure) {
            throw new InvocationTargetException(failure);
        }
    }

    // 保留存储校验和事务的异常类型，将 Error 交给原有的拒绝与回滚流程。
    public static <T> T storage(Call<T> action) throws Exception {
        try {
            return action.run();
        } catch (Exception failure) {
            throw failure;
        } catch (Throwable failure) {
            throw new InvocationTargetException(failure);
        }
    }

    private DirectCalls() {}
}
