package travail.smoke;
import java.util.*;
import com.thelongtravail.data.HotPathMetrics;
public final class FrozenQueueChecks {
    private record Value(String label, Set<Long> chunks) {}
    private static void check(boolean value, String message) { if(!value) throw new AssertionError(message); }
    private static final class Queue {
        final Object queue;
        final Class<?> type;
        Queue() throws Exception {
            type=Class.forName("com.thelongtravail.boundless.IndexedFrozenQueue");
            var constructor=type.getDeclaredConstructor(java.util.function.Function.class);constructor.setAccessible(true);
            queue=constructor.newInstance((java.util.function.Function<Value,Set<Long>>)Value::chunks);
        }
        Object call(String name,Class<?>[] types,Object... arguments) {
            try { var method=type.getDeclaredMethod(name,types);method.setAccessible(true);return method.invoke(queue,arguments); }
            catch(Exception failure){throw new RuntimeException(failure);}
        }
        void put(Integer key,Value value){call("put",new Class<?>[]{Object.class,Object.class},key,value);}
        void changed(Set<Long> chunks){call("changed",new Class<?>[]{Collection.class},chunks);}
        @SuppressWarnings("unchecked") List<Value> release(java.util.function.Predicate<Value> predicate){return (List<Value>)call("release",new Class<?>[]{java.util.function.Predicate.class},predicate);}
        boolean containsKey(Integer key){return (Boolean)call("containsKey",new Class<?>[]{Object.class},key);}
    }
    public static void run() throws Exception {
        var queue = new Queue();
        for(int i=0;i<1000;i++) queue.put(i,new Value("v"+i,Set.of((long)i)));
        check(queue.release(value->false).isEmpty(),"initial frozen records remain held");
        HotPathMetrics.reset();queue.changed(Set.of(8L,9L));
        check(queue.release(value->true).stream().map(Value::label).toList().equals(List.of("v8","v9")),"candidate order remains insertion order");
        check(HotPathMetrics.Counter.RELEASE_CANDIDATES.value()==2,"unaffected chunks not visited");
        queue.put(5,new Value("changed",Set.of(5L,20L)));
        queue.release(value->false);
        queue.changed(Set.of(20L));
        check(queue.release(value->true).stream().map(Value::label).toList().equals(List.of("changed","v20")),"replacement retains order and either edge chunk invalidates");
        check(!queue.containsKey(5),"released entry removed");
        System.out.println("FROZEN_QUEUE_INDEX_PASS: unaffected chunks, replacement order, dependency chunks");
    }
}
