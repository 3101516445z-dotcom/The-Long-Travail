package travail.smoke;

import com.mojang.authlib.GameProfile;
import com.thelongtravail.combat.*;
import com.thelongtravail.farreach.DonQuixoteCombat;
import com.thelongtravail.flourishing.FloralCombat;
import com.thelongtravail.registry.ModAttributes;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.level.GameType;
import java.util.UUID;
import java.util.function.BiConsumer;

/** 在真实 Player/LivingEntity 结算上检查统一类型、黄心保留和属性边界。 */
final class DirectStrikeCases {
    static void run(ServerLevel level, BiConsumer<Boolean, String> check) {
        Cow actor = new Cow(EntityType.COW, level);
        for (boolean player : new boolean[] {false, true}) {
            for (int kind = 0; kind < 3; kind++) {
                for (int absorption : new int[] {0, 5, 40}) {
                    LivingEntity target;
                    if (player) {
                        ServerPlayer p =
                                new ServerPlayer(
                                        level.getServer(),
                                        level,
                                        new GameProfile(UUID.randomUUID(), "DirectStrikeTest"));
                        var wire =
                                new net.minecraft.network.Connection(
                                        net.minecraft.network.protocol.PacketFlow.SERVERBOUND) {
                                    @Override
                                    public void send(
                                            net.minecraft.network.protocol.Packet<?> packet) {}
                                };
                        p.connection =
                                new net.minecraft.server.network.ServerGamePacketListenerImpl(
                                        level.getServer(), wire, p) {
                                    @Override
                                    public void send(
                                            net.minecraft.network.protocol.Packet<?> packet) {}
                                };
                        p.setGameMode(GameType.SURVIVAL);
                        try {
                            var protection =
                                    ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");
                            protection.setAccessible(true);
                            protection.setInt(p, 0);
                        } catch (ReflectiveOperationException error) {
                            throw new AssertionError(error);
                        }
                        target = p;
                    } else target = new Cow(EntityType.COW, level);
                    target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
                    target.setHealth(100);
                    target.setAbsorptionAmount(absorption);
                    var source =
                            switch (kind) {
                                case 0 -> DirectStrikeDamage.source(level, actor, actor);
                                case 1 -> DonQuixoteCombat.strikeSource(actor);
                                default -> FloralCombat.roseSource(actor);
                            };
                    String label =
                            "direct player="
                                    + player
                                    + " kind="
                                    + kind
                                    + " absorption="
                                    + absorption;
                    check.accept(source.is(DirectStrikeDamage.TYPE), label + " unified type");
                    check.accept(
                            DonQuixoteCombat.secondary(source) == (kind == 1),
                            label + " quixote identity");
                    check.accept(
                            FloralCombat.rose(source) == (kind == 2), label + " rose identity");
                    check.accept(
                            InsightRetaliation.isRetaliation(source) == (kind == 2),
                            label + " retaliation identity");
                    target.hurt(source, 10);
                    check.accept(Math.abs(target.getHealth() - 90) < .001, label + " health loss");
                    check.accept(
                            target.getAbsorptionAmount() == absorption,
                            label + " preserves yellow hearts");
                    target.getAttribute(ModAttributes.OVERLORD.get()).setBaseValue(.1);
                    target.hurt(source, 10);
                    check.accept(
                            Math.abs(target.getHealth() - 90) < .001
                                    && target.getAbsorptionAmount() == absorption,
                            label + " overlord still blocks");
                }
            }
        }
    }
}
