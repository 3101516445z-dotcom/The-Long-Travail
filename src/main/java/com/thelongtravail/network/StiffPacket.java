package com.thelongtravail.network;

public record StiffPacket(int remaining) {
    public static java.util.function.Consumer<StiffPacket> receiver = packet -> {};
}
