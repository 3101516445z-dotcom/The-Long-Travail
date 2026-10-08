package com.thelongtravail.boundless;
import com.thelongtravail.mixin.TimeStopTicksAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.ticks.*;
import java.util.*;
import net.minecraft.world.level.ChunkPos;
import com.thelongtravail.data.HotPathMetrics;
import com.thelongtravail.data.HotPathMetrics.Counter;
// 托管任务随存档保存，以便重启后恢复尚未执行的红石和流体任务。
public final class FrozenBlockTasks extends SavedData {
    private record TaskKey(boolean fluid,ResourceLocation type,long pos){}
    private record NeighborKey(long pos,long source,ResourceLocation block){}
    private record ShapeKey(long pos,net.minecraft.core.Direction face){}
    private record EdgeKey(long from,long to){}
    private final Map<LevelChunkTicks<?>,Long> captured=new WeakHashMap<>();
    private final Set<Long> waitingChunks=new HashSet<>();
    private final FrozenCoverage coverage = new FrozenCoverage();
    private boolean loaded(ServerLevel level,BlockPos pos){
        if(level.hasChunkAt(pos))return true;
        waitingChunks.add(net.minecraft.world.level.ChunkPos.asLong(pos));return false;
    }
    private record Task(boolean fluid,ResourceLocation type,BlockPos pos,long remaining,TickPriority priority,long order){}
    private record Shape(net.minecraft.core.Direction face,BlockPos pos,BlockPos source,int flags,int recursion){}
    private final IndexedFrozenQueue<ShapeKey,Shape> shapes = new IndexedFrozenQueue<>(t -> Set.of(ChunkPos.asLong(t.pos)));
    private record FluidEdge(BlockPos from,BlockPos to){}
    private final IndexedFrozenQueue<EdgeKey,FluidEdge> fluidEdges = new IndexedFrozenQueue<>(t -> new HashSet<>(List.of(ChunkPos.asLong(t.from), ChunkPos.asLong(t.to))));
    private record Neighbor(BlockPos pos,BlockPos source,ResourceLocation block){}
    private final IndexedFrozenQueue<TaskKey,Task> tasks = new IndexedFrozenQueue<>(t -> Set.of(ChunkPos.asLong(t.pos)));
    private final IndexedFrozenQueue<NeighborKey,Neighbor> neighbors = new IndexedFrozenQueue<>(t -> Set.of(ChunkPos.asLong(t.pos)));
    public static FrozenBlockTasks get(ServerLevel l){return l.getDataStorage().computeIfAbsent(FrozenBlockTasks::load,FrozenBlockTasks::new,"travail_frozen_tasks");}
    private static TaskKey key(boolean fluid,ResourceLocation type,BlockPos p){return new TaskKey(fluid,type,p.asLong());}
    public <T> boolean hold(ServerLevel l,ScheduledTick<T> tick,boolean fluid){
        if(!TimeStopManager.frozen(l,tick.pos()))return false;
        ResourceLocation id=fluid?BuiltInRegistries.FLUID.getKey((net.minecraft.world.level.material.Fluid)tick.type()):BuiltInRegistries.BLOCK.getKey((net.minecraft.world.level.block.Block)tick.type());
        tasks.putIfAbsent(key(fluid,id,tick.pos()),new Task(fluid,id,tick.pos().immutable(),Math.max(0,tick.triggerTick()-l.getGameTime()),tick.priority(),tick.subTickOrder()));setDirty();return true;
    }
    public boolean held(BlockPos p,Object type,boolean fluid){ResourceLocation id=fluid?BuiltInRegistries.FLUID.getKey((net.minecraft.world.level.material.Fluid)type):BuiltInRegistries.BLOCK.getKey((net.minecraft.world.level.block.Block)type);return tasks.containsKey(key(fluid,id,p));}
    @SuppressWarnings("unchecked")
    private <T> void capture(ServerLevel level, LevelTicks<T> ticks, boolean fluid) {
        var access = (TimeStopTicksAccessor<T>)(Object)ticks;
        for (long chunk : TimeStopManager.get(level).coveredChunks()) {
            var container = access.travail$containers().get(chunk);
            if (container == null) continue;
            long version = ((TickContainerRevision)container).travail$tickRevision();
            if (Objects.equals(captured.get(container), version)) continue;
            Counter.TASK_CONTAINER_SCANS.add(1);
            List<ScheduledTick<T>> removing = container.getAll().filter(tick -> {
                Counter.TASK_VISITS.add(1);
                return TimeStopManager.frozen(level, tick.pos());
            }).toList();
            if (removing.isEmpty()) {
                captured.put(container, version);
                continue;
            }
            for (var tick : removing) hold(level, tick, fluid);
            Set<ScheduledTick<T>> set = new HashSet<>(removing);
            container.removeIf(set::contains);
            captured.put(container, ((TickContainerRevision)container).travail$tickRevision());
            var next = container.peek();
            if (next == null) access.travail$nextTicks().remove(chunk);
            else access.travail$nextTicks().put(chunk, next.triggerTick());
        }
    }
    public void neighbor(ServerLevel l,BlockPos p,net.minecraft.world.level.block.Block b,BlockPos from){
        Neighbor n=new Neighbor(p.immutable(),from.immutable(),BuiltInRegistries.BLOCK.getKey(b));neighbors.put(new NeighborKey(p.asLong(),from.asLong(),n.block),n);setDirty();
    }
    public void shape(net.minecraft.core.Direction face,BlockPos pos,BlockPos source,int flags,int recursion){
        shapes.put(new ShapeKey(pos.asLong(),face),new Shape(face,pos.immutable(),source.immutable(),flags,recursion));setDirty();
    }
    public void fluidBoundary(BlockPos from,BlockPos to){fluidEdges.putIfAbsent(new EdgeKey(from.asLong(),to.asLong()),new FluidEdge(from.immutable(),to.immutable()));setDirty();}
    private void changed(Collection<Long> chunks) {
        tasks.changed(chunks);
        fluidEdges.changed(chunks);
        shapes.changed(chunks);
        neighbors.changed(chunks);
    }
    public void tick(ServerLevel level) {
        var manager = TimeStopManager.get(level);
        Set<Long> changed = coverage.changed(manager);
        changed(changed);
        var blocks = (TimeStopTicksAccessor<?>)(Object)level.getBlockTicks();
        var fluids = (TimeStopTicksAccessor<?>)(Object)level.getFluidTicks();
        for (long chunk : changed) {
            captured.remove(blocks.travail$containers().get(chunk));
            captured.remove(fluids.travail$containers().get(chunk));
        }
        Set<Long> loadedChunks = new HashSet<>();
        waitingChunks.removeIf(chunk -> {
            if (!level.hasChunkAt(new BlockPos(ChunkPos.getX(chunk)<<4, 0, ChunkPos.getZ(chunk)<<4))) return false;
            loadedChunks.add(chunk);
            return true;
        });
        changed(loadedChunks);
        capture(level, level.getBlockTicks(), false);
        capture(level, level.getFluidTicks(), true);
        long started = HotPathMetrics.start();
        try {
            releaseTasks(level);
            releaseEdges(level);
            releaseShapes(level);
            releaseNeighbors(level);
        } finally { HotPathMetrics.elapsed(Counter.RELEASE_NANOS, started); }
    }
    private void releaseTasks(ServerLevel level) {
        List<Task> ready = tasks.release(t -> !TimeStopManager.frozen(level, t.pos) && loaded(level, t.pos));
        if (!ready.isEmpty()) setDirty();
        Counter.RELEASE_TASKS.add(ready.size());
        // 所有待恢复记录先移出托管队列，再调用调度器，保留原有重入边界。
        for (Task task : ready) {
            long when = level.getGameTime() + task.remaining;
            if (task.fluid) level.getFluidTicks().schedule(new ScheduledTick<>(
                    BuiltInRegistries.FLUID.get(task.type), task.pos, when, task.priority, task.order));
            else level.getBlockTicks().schedule(new ScheduledTick<>(
                    BuiltInRegistries.BLOCK.get(task.type), task.pos, when, task.priority, task.order));
        }
    }
    private void releaseEdges(ServerLevel level) {
        List<FluidEdge> ready = fluidEdges.release(t -> !TimeStopManager.frozen(level, t.from)
                && !TimeStopManager.frozen(level, t.to) && loaded(level, t.from));
        if (!ready.isEmpty()) setDirty();
        Counter.RELEASE_EDGES.add(ready.size());
        for (FluidEdge edge : ready) {
            var state = level.getFluidState(edge.from);
            if (!state.isEmpty()) level.scheduleTick(edge.from, state.getType(), state.getType().getTickDelay(level));
        }
    }
    private void releaseShapes(ServerLevel level) {
        List<Shape> ready = shapes.release(t -> !TimeStopManager.frozen(level, t.pos) && loaded(level, t.pos));
        if (!ready.isEmpty()) setDirty();
        Counter.RELEASE_SHAPES.add(ready.size());
        for (Shape shape : ready) level.neighborShapeChanged(shape.face, level.getBlockState(shape.source),
                shape.pos, shape.source, shape.flags, shape.recursion);
    }
    private void releaseNeighbors(ServerLevel level) {
        List<Neighbor> ready = neighbors.release(t -> !TimeStopManager.frozen(level, t.pos) && loaded(level, t.pos));
        if (!ready.isEmpty()) setDirty();
        Counter.RELEASE_NEIGHBORS.add(ready.size());
        for (Neighbor neighbor : ready) level.neighborChanged(neighbor.pos,
                BuiltInRegistries.BLOCK.get(neighbor.block), neighbor.source);
    }
    @Override public CompoundTag save(CompoundTag root) {
        ListTag scheduled = new ListTag();
        for (Task task : tasks.values()) {
            CompoundTag tag = new CompoundTag();
            tag.putBoolean("fluid", task.fluid);
            tag.putString("type", task.type.toString());
            tag.putLong("pos", task.pos.asLong());
            tag.putLong("remaining", task.remaining);
            tag.putInt("priority", task.priority.getValue());
            tag.putLong("order", task.order);
            scheduled.add(tag);
        }
        root.put("tasks", scheduled);
        ListTag neighborUpdates = new ListTag();
        for (Neighbor neighbor : neighbors.values()) {
            CompoundTag tag = new CompoundTag();
            tag.putLong("pos", neighbor.pos.asLong());
            tag.putLong("source", neighbor.source.asLong());
            tag.putString("block", neighbor.block.toString());
            neighborUpdates.add(tag);
        }
        root.put("neighbors", neighborUpdates);
        ListTag shapeUpdates = new ListTag();
        for (Shape shape : shapes.values()) {
            CompoundTag tag = new CompoundTag();
            tag.putInt("face", shape.face.ordinal());
            tag.putLong("pos", shape.pos.asLong());
            tag.putLong("source", shape.source.asLong());
            tag.putInt("flags", shape.flags);
            tag.putInt("recursion", shape.recursion);
            shapeUpdates.add(tag);
        }
        root.put("shapes", shapeUpdates);
        ListTag edges = new ListTag();
        for (FluidEdge edge : fluidEdges.values()) {
            CompoundTag tag = new CompoundTag();
            tag.putLong("from", edge.from.asLong());
            tag.putLong("to", edge.to.asLong());
            edges.add(tag);
        }
        root.put("fluidEdges", edges);
        return root;
    }
    private static FrozenBlockTasks load(CompoundTag root) {
        FrozenBlockTasks out = new FrozenBlockTasks();
        for (Tag raw : root.getList("tasks", Tag.TAG_COMPOUND)) {
            CompoundTag tag = (CompoundTag)raw;
            Task task = new Task(tag.getBoolean("fluid"), new ResourceLocation(tag.getString("type")),
                    BlockPos.of(tag.getLong("pos")), Math.max(0, tag.getLong("remaining")),
                    TickPriority.byValue(tag.getInt("priority")), tag.getLong("order"));
            out.tasks.put(key(task.fluid, task.type, task.pos), task);
        }
        for (Tag raw : root.getList("neighbors", Tag.TAG_COMPOUND)) {
            CompoundTag tag = (CompoundTag)raw;
            Neighbor neighbor = new Neighbor(BlockPos.of(tag.getLong("pos")), BlockPos.of(tag.getLong("source")),
                    new ResourceLocation(tag.getString("block")));
            out.neighbors.put(new NeighborKey(neighbor.pos.asLong(), neighbor.source.asLong(), neighbor.block), neighbor);
        }
        for (Tag raw : root.getList("shapes", Tag.TAG_COMPOUND)) {
            CompoundTag tag = (CompoundTag)raw;
            Shape shape = new Shape(net.minecraft.core.Direction.from3DDataValue(tag.getInt("face")),
                    BlockPos.of(tag.getLong("pos")), BlockPos.of(tag.getLong("source")), tag.getInt("flags"), tag.getInt("recursion"));
            out.shapes.put(new ShapeKey(shape.pos.asLong(), shape.face), shape);
        }
        for (Tag raw : root.getList("fluidEdges", Tag.TAG_COMPOUND)) {
            CompoundTag tag = (CompoundTag)raw;
            FluidEdge edge = new FluidEdge(BlockPos.of(tag.getLong("from")), BlockPos.of(tag.getLong("to")));
            out.fluidEdges.put(new EdgeKey(edge.from.asLong(), edge.to.asLong()), edge);
        }
        return out;
    }
}
