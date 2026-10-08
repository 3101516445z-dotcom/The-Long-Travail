package com.thelongtravail.data;

// 使用稳定相位分散周期任务，不改变执行频率，也不消耗游戏随机数。
public final class PeriodicSchedule {
    public static final int FLUID = 0x3F97A251;
    public static final int FAR_REACH = 0x6B124EC7;

    public static boolean due(long gameTime, int playerHash, int task, int interval) {
        if (interval <= 1) return true;
        int hash = playerHash ^ task;
        hash ^= hash >>> 16;
        hash *= 0x7FEB352D;
        hash ^= hash >>> 15;
        long phase = Math.floorMod(hash, interval);
        // 先对时间取模，避免接近时钟上限时发生 long 溢出。
        return (Math.floorMod(gameTime, interval) + phase) % interval == 0;
    }

    private PeriodicSchedule() {}
}
