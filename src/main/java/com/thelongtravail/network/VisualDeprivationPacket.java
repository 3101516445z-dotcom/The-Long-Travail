package com.thelongtravail.network;

/** S2C only: remaining and total ticks of the visual deprivation state, sent when it changes. */
public record VisualDeprivationPacket(int remaining, int total, boolean immediate) {
    public static java.util.function.Consumer<VisualDeprivationPacket> receiver = packet -> {};
}
