package travail.configtests;

import com.mojang.authlib.GameProfile;
import com.thelongtravail.TravailAspect;
import com.thelongtravail.config.*;
import com.thelongtravail.config.TravailConfig.ReceivedTrigger;
import com.thelongtravail.journey.LongTravailData;
import com.thelongtravail.registry.ModRegistry;
import com.thelongtravail.valley.Phosphophyllite;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingHealEvent;

import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICurio.DropRule;
import top.theillusivec4.curios.common.inventory.CurioStacksHandler;

import java.util.*;
import java.util.function.Consumer;

// 在隔离 Forge 服务端调用真实受伤链路，结束后还原修改的配置。
public final class PhosphophylliteConfigIntegrationTest {
    private final MinecraftServer server;
    private final Map<ForgeConfigSpec.ConfigValue<?>, Object> previous = new LinkedHashMap<>();
    private final List<String> checks = new ArrayList<>();
    private final List<ServerPlayer> players = new ArrayList<>();
    private ServerPlayer observed;
    private int heals;
    private final Consumer<LivingHealEvent> listener =
            e -> {
                if (e.getEntity() == observed) heals++;
            };

    private PhosphophylliteConfigIntegrationTest(MinecraftServer server) {
        this.server = server;
    }

    private <T> void set(ForgeConfigSpec.ConfigValue<T> key, T value) {
        previous.putIfAbsent(key, key.get());
        key.set(value);
    }

    private void check(boolean value, String description) {
        if (!value) throw new AssertionError(description);
        checks.add(description);
    }

    public static List<String> run(MinecraftServer server) throws Exception {
        var test = new PhosphophylliteConfigIntegrationTest(server);
        MinecraftForge.EVENT_BUS.addListener(test.listener);
        try {
            test.verify();
            return List.copyOf(test.checks);
        } finally {
            MinecraftForge.EVENT_BUS.unregister(test.listener);
            test.previous.forEach((key, value) -> ((ForgeConfigSpec.ConfigValue) key).set(value));
            com.thelongtravail.network.TooltipConfigSync.reset();
            for (var player : test.players) {
                Phosphophyllite.forget(player);
                com.thelongtravail.combat.ReceivedMalice.forget(player);
                player.discard();
            }
        }
    }

