package travail.smoke;

import com.mojang.authlib.GameProfile;
import com.thelongtravail.data.StiffState;
import com.thelongtravail.event.TravailEvents;
import com.thelongtravail.registry.ModRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.Event;
import java.util.UUID;
import java.util.function.Consumer;

public final class StiffStateSmoke {
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    public static void run(ServerLevel level) throws Exception {
        var player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "StiffStateSmoke"));
        Consumer<MobEffectEvent.Applicable> immune = event -> {
            if (event.getEntity() == player && event.getEffectInstance().getEffect() == ModRegistry.STIFF.get())
                event.setResult(Event.Result.DENY);
        };
        MinecraftForge.EVENT_BUS.addListener(immune);
        try {
            StiffState.start(player, 60);
            check(!player.hasEffect(ModRegistry.STIFF.get()), "fixture denies effect icon");
            check(StiffState.active(player) && player.getSpeed() == 0, "immunity cannot block movement restriction");
            player.setDeltaMovement(0.1, 0.42, 0.2);
            new TravailEvents().onJump(new LivingEvent.LivingJumpEvent(player));
            check(player.getDeltaMovement().y == 0, "jump blocked without icon");
            player.removeAllEffects();
            check(StiffState.active(player), "cleanse cannot remove independent state");
            StiffState.start(player, 10);
            check(StiffState.remaining(player) == 60, "short refresh cannot shorten active state");
            StiffState.start(player, 100);
            check(StiffState.remaining(player) == 100, "refresh extends duration");
            var restored = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "StiffRestored"));
            restored.getPersistentData().merge(player.getPersistentData().copy());
            StiffState.resend(restored);
            check(StiffState.active(restored), "saved independent state survives reconnect");
            StiffState.clear(restored);
            new TravailEvents().onUseItemFinish(new LivingEntityUseItemEvent.Finish(player,
                    new ItemStack(Items.MILK_BUCKET), 0, new ItemStack(Items.BUCKET)));
            check(!StiffState.active(player) && player.getSpeed() > 0, "milk restores movement");
            StiffState.start(player, 20);
            level.getServer().getCommands().getDispatcher().execute("the_long_travail stiff clear",
                    player.createCommandSourceStack().withPermission(2));
            check(!StiffState.active(player), "dedicated command clears independent state");
            StiffState.start(player, 20);
            new TravailEvents().onPlayerChangedDimension(new net.minecraftforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent(
                    player, net.minecraft.world.level.Level.OVERWORLD, net.minecraft.world.level.Level.NETHER));
            check(!StiffState.active(player), "dimension change clears independent state");
            StiffState.start(player, 20);
            new TravailEvents().onPlayerRespawn(new net.minecraftforge.event.entity.player.PlayerEvent.PlayerRespawnEvent(player, false));
            check(!StiffState.active(player), "respawn clears independent state");
            StiffState.start(player, 20);
            player.getPersistentData().putLong("LongTravailStiffUntil", level.getGameTime());
            StiffState.tick(player);
            check(!StiffState.active(player) && !player.getPersistentData().contains("LongTravailStiffUntil"), "expiry clears stored state");
        } finally {
            MinecraftForge.EVENT_BUS.unregister(immune);
            StiffState.clear(player);
        }
        player.addEffect(new MobEffectInstance(ModRegistry.STIFF.get(), 100));
        check(!StiffState.active(player) && player.getSpeed() > 0, "player icon alone does not apply restriction");
        player.removeAllEffects();
        System.out.println("STIFF_STATE_PASS: immunity, cleanse, jump, refresh, saved state, milk, command, expiry, icon-only");
    }
}
