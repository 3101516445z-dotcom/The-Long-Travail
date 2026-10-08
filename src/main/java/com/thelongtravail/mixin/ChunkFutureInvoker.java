package com.thelongtravail.mixin;

import com.mojang.datafixers.util.Either;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import java.util.concurrent.CompletableFuture;

// 1.20.1 的公开 getChunkFuture 在主线程也会 managedBlock；直接调用只提交请求的内部方法。
@Mixin(ServerChunkCache.class)
public interface ChunkFutureInvoker {
    @Invoker("getChunkFutureMainThread")
    CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> travail$requestChunk(int x, int z, ChunkStatus status, boolean create);
}
