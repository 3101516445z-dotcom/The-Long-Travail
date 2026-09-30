import com.thelongtravail.client.EffectSoundPolicy;
import java.util.concurrent.atomic.AtomicInteger;
public final class EffectSoundPolicyTest {
    static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    static void ticks(EffectSoundPolicy p, int n) { for (int i=0;i<n;i++) p.advance(); }
    public static void main(String[] args) {
        AtomicInteger plays = new AtomicInteger();
        var counts = new java.util.HashMap<String, Integer>();
        var policy = new EffectSoundPolicy((reason,id) -> counts.merge(reason, 1, Integer::sum));
        policy.notification("poison", true, () -> { plays.incrementAndGet(); return true; }); ticks(policy,2);
        check(plays.get()==1,"first harmful warning preserved");
        policy.notification("poison", false, () -> { plays.incrementAndGet(); return true; });
        policy.advance(); policy.mark("poison",EffectSoundPolicy.Reason.CLEAR,1,1); policy.advance();
        check(plays.get()==1,"reason arriving after vanilla packet matched");
        policy.notification("poison",true,() -> { plays.incrementAndGet(); return true; }); ticks(policy,2);
        check(plays.get()==2,"first refill warning preserved");
        policy.mark("poison",EffectSoundPolicy.Reason.CLEAR,2,2);
        policy.notification("poison",false,() -> { plays.incrementAndGet(); return true; }); ticks(policy,2);
        policy.notification("poison",true,() -> { plays.incrementAndGet(); return true; }); ticks(policy,2);
        check(plays.get()==2,"repeated refill quiet");
        ticks(policy,41); policy.notification("poison",true,() -> { plays.incrementAndGet(); return true; }); ticks(policy,2);
        check(plays.get()==3,"outside refill window warns normally");
        policy.mark("speed",EffectSoundPolicy.Reason.GAIN,3,3);
        policy.notification("speed",true,() -> { plays.incrementAndGet(); return true; }); ticks(policy,2);
        check(plays.get()==3,"automatic gain quiet");
        policy.mark("speed",EffectSoundPolicy.Reason.EXPIRE,4,4);
        policy.notification("speed",false,() -> { plays.incrementAndGet(); return true; }); ticks(policy,2);
        check(plays.get()==3,"owned expiry quiet");
        policy.notification("external",false,() -> { plays.incrementAndGet(); return true; }); ticks(policy,2);
        check(plays.get()==4,"unknown removal still plays");
        policy.mark("stale",EffectSoundPolicy.Reason.GAIN,5,5); ticks(policy,11);
        policy.notification("stale",true,() -> { plays.incrementAndGet(); return true; }); ticks(policy,2);
        check(plays.get()==5,"expired marks never swallow later potion");
        policy.mark("one",EffectSoundPolicy.Reason.MALICE_CLEAR,6,6);
        policy.mark("two",EffectSoundPolicy.Reason.MALICE_CLEAR,7,6);
        policy.notification("one",true,() -> { plays.incrementAndGet(); return true; });
        policy.notification("two",true,() -> { plays.incrementAndGet(); return true; }); ticks(policy,2);
        check(plays.get()==6,"same malice batch merges restored positives");
        policy.notification("late",true,() -> { plays.incrementAndGet(); return true; }); ticks(policy,2);
        policy.mark("late",EffectSoundPolicy.Reason.GAIN,8,7);
        check(plays.get()==7,"excessively delayed notice falls back to normal sound");
        policy.notification("logout",true,() -> { plays.incrementAndGet(); return true; }); policy.clear(); ticks(policy,2);
        check(plays.get()==7,"logout discards stale callbacks");
        for(int i=0;i<1000;i++) policy.notification("external"+i,true,() -> { plays.incrementAndGet(); return true; });
        ticks(policy,2); check(plays.get()==1007,"bounded queue overflow preserves unrelated sounds");
        int recorded = counts.getOrDefault("play-gain", 0) + counts.getOrDefault("play-loss", 0)
                + counts.getOrDefault("overflow-play", 0);
        check(recorded == plays.get(), "normal, loss and overflow playback counted exactly once");
        check(counts.getOrDefault("overflow-play", 0) == 488, "overflow playback diagnostic preserved");
        for(int i=0;i<1000;i++) policy.notification("muted"+i,true,() -> false);
        ticks(policy,2);
        check(recorded == counts.getOrDefault("play-gain", 0) + counts.getOrDefault("play-loss", 0)
                + counts.getOrDefault("overflow-play", 0), "suppressed callbacks never add playback counts");
        System.out.println("PASS: precise matching, packet order, warnings, refill cooldown, expiry, batching, timeout, logout, queue capacity");
    }
}
