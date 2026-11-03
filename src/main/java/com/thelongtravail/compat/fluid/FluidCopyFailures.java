package com.thelongtravail.compat.fluid;

public final class FluidCopyFailures {
    public static void report(String stage, Exception failure) {
        com.thelongtravail.abyss.copy.FluidCopyFailures.report(stage, failure);
    }

    private FluidCopyFailures() {}
}
