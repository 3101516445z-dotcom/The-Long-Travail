package com.thelongtravail;

public enum TravailAspect {
    FLOURISHING("flourishing"),
    ABYSS("abyss"),
    FAR_REACH("far_reach"),
    DEEP_VALLEY("deep_valley"),
    UNDERWORLD("underworld"),
    BOUNDLESS("boundless");

    private final String id;

    TravailAspect(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public int mask() {
        return 1 << ordinal();
    }
}
