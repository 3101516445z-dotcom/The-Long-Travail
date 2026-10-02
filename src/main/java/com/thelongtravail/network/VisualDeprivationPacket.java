package com.thelongtravail.network;

/** 仅由服务端发往客户端，在状态变化时同步剩余和总持续刻数。 */
public record VisualDeprivationPacket(int remaining, int total, boolean immediate) {
    public static java.util.function.Consumer<VisualDeprivationPacket> receiver = packet -> {};
}
