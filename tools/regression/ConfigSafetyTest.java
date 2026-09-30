import com.thelongtravail.network.TooltipConfigCodec;
import com.thelongtravail.config.TravailClientConfig.HaloQuality;
import net.minecraft.network.FriendlyByteBuf;
import io.netty.buffer.Unpooled;
import java.util.*;
import java.util.function.Consumer;

public final class ConfigSafetyTest {
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private static void invalid(Consumer<FriendlyByteBuf> writer) {
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            writer.accept(buffer);
            try { TooltipConfigCodec.decode(buffer); throw new AssertionError("Malformed packet accepted"); }
            catch (IllegalArgumentException | io.netty.handler.codec.DecoderException | IndexOutOfBoundsException expected) {}
        } finally { buffer.release(); }
    }
    public static void main(String[] args) {
        var window = new com.thelongtravail.data.EffectActionWindow();
        for (int i=0;i<3;i++) check(window.acquire(19, 3), "initial window allowance");
        check(!window.acquire(20, 3), "second boundary cannot double allowance");
        check(!window.acquire(38, 3), "rolling window retained");
        check(window.acquire(39, 3), "attempts expire after 20 ticks");
        check(!window.acquire(39, 1), "lowered cap applies immediately");
        check(window.acquire(1000, 3), "idle time expires old attempts");
        check(window.acquire(5, 3), "clock rewind resets window");
        check(window.acquire(Long.MAX_VALUE, 3) && window.acquire(Long.MIN_VALUE, 3), "clock overflow safe");
        for(int i=0;i<100;i++) check(window.acquire(0, 0), "zero disables cap");
        var value = new TooltipConfigCodec.Snapshot(Map.of("test", 1.25), Map.of("pool", List.of("minecraft:plains|平原|3")));
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try { TooltipConfigCodec.encode(value, buffer); check(value.equals(TooltipConfigCodec.decode(buffer)), "round trip"); }
        finally { buffer.release(); }
        invalid(b -> b.writeVarInt(-1));
        invalid(b -> b.writeVarInt(Integer.MAX_VALUE));
        invalid(b -> { b.writeVarInt(1); b.writeUtf("x"); b.writeDouble(Double.NaN); b.writeVarInt(0); });
        invalid(b -> { b.writeVarInt(2); for (int i=0;i<2;i++) { b.writeUtf("x"); b.writeDouble(1); } b.writeVarInt(0); });
        invalid(b -> { b.writeVarInt(0); b.writeVarInt(1); b.writeUtf("pool"); b.writeVarInt(-1); });
        invalid(b -> { b.writeVarInt(0); b.writeVarInt(1); b.writeUtf("pool"); b.writeVarInt(Integer.MAX_VALUE); });
        invalid(b -> { b.writeVarInt(0); b.writeVarInt(2); for(int i=0;i<2;i++) { b.writeUtf("pool"); b.writeVarInt(0); } });
        invalid(b -> { b.writeVarInt(1); b.writeUtf("x".repeat(129)); b.writeDouble(1); b.writeVarInt(0); });
        invalid(b -> { b.writeVarInt(0); b.writeVarInt(1); b.writeUtf("pool"); b.writeVarInt(1); b.writeUtf("x".repeat(1025)); });
        invalid(b -> { b.writeVarInt(0); b.writeVarInt(0); b.writeByte(1); });
        invalid(b -> { b.writeVarInt(0); b.writeVarInt(1); b.writeUtf("truncated"); });
        invalid(b -> {
            b.writeVarInt(0); b.writeVarInt(5);
            for(int i=0;i<5;i++) { b.writeUtf("pool"+i); b.writeVarInt(2048); for(int j=0;j<2048;j++) b.writeUtf("x"); }
        });
        try { new TooltipConfigCodec.Snapshot(Map.of("infinity", Double.POSITIVE_INFINITY), Map.of()); throw new AssertionError("server accepted infinity"); }
        catch (IllegalArgumentException expected) {}
        try { new TooltipConfigCodec.Snapshot(Map.of(), Map.of("oversized", Collections.nCopies(2049, "x"))); throw new AssertionError("server accepted oversized pool"); }
        catch (IllegalArgumentException expected) {}
        for (var quality : HaloQuality.values()) for (float alpha : new float[]{0.075F, 0.035F}) {
            if (quality == HaloQuality.OFF) { check(quality.opacity(alpha) == 0, "off"); continue; }
            double before = Math.pow(1-alpha, 8), after = Math.pow(1-quality.opacity(alpha), quality.samplesPerRing);
            check(Math.abs(before-after)<0.00001, "halo opacity normalization " + quality);
        }
        System.out.println("PASS: config roundtrip, malformed lengths/keys/numbers/truncation/total limit; server validation; halo quality normalization");
    }
}
