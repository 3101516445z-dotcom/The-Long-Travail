package com.thelongtravail.data;

public final class FlourishingBonus {
    public static double calculate(int biomeCount, double perBiome, int cap) {
        double bonus = Math.max(0, biomeCount) * Math.max(0, perBiome);
        return cap > 0 ? Math.min(bonus, cap) : bonus;
    }

    private FlourishingBonus() {}
}
