package travail.smoke;

import com.thelongtravail.mixin.StructureRingsAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.placement.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

// 对照冻结算法，覆盖负坐标、候选顺序、预算分段及异步布局的等待与失败。
public final class WayguideCursorChecks {

    // 测试模组使用独立包，通过反射访问包内组件，避免扩大生产 API。
    private static final class Cursor {
        private final Object value;
        private final java.lang.reflect.Method ready, next;
        Cursor(ServerLevel level, BlockPos origin, int radius, StructurePlacement placement,
                List<net.minecraft.core.Holder<net.minecraft.world.level.levelgen.structure.Structure>> structures) {
            try {
                var type=Class.forName("com.thelongtravail.data.WayguidePlacementCursor");
                var constructor=type.getDeclaredConstructor(ServerLevel.class,BlockPos.class,int.class,StructurePlacement.class,List.class);
                constructor.setAccessible(true);
                value=constructor.newInstance(level,origin,radius,placement,structures);
                ready=type.getDeclaredMethod("ready",java.util.function.BooleanSupplier.class);
                next=type.getDeclaredMethod("next");ready.setAccessible(true);next.setAccessible(true);
            } catch(ReflectiveOperationException error){throw new AssertionError(error);}
        }
        private Object invoke(java.lang.reflect.Method method,Object... args) {
            try{return method.invoke(value,args);}
            catch(java.lang.reflect.InvocationTargetException error){
                if(error.getCause() instanceof RuntimeException cause)throw cause;
                throw new AssertionError(error.getCause());
            } catch(ReflectiveOperationException error){throw new AssertionError(error);}
        }
        boolean ready(java.util.function.BooleanSupplier budget){return (boolean)invoke(ready,budget);}
        ChunkPos next(){return (ChunkPos)invoke(next);}
    }

    private static int checks;
    private static void check(boolean value, String label) {
        checks++;
        if (!value) throw new AssertionError(label);
    }
    public static void run(ServerLevel level) {
        var sets = level.registryAccess().registryOrThrow(Registries.STRUCTURE_SET);
        var random = (RandomSpreadStructurePlacement) sets.stream().map(s -> s.placement())
                .filter(p -> p instanceof RandomSpreadStructurePlacement).findFirst().orElseThrow();
        var ring = (ConcentricRingsStructurePlacement) sets.stream().map(s -> s.placement())
                .filter(p -> p instanceof ConcentricRingsStructurePlacement).findFirst().orElseThrow();
        for (int x : new int[]{-10001,-513,-17,-1,0,1,17,513,10001}) {
            for (int radius : new int[]{0,1,16,257,5000}) {
                var origin = new BlockPos(x,64,-x);
                var old = new FrozenPlacementCursor(level,origin,radius,random,List.of());
                var current = new Cursor(level,origin,radius,random,List.of());
                check(old.ready(new LoadBudget(0)) == current.ready(() -> false),"spread readiness");
                for (int n=0;n<100000;n++) {
                    var a=old.next();var b=current.next();
                    check(Objects.equals(a,b),"spread candidate order");
                    if (a==null) break;
                    check(n<99999,"finite spread enumeration");
                }
            }
        }
        var map=((StructureRingsAccessor)level.getChunkSource().getGeneratorState()).travail$ringPositions();
        var previous=map.get(ring);
        try {
            var future=new CompletableFuture<List<ChunkPos>>();map.put(ring,future);
            var origin=new BlockPos(0,64,0);
            var old=new FrozenPlacementCursor(level,origin,5000,ring,List.of());
            var current=new Cursor(level,origin,5000,ring,List.of());
            check(!old.ready(new LoadBudget(5)) && !current.ready(() -> true),"pending future does not block");
            List<ChunkPos> positions=new ArrayList<>();
            for(int i=0;i<80;i++)positions.add(new ChunkPos(i%9-4,4-i%9));
            future.complete(positions);
            for(int batch=0;batch<20;batch++) {
                int allowance=batch%4==0?0:7;
                var a=new LoadBudget(allowance);var b=new LoadBudget(allowance);
                boolean ready=old.ready(a);
                check(ready==current.ready(b::take),"ring batch readiness");
                check(a.remaining==b.remaining,"same budget consumption");
                if(ready)break;
                check(batch<19,"ring completion");
            }
            // 相同距离按原始下标排序，包含重复坐标。
            var expected=new ArrayList<>(positions);
            expected.sort(Comparator.comparingDouble(p->p.getMiddleBlockPosition(64).distSqr(origin)));
            for(var p:expected){check(p.equals(current.next()),"stable ring distance order");check(p.equals(old.next()),"frozen ring order");}
            check(current.next()==null && old.next()==null,"ring exhausted");
            map.remove(ring);
            var absent=new Cursor(level,origin,5000,ring,List.of());
            check(absent.ready(()->true)&&absent.next()==null,"missing ring future");
            map.put(ring,CompletableFuture.completedFuture(List.of()));
            var empty=new Cursor(level,origin,5000,ring,List.of());
            check(empty.ready(()->true)&&empty.next()==null,"empty ring layout");
            map.put(ring,CompletableFuture.failedFuture(new IllegalStateException("fixture")));
            var failed=new Cursor(level,origin,5000,ring,List.of());
            boolean threw=false;
            try {failed.ready(()->true);} catch(java.util.concurrent.CompletionException expectedFailure){threw=true;}
            check(threw,"layout failure remains visible to the caller");
        } finally {
            if(previous==null)map.remove(ring);else map.put(ring,previous);
        }
        System.out.println("WAYGUIDE_CURSOR_PASS checks="+checks);
    }
}
