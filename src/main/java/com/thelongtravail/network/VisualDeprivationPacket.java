package com.thelongtravail.network;

// progress 区分进度确认与重新触发，避免每刻重启渐入。
public record VisualDeprivationPacket(int remaining, int total, boolean immediate, boolean progress) {
    public VisualDeprivationPacket(int remaining, int total, boolean immediate) { this(remaining, total, immediate, false); }
    public static java.util.function.Consumer<VisualDeprivationPacket> receiver = packet -> {};
}
