package com.thelongtravail.network;

import java.util.UUID;
import net.minecraft.resources.ResourceLocation;

public record ExecutionTerminationPacket(ResourceLocation dimension, int id, UUID uuid, long expires, long limit) {
    public static java.util.function.Consumer<ExecutionTerminationPacket> receiver = packet -> {};
}
