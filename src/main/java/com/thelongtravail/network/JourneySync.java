package com.thelongtravail.network;

import com.thelongtravail.TravailAspect;

/** The local viewer's personal exploration count, supplied by the server. */
public final class JourneySync {
    private static volatile int biomeCount = -1;
    private static volatile int revealedMask;

    public static int biomeCount() { return biomeCount; }
    public static boolean isRevealed(TravailAspect aspect) { return (revealedMask & aspect.mask()) != 0; }
    static void accept(int count, int revealed) { biomeCount = Math.max(0, count); revealedMask = revealed & 63; }
    public static void reset() { biomeCount = -1; revealedMask = 0; }

    private JourneySync() {}
}
