package travail.smoke;
import com.mojang.authlib.GameProfile;
import com.thelongtravail.data.EffectChanges;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import java.util.UUID;

public final class MaliceRemovalSmoke {
    private static final class Victim extends FakePlayer {
        boolean blockDirect;
        int directCalls, removals, updates;
        Victim(ServerLevel level) {
            super(level, new GameProfile(UUID.randomUUID(), "MaliceStrip"));
            var wire = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
            connection = new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(), wire, this) {
                @Override public void send(net.minecraft.network.protocol.Packet<?> packet) {
                    if (packet instanceof net.minecraft.network.protocol.game.ClientboundRemoveMobEffectPacket) removals++;
                    if (packet instanceof net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket) updates++;
                }
            };
        }
        @Override public MobEffectInstance removeEffectNoUpdate(MobEffect effect) {
            directCalls++;
            return blockDirect ? null : super.removeEffectNoUpdate(effect);
        }
        void effectTick() { tickEffects(); }
    }
    private static final class MaliceCancellationProbe {
        final Victim player; int calls;
        MaliceCancellationProbe(Victim player) { this.player = player; }
        @SubscribeEvent public void remove(MobEffectEvent.Remove event) {
            if (event.getEntity() == player) { calls++; event.setCanceled(true); }
        }
    }
    static void check(boolean result, String message) { if (!result) throw new AssertionError(message); }
    public static void run(ServerLevel level) {
        var player = new Victim(level);
        var cancel = new MaliceCancellationProbe(player); MinecraftForge.EVENT_BUS.register(cancel);
        try {
            double base = player.getAttributeValue(Attributes.MOVEMENT_SPEED);
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 0));
            check(player.getAttributeValue(Attributes.MOVEMENT_SPEED) > base, "speed attribute applied");
            check(!EffectChanges.remove(player, MobEffects.MOVEMENT_SPEED, false), "witness still respects cancellation");
            int calls = cancel.calls, packets = player.removals;
            check(EffectChanges.remove(player, MobEffects.MOVEMENT_SPEED, true), "malice bypasses cancellation");
            check(cancel.calls == calls && player.removals == packets + 1, "no cancel event, exactly one removal packet");
            check(player.getAttributeValue(Attributes.MOVEMENT_SPEED) == base, "no leftover attribute");
            for (int duration : new int[]{600, -1}) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 1200, 0));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, 2));
                player.blockDirect = true;
                int attempts = player.directCalls;
                check(!EffectChanges.remove(player, MobEffects.MOVEMENT_SPEED, true), "fallback does not claim immediate removal");
                check(player.directCalls == attempts + 1, "one direct attempt only");
                check(player.getEffect(MobEffects.MOVEMENT_SPEED).getDuration() == 1, "finite/infinite duration clamped");
                player.effectTick(); EffectChanges.tick(player);
                check(!player.hasEffect(MobEffects.MOVEMENT_SPEED), "expires despite remove cancellation, no hidden revival");
                check(player.getAttributeValue(Attributes.MOVEMENT_SPEED) == base, "fallback attribute cleanup");
                player.blockDirect = false;
            }
        } finally { MinecraftForge.EVENT_BUS.unregister(cancel); EffectChanges.forget(player); }
        var lock = net.minecraftforge.registries.ForgeRegistries.MOB_EFFECTS.getValue(
                new net.minecraft.resources.ResourceLocation("more_potion_effects", "lock"));
        if (lock != null) {
            player.addEffect(new MobEffectInstance(lock, 600));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600));
            check(!EffectChanges.remove(player, MobEffects.MOVEMENT_SPEED, false), "real MPE lock intercepts ordinary removal");
            check(EffectChanges.remove(player, MobEffects.MOVEMENT_SPEED, true), "real MPE lock bypassed by malice");
            check(player.hasEffect(lock) && player.getEffect(lock).getDuration() == 600, "lock remains unchanged");
            System.out.println("TRAVAIL_MPE_LOCK_PASS: real 2.6.0 lock blocks ordinary removal, malice bypasses, lock unchanged");
        }
        System.out.println("TRAVAIL_MALICE_REMOVAL_PASS: cancellation, attribute cleanup, removal packet, single attempt, finite/infinite expiry, hidden chain");
    }
}