    private ServerPlayer player() throws Exception {
        var p =
                new ServerPlayer(
                        server,
                        server.overworld(),
                        new GameProfile(UUID.randomUUID(), "PhosphSplit"));
        var wire =
                new net.minecraft.network.Connection(
                        net.minecraft.network.protocol.PacketFlow.SERVERBOUND) {
                    @Override
                    public void send(net.minecraft.network.protocol.Packet<?> packet) {}
                };
        p.connection =
                new net.minecraft.server.network.ServerGamePacketListenerImpl(server, wire, p) {
                    @Override
                    public void send(net.minecraft.network.protocol.Packet<?> packet) {}
                };
        var spawn = ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");
        spawn.setAccessible(true);
        spawn.setInt(p, 0);
        p.setPos(8, 200, 8);
        p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);
        var inventory = CuriosApi.getCuriosInventory(p).resolve().orElseThrow();
        var slots =
                new HashMap<
                        String, top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler>();
        for (String id : List.of("curio", "travel_diary"))
            slots.put(
                    id,
                    new CurioStacksHandler(inventory, id, 1, true, false, true, DropRule.DEFAULT));
        inventory.setCurios(slots);
        slots.get("curio")
                .getStacks()
                .setStackInSlot(0, new ItemStack(ModRegistry.PHOSPHOPHYLLITE.get()));
        var diary = new ItemStack(ModRegistry.LONG_TRAVAIL.get());
        LongTravailData.initialize(diary, p);
        for (var aspect : TravailAspect.values()) LongTravailData.setWitness(diary, aspect, true);
        LongTravailData.setWitness(diary, TravailAspect.FAR_REACH, false);
        slots.get("travel_diary").getStacks().setStackInSlot(0, diary);
        players.add(p);
        check(Phosphophyllite.phase(p) == 2, "Witness equipped " + players.size());
        return p;
    }

    private void hit(
            ServerPlayer p,
            boolean absorption,
            boolean expectDiary,
            boolean expectHeal,
            String name) {
        p.setHealth(100);
        p.setAbsorptionAmount(absorption ? 20 : 0);
        p.invulnerableTime = 0;
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200));
        observed = p;
        heals = 0;
        p.hurt(server.overworld().damageSources().generic(), 2);
        check(!p.hasEffect(MobEffects.MOVEMENT_SPEED) == expectDiary, name + " diary");
        check(heals == (expectHeal ? 1 : 0), name + " heal count=" + heals);
    }

    private void verify() throws Exception {
        check(
                DeepValleyItemsConfig.PHOSPHOPHYLLITE_HEAL_TRIGGER
                        .getPath()
                        .equals(List.of("items", "phosphophyllite", "healTrigger")),
                "trigger owned by phosphophyllite");
        check(
                DeepValleyItemsConfig.PHOSPHOPHYLLITE_HEAL_COOLDOWN
                        .getPath()
                        .equals(List.of("items", "phosphophyllite", "healCooldownTicks")),
                "cooldown owned by phosphophyllite");
        check(
                DeepValleyItemsConfig.PHOSPHOPHYLLITE_HEAL_EXCLUSIONS
                        .getPath()
                        .equals(List.of("items", "phosphophyllite", "healExcludedDamageTypes")),
                "exclusions owned by phosphophyllite");
        set(DeepValleyItemsConfig.VALUES.get("phosphophyllite.healChance"), 1.0);
        set(DeepValleyItemsConfig.VALUES.get("phosphophyllite.healFraction"), .1);
        set(TravailConfig.FAR_ALL_CLEAR_CHANCE, 1.0);
        set(TravailConfig.RECEIVED_COOLDOWN, 0);
        set(TravailConfig.RECEIVED_EXCLUSIONS, List.of());
        set(DeepValleyItemsConfig.PHOSPHOPHYLLITE_HEAL_COOLDOWN, 0);
        set(DeepValleyItemsConfig.PHOSPHOPHYLLITE_HEAL_EXCLUSIONS, List.of());
        for (var diary : ReceivedTrigger.values())
            for (var phosph : ReceivedTrigger.values()) {
                set(TravailConfig.RECEIVED_TRIGGER, diary);
                set(DeepValleyItemsConfig.PHOSPHOPHYLLITE_HEAL_TRIGGER, phosph);
                for (boolean absorption : new boolean[] {false, true})
                    hit(
                            player(),
                            absorption,
                            !absorption || diary == ReceivedTrigger.ATTACK_ATTEMPT,
                            !absorption || phosph == ReceivedTrigger.ATTACK_ATTEMPT,
                            diary + "/" + phosph + "/absorption=" + absorption);
            }
        set(TravailConfig.RECEIVED_TRIGGER, ReceivedTrigger.ATTACK_ATTEMPT);
        set(DeepValleyItemsConfig.PHOSPHOPHYLLITE_HEAL_TRIGGER, ReceivedTrigger.ATTACK_ATTEMPT);
        set(TravailConfig.RECEIVED_EXCLUSIONS, List.of("minecraft:generic"));
        hit(player(), true, false, true, "diary excluded, phosph allowed");
        set(TravailConfig.RECEIVED_EXCLUSIONS, List.of());
        set(DeepValleyItemsConfig.PHOSPHOPHYLLITE_HEAL_EXCLUSIONS, List.of("minecraft:generic"));
        hit(player(), true, true, false, "phosph excluded, diary allowed");
        set(DeepValleyItemsConfig.PHOSPHOPHYLLITE_HEAL_EXCLUSIONS, List.of());
        set(TravailConfig.RECEIVED_COOLDOWN, 100);
        var a = player();
        hit(a, true, true, true, "diary cooldown first");
        hit(a, true, false, true, "diary cooldown second");
        set(TravailConfig.RECEIVED_COOLDOWN, 0);
        set(DeepValleyItemsConfig.PHOSPHOPHYLLITE_HEAL_COOLDOWN, 100);
        var b = player();
        hit(b, true, true, true, "phosph cooldown first");
        hit(b, true, true, false, "phosph cooldown second");
        Phosphophyllite.forget(b);
        hit(b, true, true, true, "phosph lifecycle reset");

        // 未佩戴磷叶石时，不应消耗其后续治疗的冷却。
        var c = player();
        var slot =
                CuriosApi.getCuriosInventory(c)
                        .resolve()
                        .orElseThrow()
                        .getStacksHandler("curio")
                        .orElseThrow()
                        .getStacks();
        var accessory = slot.getStackInSlot(0);
        slot.setStackInSlot(0, ItemStack.EMPTY);
        hit(c, true, true, false, "no accessory");
        slot.setStackInSlot(0, accessory);
        hit(c, true, true, true, "newly equipped independent cooldown");

        var d = player();
        set(DeepValleyItemsConfig.VALUES.get("phosphophyllite.healChance"), 0.0);
        hit(d, true, true, false, "failed healing chance starts own cooldown");
        set(DeepValleyItemsConfig.VALUES.get("phosphophyllite.healChance"), 1.0);
        hit(d, true, true, false, "chance change does not bypass own cooldown");

        set(DeepValleyItemsConfig.PHOSPHOPHYLLITE_HEAL_TRIGGER, ReceivedTrigger.HEALTH_LOSS);
        var sync = com.thelongtravail.network.TooltipConfigSync.class;
        var snapshot = sync.getDeclaredMethod("serverValues");
        snapshot.setAccessible(true);
        var values = (Map<String, Double>) snapshot.invoke(null);
        set(DeepValleyItemsConfig.PHOSPHOPHYLLITE_HEAL_TRIGGER, ReceivedTrigger.ATTACK_ATTEMPT);
        var accept = sync.getDeclaredMethod("accept", Map.class, Map.class);
        accept.setAccessible(true);
        accept.invoke(null, values, Map.of());
        check(
                DeepValleyItemsConfig.healingRequiresHealthLossDisplay(),
                "server trigger overrides local trigger in display");
        check(
                com.thelongtravail.valley.DeepValleyItemText.details("phosphophyllite").stream()
                        .anyMatch(
                                line ->
                                        line.getContents()
                                                        instanceof
                                                        net.minecraft.network.chat.contents
                                                                                .TranslatableContents
                                                                        t
                                                && t.getKey().endsWith("witness.health_loss")),
                "catalog uses independent synced trigger");
        com.thelongtravail.network.TooltipConfigSync.reset();
        check(
                !DeepValleyItemsConfig.healingRequiresHealthLossDisplay(),
                "display reset restores local default");
        // 客户端默认值不同时，两项触发模式仍应独立显示。
        for (var diaryMode : ReceivedTrigger.values())
            for (var phosphMode : ReceivedTrigger.values()) {
                set(TravailConfig.RECEIVED_TRIGGER, diaryMode);
                set(DeepValleyItemsConfig.PHOSPHOPHYLLITE_HEAL_TRIGGER, phosphMode);
                values = (Map<String, Double>) snapshot.invoke(null);
                set(
                        TravailConfig.RECEIVED_TRIGGER,
                        diaryMode == ReceivedTrigger.ATTACK_ATTEMPT
                                ? ReceivedTrigger.HEALTH_LOSS
                                : ReceivedTrigger.ATTACK_ATTEMPT);
                set(
                        DeepValleyItemsConfig.PHOSPHOPHYLLITE_HEAL_TRIGGER,
                        phosphMode == ReceivedTrigger.ATTACK_ATTEMPT
                                ? ReceivedTrigger.HEALTH_LOSS
                                : ReceivedTrigger.ATTACK_ATTEMPT);
                accept.invoke(null, values, Map.of());
                check(
                        com.thelongtravail.network.TooltipConfigSync
                                        .receivedMaliceRequiresHealthLoss()
                                == (diaryMode == ReceivedTrigger.HEALTH_LOSS),
                        "diary synced mode " + diaryMode + "/" + phosphMode);
                check(
                        DeepValleyItemsConfig.healingRequiresHealthLossDisplay()
                                == (phosphMode == ReceivedTrigger.HEALTH_LOSS),
                        "phosph synced mode " + diaryMode + "/" + phosphMode);
                for (var aspect : List.of(TravailAspect.FAR_REACH, TravailAspect.DEEP_VALLEY)) {
                    var text = com.thelongtravail.item.LongTravailItem.aspectEffect(aspect, false);
                    var content =
                            (net.minecraft.network.chat.contents.TranslatableContents)
                                    text.getContents();
                    check(
                            content.getKey().endsWith(".health_loss")
                                    == (diaryMode == ReceivedTrigger.HEALTH_LOSS),
                            "diary actual text " + aspect + "/" + diaryMode + "/" + phosphMode);
                }
            }
    }
}
