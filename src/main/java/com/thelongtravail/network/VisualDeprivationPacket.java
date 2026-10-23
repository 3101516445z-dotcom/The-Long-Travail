package com.thelongtravail.network;

public record VisualDeprivationPacket(
        int remaining, int total, boolean immediate, boolean progress) {
    public VisualDeprivationPacket(int remaining, int total, boolean immediate) {
        this(remaining, total, immediate, false);
    }

    public static java.util.function.Consumer<VisualDeprivationPacket> receiver = packet -> {};
}
