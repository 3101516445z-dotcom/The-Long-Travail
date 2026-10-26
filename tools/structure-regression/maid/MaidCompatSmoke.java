package travail.smoke;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.item.ItemFilm;
import com.github.tartaricacid.touhoulittlemaid.api.event.MaidAndItemTransformEvent;
import com.mojang.authlib.GameProfile;
import com.thelongtravail.*;
import com.thelongtravail.compat.maid.*;
import com.thelongtravail.registry.ModRegistry;
import com.thelongtravail.data.LongTravailData;
import com.thelongtravail.flourishing.*;
import com.thelongtravail.config.*;
import com.thelongtravail.boundless.*;
import com.thelongtravail.farreach.*;
import com.thelongtravail.helper.CombatContext;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.effect.*;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.*;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.event.CurioEquipEvent;
import top.theillusivec4.curios.common.inventory.CurioStacksHandler;
import java.util.*;

@Mod("travail_smoke")
public final class MaidCompatSmoke {
    private static int checks;
    private static final List<String> results = new ArrayList<>();

    private static void check(boolean ok, String text) {
        if (!ok) throw new AssertionError(text);
        checks++;
        results.add("PASS " + text);
    }

    private static void near(double actual, double expected, String text) {
        check(
                Math.abs(actual - expected) < .002,
                text + " actual=" + actual + " expected=" + expected);
    }

    private static final class TestPlayer extends ServerPlayer {
        TestPlayer(ServerLevel level) {
            super(level.getServer(), level, new GameProfile(UUID.randomUUID(), "MaidTest"));
            var wire =
                    new net.minecraft.network.Connection(
                            net.minecraft.network.protocol.PacketFlow.SERVERBOUND) {
                        @Override
                        public void send(net.minecraft.network.protocol.Packet<?> p) {}
                    };
            connection =
                    new net.minecraft.server.network.ServerGamePacketListenerImpl(
                            level.getServer(), wire, this) {
                        @Override
                        public void send(net.minecraft.network.protocol.Packet<?> p) {}
                    };
        }

        boolean disconnected;

        @Override
        public boolean hasDisconnected() {
            return disconnected;
        }
    }

    // 仅用于准备测试状态；MaidSpell 会拒绝直接降低生命值。
    // 后续伤害与治疗走实际玩法入口。
    private static void fixtureHealth(EntityMaid m, float health) {
        try {
            if (net.minecraftforge.fml.ModList.get().isLoaded("touhou_little_maid_spell")) {
                var accessor =
                        (net.minecraft.network.syncher.EntityDataAccessor<Float>)
                                Class.forName(
                                                "com.github.yimeng261.maidspell.mixin.accessor.LivingEntityHealthAccessor")
                                        .getMethod("maidspell$getHealthAccessor")
                                        .invoke(null);
                Class.forName("com.github.yimeng261.maidspell.utils.MaidHealthWriteGuard")
                        .getMethod("runBypassing", Runnable.class)
                        .invoke(null, (Runnable) () -> m.getEntityData().set(accessor, health));
            } else m.setHealth(health);
        } catch (ReflectiveOperationException error) {
            throw new RuntimeException(error);
        }
    }

    private static void socialChecks(ServerLevel level) throws Exception {
        var p = new TestPlayer(level);
        ownerOnline(level, p, true);
        diary(p);
        var m = maid(level, p, 70);
        var logic = Class.forName("cn.blockforge.touhou_little_maid_personality.PersonalityLogic");
        logic.getMethod("setPersonality", Entity.class, int.class, int.class)
                .invoke(null, m, 24, 0);
        check(
                (boolean)
                        logic.getMethod("hasPersonality", Entity.class, int.class)
                                .invoke(null, m, 24),
                "social: explicit discriminating personality active");
        m.getPersistentData().putString("PersonalityDiscriminatingType", "minecraft:cow");
        var target = cow(level);
        float hp = target.getHealth();
        target.hurt(m.damageSources().mobAttack(m), 4);
        near(hp - target.getHealth(), 6, "social: personality matching target damage x1.5");
        flower(m, Flower.POPPY);
        target.invulnerableTime = 0;
        hp = target.getHealth();
        target.hurt(m.damageSources().mobAttack(m), 4);
        near(
                hp - target.getHealth(),
                6 * (1 + FlourishingItemsConfig.value(Flower.POPPY)),
                "social: personality and crown damage compose");
        m.getPersistentData().putString("PersonalityDiscriminatingType", "minecraft:zombie");
        target.invulnerableTime = 0;
        hp = target.getHealth();
        target.hurt(m.damageSources().mobAttack(m), 4);
        near(
                hp - target.getHealth(),
                2 * (1 + FlourishingItemsConfig.value(Flower.POPPY)),
                "social: personality mismatch reduction preserved");
        flower(m, Flower.PINK_TULIP);
        fixtureHealth(m, 5);
        var prayer =
                ForgeRegistries.MOB_EFFECTS.getValue(
                        new ResourceLocation("touhou_maid_affection", "maids_prayer"));
        prayer.applyEffectTick(m, 0);
        near(
                m.getHealth(),
                6 + FlourishingItemsConfig.value(Flower.PINK_TULIP),
                "social: Affection prayer heal respects pink tulip");
        var rescue =
                Class.forName("com.github.touhoumaidaffection.bond.rescue.MaidRescueContributorId");
        var contributor = (String) rescue.getMethod("ensure", EntityMaid.class).invoke(null, m);
        var stable = MaidAccessoryRuntime.identity(m);
        MaidAccessoryLedger.get(level.getServer()).entry(stable).putInt("StarCooldown", 321);
        var film = ItemFilm.maidToFilm(m);
        m.discard();
        ItemFilm.filmToMaid(film, level, new net.minecraft.core.BlockPos(70, 120, 0), p);
        EntityMaid restored = null;
        for (var e : level.getAllEntities())
            if (e instanceof EntityMaid candidate
                    && !candidate.isRemoved()
                    && MaidAccessoryRuntime.identity(candidate).equals(stable))
                restored = candidate;
        check(restored != null, "social: film restores Travail stable identity with both addons");
        check(
                contributor.equals(
                        rescue.getMethod("ensure", EntityMaid.class).invoke(null, restored)),
                "social: film also preserves Affection rescue contributor identity");
        near(
                MaidAccessoryLedger.get(level.getServer()).entry(stable).getInt("StarCooldown"),
                321,
                "social: film does not reset cooldown");
        var device =
                new ItemStack(
                        ForgeRegistries.ITEMS.getValue(
                                new ResourceLocation(
                                        "touhou_little_maid_personality",
                                        "monster_battle_device")));
        check(!device.isEmpty(), "social: actual battle device registered");
        Class.forName("cn.blockforge.touhou_little_maid_personality.item.MonsterBattleDeviceItem")
                .getMethod("recallInto", ItemStack.class, EntityMaid.class)
                .invoke(null, device, restored);
        var stored = device.getTag().getList("DeviceMaids", 10).getCompound(0).getCompound("Data");
        check(
                stored.getCompound("ForgeData").getUUID("TravailMaidIdentity").equals(stable),
                "social: battle device snapshot retains Travail identity");
        check(
                stored.getCompound("ForgeData")
                        .getString("touhou_maid_affection.rescue_contributor_id")
                        .equals(contributor),
                "social: battle device snapshot retains Affection identity");
        // 沿用 releaseAt 的 Entity.load 入口，保留实体 UUID。
        restored.discard();
        var loaded =
                (EntityMaid)
                        ForgeRegistries.ENTITY_TYPES
                                .getValue(new ResourceLocation("touhou_little_maid", "maid"))
                                .create(level);
        loaded.load(stored);
        check(level.addFreshEntity(loaded), "social: battle device snapshot loads successfully");
        check(
                MaidAccessoryRuntime.identity(loaded).equals(stable),
                "social: battle device loaded identity shares original cooldown");
        var vow = Class.forName("cn.blockforge.touhou_little_maid_personality.EternalVow");
        flower(loaded, Flower.PEONY, Flower.ORANGE_TULIP, Flower.PINK_TULIP);
        MaidAccessoryRuntime.tick(loaded);
        vow.getMethod("bind", LivingEntity.class, net.minecraft.world.entity.player.Player.class)
                .invoke(null, loaded, p);
        double withCrown = loaded.getMaxHealth();
        equip(loaded);
        MaidAccessoryRuntime.tick(loaded);
        check(
                loaded.getMaxHealth() < withCrown
                        && loaded.getMaxHealth()
                                > loaded.getAttribute(Attributes.MAX_HEALTH).getBaseValue(),
                "social: crown removal keeps vow health modifier");
        fixtureHealth(loaded, loaded.getMaxHealth());
        float before = loaded.getHealth();
        loaded.invulnerableTime = 0;
        loaded.hurt(loaded.damageSources().generic(), 4);
        near(before - loaded.getHealth(), 0, "social: vow attack cancellation preserved");
        fixtureHealth(loaded, 1);
        var deathEvent =
                new net.minecraftforge.event.entity.living.LivingDeathEvent(
                        loaded, loaded.damageSources().generic());
        MinecraftForge.EVENT_BUS.post(deathEvent);
        check(
                deathEvent.isCanceled() && loaded.getHealth() == loaded.getMaxHealth(),
                "social: vow death event handler cancels and heals");
        check(
                MaidAccessoryRuntime.identity(loaded).equals(stable),
                "social: vow binding and revival preserve Travail identity");
        storageMatrix(loaded);
        storageForeignRecovery(loaded, -1);
        storageForeignRecovery(loaded, 0);
        storageForeignRecovery(loaded, 60);
        // VowReleaseRepair 从誓约快照恢复已绑定女仆的基础值。
        // 自定义基础值使用未绑定女仆，避免誓约快照覆盖。
        var numericMaid = maid(level, p, 48);
        try {
            storageNumericBounds(numericMaid);
        } finally {
            numericMaid.discard();
        }
        // Personality 在储存后恢复全部最大生命值修饰快照。
        var freeze =
                Class.forName(
                        "cn.blockforge.touhou_little_maid_personality.emotion.MaidStorageFreeze");
        flower(loaded, Flower.PEONY);
        MaidAccessoryRuntime.tick(loaded);
        loaded.heal(1000);
        double storedMax = loaded.getMaxHealth();
        var snapshot = loaded.saveWithoutId(new CompoundTag());
        freeze.getMethod("stamp", LivingEntity.class, CompoundTag.class)
                .invoke(null, loaded, snapshot);
        long now = level.getGameTime();
        freeze.getMethod("thaw", CompoundTag.class, net.minecraft.world.level.Level.class)
                .invoke(null, snapshot, level);
        loaded.load(snapshot);
        equip(loaded);
        MaidAccessoryRuntime.tick(loaded);
        double expectedMax = loaded.getMaxHealth();
        var storageTick = freeze.getMethod("onMaidTick", LivingEntity.class, long.class);
        storageTick.invoke(null, loaded, now + 40);
        MaidAccessoryRuntime.tick(loaded);
        storageTick.invoke(null, loaded, now + 45);
        MaidAccessoryRuntime.tick(loaded);
        for (long t = now + 46; t <= now + 246; t++) {
            storageTick.invoke(null, loaded, t);
            MaidAccessoryRuntime.tick(loaded);
        }
        double afterRepair = loaded.getMaxHealth();
        java.nio.file.Files.writeString(
                java.nio.file.Path.of("maid-social-storage-probe.txt"),
                "stored="
                        + storedMax
                        + "\nexpectedAfterUnequip="
                        + expectedMax
                        + "\nactualAfterRepair="
                        + afterRepair
                        + "\nfloor="
                        + loaded.getPersistentData().getCompound("TlmpStoreHealthFloor")
                        + "\n");
        ownerOnline(level, p, false);
        loaded.discard();
        target.discard();
        near(
                afterRepair,
                expectedMax,
                "social: storage repair must not restore removed peony health");
    }

    private static void storageMatrix(EntityMaid maid) throws Exception {
        var freeze =
                Class.forName(
                        "cn.blockforge.touhou_little_maid_personality.emotion.MaidStorageFreeze");
        var stamp = freeze.getMethod("stamp", LivingEntity.class, CompoundTag.class);
        var thaw =
                freeze.getMethod("thaw", CompoundTag.class, net.minecraft.world.level.Level.class);
        var tick = freeze.getMethod("onMaidTick", LivingEntity.class, long.class);
        var attr = maid.getAttribute(Attributes.MAX_HEALTH);
        var extra =
                new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                        UUID.randomUUID(),
                        "social test external health",
                        7,
                        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation
                                .ADDITION);
        attr.addPermanentModifier(extra);
        var mult =
                new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                        UUID.randomUUID(),
                        "social test external health multiplier",
                        .3,
                        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation
                                .MULTIPLY_TOTAL);
        attr.addPermanentModifier(mult);
        for (int mode = 0; mode < 5; mode++) {
            equip(maid);
            MaidAccessoryRuntime.tick(maid);
            double plain = maid.getMaxHealth();
            flower(maid, Flower.PEONY);
            MaidAccessoryRuntime.tick(maid);
            double boosted = maid.getMaxHealth();
            if (mode == 3) {
                equip(maid);
                MaidAccessoryRuntime.tick(maid);
            }
            fixtureHealth(maid, maid.getMaxHealth());
            var data = maid.saveWithoutId(new CompoundTag());
            stamp.invoke(null, maid, data);
            if (mode != 3) {
                var snap = data.getCompound("ForgeData").getCompound("PersonalityStoreFreeze");
                near(
                        snap.getDouble("WantMax"),
                        plain,
                        "social matrix " + mode + ": snapshot excludes only peony");
                check(
                        snap.getList("HealthModifiers", 10).stream()
                                .noneMatch(
                                        tag ->
                                                ((CompoundTag) tag).hasUUID("UUID")
                                                        && ((CompoundTag) tag)
                                                                .getUUID("UUID")
                                                                .equals(
                                                                        UUID.fromString(
                                                                                "193ab8d0-46cc-4179-bb32-f0317ed65401"))),
                        "social matrix " + mode + ": peony not copied to restoration modifiers");
            }
            long now = maid.level().getGameTime();
            thaw.invoke(null, data, maid.level());
            maid.load(data);
            if (mode == 1) equip(maid); // 立即卸下。
            if (mode == 3) flower(maid, Flower.PEONY); // 释放后再佩戴。
            MaidAccessoryRuntime.tick(maid);
            for (int t = 1; t <= 246; t++) {
                if (mode == 2 && t == 42) equip(maid); // 在修复与补偿之间佩戴。
                if (mode == 4 && t == 42) flower(maid, Flower.POPPY); // 替换配花数据。
                tick.invoke(null, maid, now + t);
                MaidAccessoryRuntime.tick(maid);
            }
            near(
                    maid.getMaxHealth(),
                    mode == 0 || mode == 3 ? boosted : plain,
                    "social matrix " + mode + ": live equipment decides max health");
            check(
                    !maid.getPersistentData().contains("TlmpStoreHealthFix"),
                    "social matrix " + mode + ": no repeated health recovery request");
            check(
                    !maid.getPersistentData().contains("TlmpStoreHealthFloor"),
                    "social matrix " + mode + ": no stale compensation floor");
            check(
                    maid.getAttribute(Attributes.MAX_HEALTH).getModifier(extra.getId()) != null
                            && maid.getAttribute(Attributes.MAX_HEALTH).getModifier(mult.getId())
                                    != null,
                    "social matrix " + mode + ": foreign additive and total modifiers preserved");
            if (mode == 0)
                near(
                        maid.getHealth(),
                        boosted,
                        "social: keeping peony preserves full current health");
        }
        maid.getAttribute(Attributes.MAX_HEALTH).removeModifier(extra.getId());
        maid.getAttribute(Attributes.MAX_HEALTH).removeModifier(mult.getId());
    }

    private static void storageForeignRecovery(EntityMaid maid, int equipAt) throws Exception {
        var freeze =
                Class.forName(
                        "cn.blockforge.touhou_little_maid_personality.emotion.MaidStorageFreeze");
        var attr = maid.getAttribute(Attributes.MAX_HEALTH);
        var external =
                new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                        UUID.randomUUID(),
                        "temporary foreign modifier",
                        20,
                        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation
                                .ADDITION);
        attr.addTransientModifier(external);
        flower(maid, Flower.PEONY);
        MaidAccessoryRuntime.tick(maid);
        double expected = maid.getMaxHealth();
        if (equipAt >= 0) {
            equip(maid);
            MaidAccessoryRuntime.tick(maid);
        }
        var data = maid.saveWithoutId(new CompoundTag());
        freeze.getMethod("stamp", LivingEntity.class, CompoundTag.class).invoke(null, maid, data);
        long now = maid.level().getGameTime();
        freeze.getMethod("thaw", CompoundTag.class, net.minecraft.world.level.Level.class)
                .invoke(null, data, maid.level());
        maid.load(data);
        maid.getAttribute(Attributes.MAX_HEALTH).removeModifier(external.getId());
        if (equipAt == 0) flower(maid, Flower.PEONY);
        MaidAccessoryRuntime.tick(maid);
        var tick = freeze.getMethod("onMaidTick", LivingEntity.class, long.class);
        for (int t = 1; t <= 246; t++) {
            if (t == equipAt) flower(maid, Flower.PEONY);
            tick.invoke(null, maid, now + t);
            MaidAccessoryRuntime.tick(maid);
        }
        near(
                maid.getMaxHealth(),
                expected,
                "review: legitimate foreign recovery does not absorb equipped peony bonus equipAt="
                        + equipAt);
        equip(maid);
        MaidAccessoryRuntime.tick(maid);
        for (int t = 247; t <= 300; t++) {
            tick.invoke(null, maid, now + t);
            MaidAccessoryRuntime.tick(maid);
        }
        near(
                maid.getMaxHealth(),
                expected / (1 + FlourishingItemsConfig.value(Flower.PEONY)),
                "review: foreign compensation survives peony removal without preserving peony");
        maid.getAttribute(Attributes.MAX_HEALTH).addTransientModifier(external);
        for (int t = 301; t <= 360; t++) {
            tick.invoke(null, maid, now + t);
            MaidAccessoryRuntime.tick(maid);
        }
        near(
                maid.getMaxHealth(),
                expected / (1 + FlourishingItemsConfig.value(Flower.PEONY)),
                "review: returning foreign source removes compensation instead of doubling health");
        check(
                !maid.getPersistentData().contains("TlmpStoreHealthFloor"),
                "review: recovered foreign source clears tracked compensation");
        // 只移除本夹具的临时修饰及补偿。
        maid.getAttribute(Attributes.MAX_HEALTH).removeModifier(external.getId());
        maid.getAttribute(Attributes.MAX_HEALTH)
                .removeModifier(
                        UUID.nameUUIDFromBytes(
                                "touhou_little_maid_personality.store_freeze_health"
                                        .getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        maid.getPersistentData().remove("TlmpStoreHealthFloor");
        maid.getPersistentData().remove("TlmpStoreHealthFix");
    }

    private static void storageNumericBounds(EntityMaid maid) throws Exception {
        var freeze =
                Class.forName(
                        "cn.blockforge.touhou_little_maid_personality.emotion.MaidStorageFreeze");
        var tick = freeze.getMethod("onMaidTick", LivingEntity.class, long.class);
        double originalBase = maid.getAttribute(Attributes.MAX_HEALTH).getBaseValue();
        for (double base : new double[] {.25, 19.123456789, 1000})
            for (boolean remove : new boolean[] {false, true}) {
                maid.getAttribute(Attributes.MAX_HEALTH).setBaseValue(base);
                equip(maid);
                MaidAccessoryRuntime.tick(maid);
                double without = maid.getMaxHealth();
                flower(maid, Flower.PEONY);
                MaidAccessoryRuntime.tick(maid);
                double with = maid.getMaxHealth();
                fixtureHealth(maid, (float) with);
                var data = maid.saveWithoutId(new CompoundTag());
                freeze.getMethod("stamp", LivingEntity.class, CompoundTag.class)
                        .invoke(null, maid, data);
                long now = maid.level().getGameTime();
                freeze.getMethod("thaw", CompoundTag.class, net.minecraft.world.level.Level.class)
                        .invoke(null, data, maid.level());
                System.out.println(
                        "STORAGE_NUMERIC beforeLoad requested="
                                + base
                                + " base="
                                + maid.getAttribute(Attributes.MAX_HEALTH).getBaseValue()
                                + " max="
                                + maid.getMaxHealth()
                                + " attributes="
                                + data.getList("Attributes", 10));
                maid.load(data);
                System.out.println(
                        "STORAGE_NUMERIC afterLoad base="
                                + maid.getAttribute(Attributes.MAX_HEALTH).getBaseValue()
                                + " max="
                                + maid.getMaxHealth());
                MaidAccessoryRuntime.tick(maid);
                System.out.println(
                        "STORAGE_NUMERIC afterTLTTick base="
                                + maid.getAttribute(Attributes.MAX_HEALTH).getBaseValue()
                                + " max="
                                + maid.getMaxHealth());
                for (int t = 1; t <= 246; t++) {
                    if (remove && t == 42) equip(maid);
                    tick.invoke(null, maid, now + t);
                    MaidAccessoryRuntime.tick(maid);
                }
                near(
                        maid.getMaxHealth(),
                        remove ? without : with,
                        "review: bounded health base=" + base + " remove=" + remove);
                check(
                        !maid.getPersistentData().contains("TlmpStoreHealthFix")
                                && !maid.getPersistentData().contains("TlmpStoreHealthFloor"),
                        "review: no numerical retry or spurious floor base="
                                + base
                                + " remove="
                                + remove);
            }
        maid.getAttribute(Attributes.MAX_HEALTH).setBaseValue(originalBase);
        equip(maid);
        MaidAccessoryRuntime.tick(maid);
    }

    private static ItemStack diary(TestPlayer p) {
        var inv = CuriosApi.getCuriosInventory(p).resolve().orElseThrow();
        var slots =
                new HashMap<
                        String, top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler>();
        for (String id :
                List.of(
                        "travel_diary",
                        "head",
                        "belt",
                        "ring",
                        "charm",
                        "body",
                        "back",
                        "feet",
                        "necklace",
                        "curio"))
            slots.put(
                    id,
                    new CurioStacksHandler(inv, id, 2, true, false, true, ICurio.DropRule.DEFAULT));
        inv.setCurios(slots);
        var diary = new ItemStack(ModRegistry.LONG_TRAVAIL.get());
        check(LongTravailData.tryInitialize(diary, p), "initialize owner diary");
        for (var a : TravailAspect.values()) LongTravailData.setWitness(diary, a, true);
        slots.get("travel_diary").getStacks().setStackInSlot(0, diary);
        return diary;
    }

    private static EntityMaid maid(ServerLevel level, TestPlayer owner, double x) {
        var type =
                ForgeRegistries.ENTITY_TYPES.getValue(
                        new ResourceLocation("touhou_little_maid", "maid"));
        var m = (EntityMaid) type.create(level);
        m.setTame(true);
        m.setOwnerUUID(owner.getUUID());
        m.setNoAi(true);
        m.setPos(x, 120, 0);
        level.getChunkAt(m.blockPosition());
        check(level.addFreshEntity(m), "spawn real EntityMaid");
        return m;
    }

    private static void equip(EntityMaid m, Item... items) {
        m.getMaidBauble().clearAll();
        for (int i = 0; i < items.length; i++)
            m.getMaidBauble().setStackInSlot(i, new ItemStack(items[i]));
        MaidAccessoryRuntime.equipmentChanged(m);
    }

    private static ItemStack flower(EntityMaid m, Flower... flowers) {
        equip(m, ModRegistry.SPRING_CORNER.get());
        var stack = m.getMaidBauble().getStackInSlot(0);
        FlowerData.write(stack, List.of(flowers));
        return stack;
    }

    private static net.minecraft.world.entity.animal.Cow cow(ServerLevel level) {
        var c = EntityType.COW.create(level);
        c.setNoAi(true);
        c.setPos(4, 120, 0);
        c.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
        c.setHealth(1000);
        level.addFreshEntity(c);
        return c;
    }

    private static void ownerOnline(ServerLevel level, TestPlayer p, boolean online)
            throws Exception {
        var field = net.minecraft.server.players.PlayerList.class.getDeclaredField("playersByUUID");
        field.setAccessible(true);
        var map = (Map<UUID, ServerPlayer>) field.get(level.getServer().getPlayerList());
        if (online) map.put(p.getUUID(), p);
        else map.remove(p.getUUID());
    }

    private static LivingEntity cancelReceived;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void cancelReceived(net.minecraftforge.event.entity.living.LivingAttackEvent e) {
        if (e.getEntity() == cancelReceived) e.setCanceled(true);
    }

    private static void receivedChecks(ServerLevel level) throws Exception {
        TimeStopManager.clear();
        var p = new TestPlayer(level);
        p.setPos(0, 120, 0);
        ownerOnline(level, p, true);
        var spawn = ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");
        spawn.setAccessible(true);
        spawn.setInt(p, 0);
        var diary = diary(p);
        LongTravailData.setWitness(diary, TravailAspect.BOUNDLESS, false);
        LongTravailData.setWitness(diary, TravailAspect.DEEP_VALLEY, false);
        var m = maid(level, p, 1);
        var enemy = cow(level);
        var inv = CuriosApi.getCuriosInventory(p).resolve().orElseThrow();
        try {
            for (boolean maid : new boolean[] {false, true}) {
                LivingEntity wearer = maid ? m : p;
                var id = maid ? MaidAccessoryRuntime.identity(m) : p.getUUID();
                var data =
                        maid
                                ? MaidAccessoryLedger.get(level.getServer()).entry(id)
                                : p.getPersistentData();
                String key = maid ? "StarReceivedInterval" : StarVoiceState.RECEIVED_INTERVAL;
                if (maid) equip(m, ModRegistry.STAR_VOICE.get());
                else
                    inv.getStacksHandler("curio")
                            .orElseThrow()
                            .getStacks()
                            .setStackInSlot(0, new ItemStack(ModRegistry.STAR_VOICE.get()));
                BoundlessItemsConfig.NUMBERS.get("star.maliceInterval").set(.2);
                BoundlessItemsConfig.NUMBERS.get("star.maliceChance").set(0.0);
                cancelReceived = wearer;
                float hp = wearer.getHealth();
                wearer.hurt(wearer.damageSources().mobAttack(enemy), 1);
                near(
                        wearer.getHealth(),
                        hp,
                        "received canceled attack loses no health maid=" + maid);
                near(
                        data.getInt(key),
                        4,
                        "failed star roll starts four tick interval maid=" + maid);
                BoundlessItemsConfig.NUMBERS.get("star.maliceChance").set(1.0);
                wearer.hurt(wearer.damageSources().mobAttack(enemy), 1);
                check(
                        !TimeStopManager.locked(id),
                        "received cooldown suppresses repeated roll maid=" + maid);
                for (int i = 0; i < 3; i++) {
                    if (maid) MaidAccessoryRuntime.tick(m);
                    else StarVoiceState.tick(p);
                }
                wearer.hurt(wearer.damageSources().mobAttack(enemy), 1);
                check(!TimeStopManager.locked(id), "three ticks still blocked maid=" + maid);
                if (maid) MaidAccessoryRuntime.tick(m);
                else StarVoiceState.tick(p);
                wearer.hurt(wearer.damageSources().mobAttack(enemy), 1);
                check(
                        TimeStopManager.locked(id),
                        "four ticks canceled hit triggers star maid=" + maid);
                TimeStopManager.clear();
                data.putInt(key, 0);
                wearer.hurt(wearer.damageSources().mobAttack(enemy), 0);
                wearer.hurt(wearer.damageSources().generic(), 1);
                wearer.hurt(wearer.damageSources().mobAttack(wearer), 1);
                check(
                        !TimeStopManager.locked(id) && data.getInt(key) == 0,
                        "zero environment self do not roll maid=" + maid);
                var roseSource =
                        new net.minecraft.world.damagesource.DamageSource(
                                level.registryAccess()
                                        .registryOrThrow(
                                                net.minecraft.core.registries.Registries
                                                        .DAMAGE_TYPE)
                                        .getHolderOrThrow(FloralCombat.ROSE),
                                enemy);
                wearer.hurt(roseSource, 1);
                check(
                        !TimeStopManager.locked(id),
                        "retaliation does not start new received proc maid=" + maid);
                cancelReceived = null;
                if (maid) equip(m, ModRegistry.STAR_VOICE.get(), ModRegistry.ICARUS.get());
                else
                    inv.getStacksHandler("back")
                            .orElseThrow()
                            .getStacks()
                            .setStackInSlot(0, new ItemStack(ModRegistry.ICARUS.get()));
                hp = wearer.getHealth();
                var magicSource =
                        new net.minecraft.world.damagesource.DamageSource(
                                level.registryAccess()
                                        .registryOrThrow(
                                                net.minecraft.core.registries.Registries
                                                        .DAMAGE_TYPE)
                                        .getHolderOrThrow(
                                                net.minecraft.world.damagesource.DamageTypes.MAGIC),
                                enemy);
                wearer.hurt(magicSource, 1);
                near(
                        wearer.getHealth(),
                        hp,
                        "Icarus internal immunity preserves health maid=" + maid);
                check(
                        TimeStopManager.locked(id),
                        "internal immunity still allows received star maid=" + maid);
                TimeStopManager.clear();
                data.putInt(key, 0);
                if (!maid)
                    inv.getStacksHandler("back")
                            .orElseThrow()
                            .getStacks()
                            .setStackInSlot(0, ItemStack.EMPTY);
                if (maid
                        && net.minecraftforge.fml.ModList.get()
                                .isLoaded("touhou_little_maid_personality")) {
                    equip(m, ModRegistry.STAR_VOICE.get());
                    m.getPersistentData().putBoolean("EternalVowBound", true);
                    hp = m.getHealth();
                    // 誓约通过 Mob.doHurtTarget 转发，使用实际攻击生物。
                    // 模拟牛伤害源缺少 ATTACK_DAMAGE 属性，无法执行该攻击。
                    var vowAttacker = EntityType.ZOMBIE.create(level);
                    vowAttacker.setNoAi(true);
                    vowAttacker.setPos(3, 120, 0);
                    check(
                            level.addFreshEntity(vowAttacker),
                            "social: real attacking mob for vow redirection");
                    try {
                        m.hurt(m.damageSources().mobAttack(vowAttacker), 1);
                    } finally {
                        vowAttacker.discard();
                    }
                    near(m.getHealth(), hp, "actual Personality vow cancels damage");
                    check(
                            TimeStopManager.locked(id),
                            "actual Personality vow still triggers received star");
                    m.getPersistentData().remove("EternalVowBound");
                    TimeStopManager.clear();
                    data.putInt(key, 0);
                }
                cancelReceived = wearer;
                if (maid) equip(m, ModRegistry.AZRAEL.get());
                else {
                    inv.getStacksHandler("necklace")
                            .orElseThrow()
                            .getStacks()
                            .setStackInSlot(0, ItemStack.EMPTY);
                    inv.getStacksHandler("head")
                            .orElseThrow()
                            .getStacks()
                            .setStackInSlot(0, new ItemStack(ModRegistry.AZRAEL.get()));
                }
                AzraelConfig.MALICE.set(1.0);
                wearer.hurt(wearer.damageSources().mobAttack(enemy), 1);
                cancelReceived = null;
                var blocked = cow(level);
                blocked.setInvulnerable(true);
                blocked.hurt(wearer.damageSources().mobAttack(wearer), 1);
                var target = cow(level);
                target.hurt(wearer.damageSources().mobAttack(wearer), 1);
                if (maid)
                    new MaidAccessoryRuntime()
                            .serverTick(
                                    new TickEvent.ServerTickEvent(
                                            TickEvent.Phase.END, () -> true, level.getServer()));
                else com.thelongtravail.valley.AzraelState.drain(level.getServer());
                check(
                        !target.isAlive() && blocked.isAlive(),
                        "canceled received arms Azrael; blocked outgoing preserves opportunity maid="
                                + maid);
                var second = cow(level);
                second.hurt(wearer.damageSources().mobAttack(wearer), 1);
                if (maid)
                    new MaidAccessoryRuntime()
                            .serverTick(
                                    new TickEvent.ServerTickEvent(
                                            TickEvent.Phase.END, () -> true, level.getServer()));
                else com.thelongtravail.valley.AzraelState.drain(level.getServer());
                check(second.isAlive(), "received opportunity is consumed once maid=" + maid);
                if (maid) flower(m, Flower.ROSE_BUSH);
                else {
                    var crown = new ItemStack(ModRegistry.SPRING_CORNER.get());
                    FlowerData.write(crown, List.of(Flower.ROSE_BUSH));
                    inv.getStacksHandler("head").orElseThrow().getStacks().setStackInSlot(0, crown);
                }
                cancelReceived = wearer;
                hp = wearer.getHealth();
                float before = enemy.getHealth();
                wearer.hurt(wearer.damageSources().mobAttack(enemy), 1);
                near(wearer.getHealth(), hp, "rose canceled hit no health loss maid=" + maid);
                near(
                        before - enemy.getHealth(),
                        wearer.getMaxHealth() * FlourishingItemsConfig.value(Flower.ROSE_BUSH),
                        "rose retaliates on canceled melee maid=" + maid);
                before = enemy.getHealth();
                wearer.hurt(wearer.damageSources().mobAttack(enemy), 1);
                near(
                        enemy.getHealth(),
                        before,
                        "rose cooldown applies to canceled hits maid=" + maid);
                if (maid) equip(m, ModRegistry.SWORD_AND_LANTERN.get());
                else {
                    inv.getStacksHandler("head")
                            .orElseThrow()
                            .getStacks()
                            .setStackInSlot(0, ItemStack.EMPTY);
                    inv.getStacksHandler("belt")
                            .orElseThrow()
                            .getStacks()
                            .setStackInSlot(0, new ItemStack(ModRegistry.SWORD_AND_LANTERN.get()));
                }
                var oldDuration = com.thelongtravail.valley.SwordLanternConfig.DURATION.get();
                com.thelongtravail.valley.SwordLanternConfig.DURATION.set(0.0);
                double base =
                        maid
                                ? MaidLanternCombat.bonus(m, null)
                                : com.thelongtravail.valley.SwordLanternState.bonus(p, null);
                wearer.hurt(wearer.damageSources().mobAttack(enemy), 1);
                level.getServer()
                        .getWorldData()
                        .overworldData()
                        .setGameTime(level.getGameTime() + 1);
                double after =
                        maid
                                ? MaidLanternCombat.bonus(m, null)
                                : com.thelongtravail.valley.SwordLanternState.bonus(p, null);
                near(
                        after - base,
                        com.thelongtravail.valley.SwordLanternConfig.PROLONGED_BONUS.get(),
                        "canceled attack starts lantern combat maid=" + maid);
                com.thelongtravail.valley.SwordLanternConfig.DURATION.set(oldDuration);
                if (maid) equip(m);
                else
                    inv.getStacksHandler("belt")
                            .orElseThrow()
                            .getStacks()
                            .setStackInSlot(0, ItemStack.EMPTY);
                cancelReceived = null;
                blocked.discard();
                target.discard();
                second.discard();
            }
        } finally {
            cancelReceived = null;
            TimeStopManager.clear();
            m.discard();
            enemy.discard();
            ownerOnline(level, p, false);
        }
    }

    private static void conservativeOptimizationChecks(ServerLevel level, TestPlayer owner) {
        var bare = maid(level, owner, 8);
        var id = MaidAccessoryRuntime.identity(bare);
        var ledger = MaidAccessoryLedger.get(level.getServer());
        MaidAccessoryRuntime.tick(bare);
        check(ledger.find(id) == null, "bare maid does not create cooldown record");
        ledger.entry(id).putInt("StarCooldown", 3);
        MaidAccessoryRuntime.tick(bare);
        check(
                ledger.find(id).getInt("StarCooldown") == 2,
                "unequipped maid retains ticking cooldown");
        bare.discard();

        for (var entries :
                java.util.List.of(
                        java.util.List.<String>of(),
                        java.util.List.of(
                                "invalid",
                                "poppy",
                                "minecraft:poppy",
                                "peony",
                                "lilac",
                                "rose_bush"),
                        java.util.List.of("peony", "lilac", "rose_bush", "poppy"))) {
            var stack = new ItemStack(ModRegistry.SPRING_CORNER.get());
            var tags = new net.minecraft.nbt.ListTag();
            for (String entry : entries) tags.add(net.minecraft.nbt.StringTag.valueOf(entry));
            stack.getOrCreateTag().put("TravailFlowers", tags);
            for (Flower flower : Flower.values()) {
                boolean expected =
                        FlourishingItemsConfig.ENABLED.get(flower).get()
                                && FlowerData.read(stack).contains(flower);
                check(
                        FlowerData.active(stack, flower) == expected,
                        "flower query preserves parsing semantics " + entries + " " + flower);
            }
        }
    }

    public MaidCompatSmoke() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    private static CompoundTag lampSnapshot(EntityMaid maid) {
        CompoundTag tag = new CompoundTag();
        var slots = maid.getMaidBauble();
        for (int i = 0; i < slots.getSlots(); i++)
            if (slots.getStackInSlot(i).is(ModRegistry.SWORD_AND_LANTERN.get()))
                tag.put(Integer.toString(i), slots.getStackInSlot(i).save(new CompoundTag()));
        return tag.isEmpty() ? null : tag;
    }

    private static void lanternRegression(ServerLevel level, TestPlayer owner) throws Exception {
        var maid = maid(level, owner, 8);
        boolean tracing = net.minecraftforge.fml.ModList.get().isLoaded("travail_damage_trace");
        if (tracing)
            level.getServer()
                    .getCommands()
                    .performPrefixedCommand(
                            level.getServer().createCommandSourceStack(),
                            "travail_damage_trace start " + maid.getUUID());
        var client =
                new com.github.tartaricacid.touhoulittlemaid.inventory.handler.BaubleItemHandler(
                        maid.getMaidBauble().getSlots());
        var ordinary = new ItemStack(ModRegistry.SPRING_CORNER.get());
        maid.getMaidBauble().setStackInSlot(1, ordinary.copy());
        client.setStackInSlot(1, ordinary.copy());
        var build =
                MaidAccessoryRuntime.class.getDeclaredMethod(
                        "lanternChanges", EntityMaid.class, CompoundTag.class, CompoundTag.class);
        build.setAccessible(true);
        var full =
                com.github.tartaricacid.touhoulittlemaid.network.message.SyncBaubleMessage.class
                        .getDeclaredField("isFull");
        full.setAccessible(true);
        var baubles =
                com.github.tartaricacid.touhoulittlemaid.network.message.SyncBaubleMessage.class
                        .getDeclaredField("baubles");
        baubles.setAccessible(true);
        CompoundTag previous = null;
        maid.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(10);
        double attack = maid.getAttributeValue(Attributes.ATTACK_DAMAGE);
        for (int step = 0; step < 5; step++) {
            var slots = maid.getMaidBauble();
            if (step == 0)
                slots.setStackInSlot(0, new ItemStack(ModRegistry.SWORD_AND_LANTERN.get()));
            if (step == 1) com.thelongtravail.valley.LanternSources.toggle(slots.getStackInSlot(0));
            if (step == 2) {
                slots.setStackInSlot(2, slots.getStackInSlot(0).copy());
                slots.setStackInSlot(0, ItemStack.EMPTY);
            }
            if (step == 3) slots.setStackInSlot(2, new ItemStack(ModRegistry.THOUSAND_YEARS.get()));
            if (step == 4) slots.setStackInSlot(2, ItemStack.EMPTY);
            var current = lampSnapshot(maid);
            var messages =
                    (List<
                                    com.github.tartaricacid.touhoulittlemaid.network.message
                                            .SyncBaubleMessage>)
                            build.invoke(null, maid, previous, current);
            check(
                    messages.size() == (step == 2 ? 2 : step == 4 ? 0 : 1),
                    "lamp delta count step=" + step);
            for (var message : messages) {
                var buffer =
                        new net.minecraft.network.FriendlyByteBuf(
                                io.netty.buffer.Unpooled.buffer());
                try {
                    com.github.tartaricacid.touhoulittlemaid.network.message.SyncBaubleMessage
                            .encode(message, buffer);
                    var decoded =
                            com.github.tartaricacid.touhoulittlemaid.network.message
                                    .SyncBaubleMessage.decode(buffer);
                    check(
                            !full.getBoolean(decoded),
                            "lamp never clears client inventory step=" + step);
                    var updates =
                            (it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap<ItemStack>)
                                    baubles.get(decoded);
                    updates.forEach((slot, stack) -> client.setStackInSlot(slot, stack));
                } finally {
                    buffer.release();
                }
            }
            check(
                    ItemStack.matches(client.getStackInSlot(1), ordinary),
                    "unrelated client bauble survives step=" + step);
            if (step < 4)
                for (int slot : new int[] {0, 2})
                    check(
                            ItemStack.matches(
                                    client.getStackInSlot(slot), slots.getStackInSlot(slot)),
                            "lamp slot matches authoritative inventory step="
                                    + step
                                    + " slot="
                                    + slot);
            MaidAccessoryRuntime.tick(maid);
            near(
                    maid.getAttributeValue(Attributes.ATTACK_DAMAGE),
                    attack,
                    "lamp leaves attack attribute unchanged step=" + step);
            var target = cow(level);
            float hp = target.getHealth();
            double bonus = MaidAccessories.bonus(target, maid.damageSources().mobAttack(maid));
            check(maid.doHurtTarget(target), "actual maid melee accepted step=" + step);
            near(
                    hp - target.getHealth(),
                    attack * (1 + bonus),
                    "actual maid melee damage through lamp lifecycle step=" + step);
            previous = current;
        }
        if (tracing) {
            MinecraftForge.EVENT_BUS.post(
                    new TickEvent.ServerTickEvent(
                            TickEvent.Phase.END, () -> true, level.getServer()));
            level.getServer()
                    .getCommands()
                    .performPrefixedCommand(
                            level.getServer().createCommandSourceStack(),
                            "travail_damage_trace stop");
        }
        maid.discard();
    }

    private static void quixoteChecks(ServerLevel level) throws Exception {
        var p = new TestPlayer(level);
        ownerOnline(level, p, true);
        var diary = diary(p);
        var m = maid(level, p, 80);
        var other = maid(level, p, 82);
        var target = cow(level);
        m.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModRegistry.DON_QUIXOTE.get()));
        check(!DonQuixoteCombat.equipped(m), "quixote: hand is not an accessory");
        equip(m, ModRegistry.DON_QUIXOTE.get());
        equip(other, ModRegistry.DON_QUIXOTE.get());
        var source = m.damageSources().mobAttack(m);
        var snap = DonQuixoteCombat.snapshot(target, source);
        check(
                snap != null && snap.bonus() > 0,
                "quixote: native bauble active against large target");
        m.setModelId("touhou_little_maid:hakurei_reimu");
        near(
                DonQuixoteCombat.snapshot(target, source).bonus(),
                snap.bonus(),
                "quixote: model does not change size bonus");
        equip(m, ModRegistry.DON_QUIXOTE.get(), ModRegistry.DON_QUIXOTE.get());
        near(
                DonQuixoteCombat.snapshot(target, source).bonus(),
                snap.bonus(),
                "quixote: duplicates do not stack");
        equip(m, ModRegistry.DON_QUIXOTE.get());
        ownerOnline(level, p, false);
        check(
                !DonQuixoteCombat.snapshot(target, source).witness(),
                "quixote: offline owner loses witness");
        float before = target.getHealth();
        target.invulnerableTime = 0;
        target.hurt(source, 10);
        near(
                before - target.getHealth(),
                10 * (1 + snap.bonus()),
                "quixote: real base damage works without owner");
        ownerOnline(level, p, true);
        LongTravailData.setWitness(diary, TravailAspect.FAR_REACH, false);
        FarReachItemsConfig.NUMBERS.get("don_quixote.healthRatio").set(500.0);
        fixtureHealth(m, 2);
        target.setHealth(1000);
        target.invulnerableTime = 0;
        check(
                DonQuixoteCombat.snapshot(target, source).strike(),
                "quixote: ratio uses maid health, not owner");
        target.hurt(source, 10);
        near(
                1000 - target.getHealth(),
                10 * (1 + snap.bonus()) + 20,
                "quixote: malice real extra damage exactly once");
        target.setHealth(1000);
        var arrow = new net.minecraft.world.entity.projectile.Arrow(level, m);
        check(
                DonQuixoteCombat.snapshot(target, m.damageSources().arrow(arrow, m)).strike(),
                "quixote: projectile attributed to maid");
        cancelReceived = target;
        target.invulnerableTime = 0;
        before = target.getHealth();
        target.hurt(source, 10);
        cancelReceived = null;
        near(target.getHealth(), before, "quixote: canceled attack does not trigger true damage");
        target.setHealth(1000);
        target.invulnerableTime = 0;
        target.hurt(m.damageSources().arrow(arrow, m), 10);
        near(
                1000 - target.getHealth(),
                10 * (1 + snap.bonus()) + 20,
                "quixote: real projectile damage and extra strike");
        check(MaidAccessories.quixoteCooldown(m) == 0, "quixote: malice does not trigger witness");
        LongTravailData.setWitness(diary, TravailAspect.FAR_REACH, true);
        target.setHealth(1000);
        target.invulnerableTime = 0;
        check(
                !DonQuixoteCombat.snapshot(target, source).strike(),
                "quixote: witness excludes malice at 500 ratio");
        target.hurt(source, 10);
        near(
                1000 - target.getHealth(),
                10 * (1 + snap.bonus()),
                "quixote: witness actual damage has no extra true damage");
        fixtureHealth(m, 20);
        var threshold = DonQuixoteCombat.snapshot(target, source);
        DonQuixoteCombat.finish(target, source, threshold, 50);
        near(
                MaidAccessories.quixoteCooldown(m),
                0,
                "quixote: exactly 5 percent does not trigger witness");
        target.setHealth(1000);
        target.invulnerableTime = 0;
        target.hurt(source, 60);
        near(MaidAccessories.quixoteCooldown(m), 80, "quixote: real hit starts 4 second cooldown");
        near(MaidAccessories.quixoteCooldown(other), 0, "quixote: independent maid cooldown");
        near(
                DonQuixoteState.reduce(m, target.damageSources().mobAttack(target), 10),
                .1,
                "quixote: designated enemy reduced 99 percent");
        near(
                DonQuixoteState.reduce(m, other.damageSources().mobAttack(other), 10),
                10,
                "quixote: other attackers unaffected");
        before = m.getHealth();
        m.invulnerableTime = 0;
        m.hurt(target.damageSources().mobAttack(target), 10);
        near(before - m.getHealth(), .1, "quixote: real incoming damage reduced");
        MaidAccessoryRuntime.tick(m);
        near(
                DonQuixoteState.reduce(m, target.damageSources().mobAttack(target), 10),
                .1,
                "quixote: first runtime tick preserves valid guard");
        DonQuixoteState.trigger(m, other);
        near(
                DonQuixoteState.reduce(m, target.damageSources().mobAttack(target), 10),
                .1,
                "quixote: cooldown prevents replacing guarded enemy");
        var originalTime = level.getGameTime();
        ((net.minecraft.world.level.storage.ServerLevelData) level.getLevelData())
                .setGameTime(originalTime + 40);
        near(
                DonQuixoteState.reduce(m, target.damageSources().mobAttack(target), 10),
                10,
                "quixote: guard expires at 40 ticks");
        ((net.minecraft.world.level.storage.ServerLevelData) level.getLevelData())
                .setGameTime(originalTime);
        DonQuixoteState.tick(m);
        near(MaidAccessories.quixoteCooldown(m), 78, "quixote: loaded cooldown ticks");
        equip(m);
        equip(m, ModRegistry.DON_QUIXOTE.get());
        near(
                DonQuixoteState.reduce(m, target.damageSources().mobAttack(target), 10),
                10,
                "quixote: unequip clears guard");
        near(MaidAccessories.quixoteCooldown(m), 78, "quixote: unequip does not reset cooldown");
        MaidAccessories.quixoteCooldown(m, 0);
        DonQuixoteState.trigger(m, target);
        ownerOnline(level, p, false);
        near(
                DonQuixoteState.reduce(m, target.damageSources().mobAttack(target), 10),
                10,
                "quixote: owner offline clears guard immediately");
        ownerOnline(level, p, true);
        MaidAccessories.quixoteCooldown(m, 0);
        DonQuixoteState.trigger(m, target);
        m.setOwnerUUID(UUID.randomUUID());
        near(
                DonQuixoteState.reduce(m, target.damageSources().mobAttack(target), 10),
                10,
                "quixote: changed owner clears guard");
        m.setOwnerUUID(p.getUUID());
        var identity = MaidAccessoryRuntime.identity(m);
        var film = ItemFilm.maidToFilm(m);
        m.discard();
        near(
                MaidAccessoryLedger.get(level.getServer())
                        .entry(identity)
                        .getInt("QuixoteCooldown"),
                80,
                "quixote: storage preserves cooldown");
        ItemFilm.filmToMaid(film, level, new net.minecraft.core.BlockPos(85, 120, 0), p);
        EntityMaid restored = null;
        for (var e : level.getAllEntities())
            if (e instanceof EntityMaid maid
                    && identity.equals(MaidAccessoryRuntime.identity(maid))) restored = maid;
        check(restored != null, "quixote: real film restores stable identity");
        near(
                MaidAccessories.quixoteCooldown(restored),
                80,
                "quixote: real film cannot reset cooldown");
        near(
                DonQuixoteState.reduce(restored, target.damageSources().mobAttack(target), 10),
                10,
                "quixote: film does not restore guard");
        var saved = MaidAccessoryLedger.get(level.getServer()).save(new CompoundTag());
        near(
                MaidAccessoryLedger.load(saved).find(identity).getInt("QuixoteCooldown"),
                80,
                "quixote: ledger reload retains cooldown");
        restored.discard();
        other.discard();
        target.discard();
        ownerOnline(level, p, false);
    }

    private static void accessPressure(ServerLevel level) throws Exception {
        int initial = checks;
        var p = new TestPlayer(level);
        var m = maid(level, p, 18);
        ownerOnline(level, p, true);
        try {
            var diary = diary(p);
            var items = MaidAccessories.items();
            var random = new Random(20261024L);
            for (int cycle = 0; cycle < 1000; cycle++) {
                var slots = m.getMaidBauble();
                for (int slot = 0; slot < slots.getSlots(); slot++) {
                    int choice = random.nextInt(items.size() + 1);
                    slots.setStackInSlot(
                            slot,
                            choice == items.size()
                                    ? ItemStack.EMPTY
                                    : new ItemStack(items.get(choice)));
                }
                for (Item item : items) {
                    ItemStack expected = ItemStack.EMPTY;
                    for (int slot = 0; slot < slots.getSlots(); slot++) {
                        if (slots.getStackInSlot(slot).is(item)) {
                            expected = slots.getStackInSlot(slot);
                            break;
                        }
                    }
                    check(
                            MaidAccessoryRuntime.equipped(m, item) == expected,
                            "access: same first stack reference");
                }
                ownerOnline(level, p, cycle % 2 == 0);
                check(
                        MaidAccessoryRuntime.owner(m) == (cycle % 2 == 0 ? p : null),
                        "access: online owner toggle");
                UUID id = MaidAccessoryRuntime.identity(m);
                var tag = m.getPersistentData().copy();
                check(
                        id.equals(MaidAccessoryRuntime.identity(m))
                                && tag.equals(m.getPersistentData()),
                        "access: stable identity read");
            }
            ownerOnline(level, p, true);
            for (int mask = 0; mask < 64; mask++) {
                for (var a : TravailAspect.values())
                    LongTravailData.setWitness(diary, a, (mask & a.mask()) != 0);
                for (var a : TravailAspect.values())
                    check(
                            MaidAccessoryRuntime.phase(m, a)
                                    == ((mask & a.mask()) != 0
                                            ? MaidAccessoryRuntime.Phase.WITNESS
                                            : MaidAccessoryRuntime.Phase.MALICE),
                            "access: phase mask");
            }
            ownerOnline(level, p, false);
            for (var a : TravailAspect.values())
                check(
                        MaidAccessoryRuntime.phase(m, a) == MaidAccessoryRuntime.Phase.INACTIVE,
                        "access: offline phase");
            equip(m, ModRegistry.STAR_VOICE.get());
            fixtureHealth(m, 0);
            check(
                    MaidAccessoryRuntime.equipped(m, ModRegistry.STAR_VOICE.get()).isEmpty(),
                    "access: dead maid");
            fixtureHealth(m, 20);
            m.discard();
            check(
                    MaidAccessoryRuntime.equipped(m, ModRegistry.STAR_VOICE.get()).isEmpty(),
                    "access: removed maid");
            System.out.println(
                    "MAID_ACCESS_PRESSURE_PASS checks="
                            + (checks - initial)
                            + " layouts=1000 seed=20261024");
        } finally {
            ownerOnline(level, p, false);
            m.discard();
        }
    }

    private static int expectedPhase(int mask, TravailAspect aspect, boolean active) {
        return active ? ((mask & aspect.mask()) != 0 ? 2 : 1) : 0;
    }

    private static void rulePressure(ServerLevel level) throws Exception {
        int initial = checks;
        var p = new TestPlayer(level);
        var q = new TestPlayer(level);
        ownerOnline(level, p, true);
        ownerOnline(level, q, true);
        var diary = diary(p);
        var otherDiary = diary(q);
        var diarySlot =
                CuriosApi.getCuriosInventory(p)
                        .resolve()
                        .orElseThrow()
                        .getStacksHandler("travel_diary")
                        .orElseThrow()
                        .getStacks();
        var m = maid(level, p, 24);
        var items = MaidAccessories.items();
        int layouts = 0;
        try {
            for (int mask = 0; mask < 64; mask++) {
                for (var aspect : TravailAspect.values()) {
                    LongTravailData.setWitness(diary, aspect, (mask & aspect.mask()) != 0);
                    LongTravailData.setWitness(otherDiary, aspect, (mask & aspect.mask()) == 0);
                }
                for (int state = 0; state < 5; state++) {
                    ownerOnline(level, p, state != 1);
                    p.disconnected = state == 4;
                    diarySlot.setStackInSlot(0, state == 2 ? ItemStack.EMPTY : diary);
                    m.setOwnerUUID(state == 3 ? q.getUUID() : p.getUUID());
                    boolean active = state == 0 || state == 3;
                    int effectiveMask = state == 3 ? mask ^ 63 : mask;
                    for (int selection = -1; selection < items.size(); selection++) {
                        m.getMaidBauble().clearAll();
                        Item item = selection < 0 ? null : items.get(selection);
                        ItemStack stack = item == null ? ItemStack.EMPTY : new ItemStack(item);
                        if (item != null) m.getMaidBauble().setStackInSlot(0, stack);
                        var before = diary.getTag().copy();
                        var beforeOther = otherDiary.getTag().copy();
                        for (var aspect : TravailAspect.values()) {
                            int expected = expectedPhase(effectiveMask, aspect, active);
                            var phase =
                                    expected == 0
                                            ? MaidAccessoryRuntime.Phase.INACTIVE
                                            : expected == 1
                                                    ? MaidAccessoryRuntime.Phase.MALICE
                                                    : MaidAccessoryRuntime.Phase.WITNESS;
                            check(
                                    MaidAccessoryRuntime.phase(m, aspect) == phase,
                                    "rules: theme phase matrix");
                        }
                        check(
                                MaidAccessories.lorentzPhase(m)
                                        == expectedPhase(
                                                effectiveMask, TravailAspect.BOUNDLESS, active),
                                "rules: lorentz phase independent of equipment");
                        check(
                                MaidAccessories.quixotePhase(m)
                                        == expectedPhase(
                                                effectiveMask, TravailAspect.FAR_REACH, active),
                                "rules: quixote phase independent of equipment");
                        check(
                                MaidAccessories.flowerPhase(m)
                                        == expectedPhase(
                                                effectiveMask, TravailAspect.FLOURISHING, active),
                                "rules: flower phase");
                        check(
                                MaidAccessories.lafitePhase(m)
                                        == expectedPhase(
                                                effectiveMask, TravailAspect.ABYSS, active),
                                "rules: lafite phase");
                        check(
                                MaidAccessories.relicPhase(m)
                                        == expectedPhase(
                                                effectiveMask, TravailAspect.UNDERWORLD, active),
                                "rules: relic phase");
                        check(
                                MaidAccessories.phosphophyllitePhase(m)
                                        == expectedPhase(
                                                effectiveMask,
                                                TravailAspect.DEEP_VALLEY,
                                                active
                                                        && item
                                                                == ModRegistry.PHOSPHOPHYLLITE
                                                                        .get()),
                                "rules: phosphophyllite requires equipment");
                        check(
                                MaidAccessories.quixote(m)
                                        == (item == ModRegistry.DON_QUIXOTE.get()),
                                "rules: quixote equipment");
                        check(
                                MaidAccessories.lafite(m)
                                        == (item == ModRegistry.MIDNIGHT_LAFITE.get()),
                                "rules: lafite equipment");
                        check(
                                MaidAccessories.seesUndead(m)
                                        == (item == ModRegistry.THOUSAND_YEARS.get()
                                                && expectedPhase(
                                                                effectiveMask,
                                                                TravailAspect.UNDERWORLD,
                                                                active)
                                                        == 2),
                                "rules: undead witness qualification");
                        check(
                                MaidAccessories.lorentzStack(m)
                                        == (item == ModRegistry.LORENTZ_FANTASY.get()
                                                ? stack
                                                : ItemStack.EMPTY),
                                "rules: lorentz original stack");
                        check(
                                MaidAccessories.flowerStack(m)
                                        == (item == ModRegistry.EVERLASTING_FLOWER.get()
                                                ? stack
                                                : ItemStack.EMPTY),
                                "rules: flower original stack");
                        check(
                                MaidAccessories.relicStack(m, true)
                                        == (item == ModRegistry.SOUL_BANNER.get()
                                                ? stack
                                                : ItemStack.EMPTY),
                                "rules: banner original stack");
                        check(
                                MaidAccessories.relicStack(m, false)
                                        == (item == ModRegistry.DEMON_KING.get()
                                                ? stack
                                                : ItemStack.EMPTY),
                                "rules: relic original stack");
                        check(
                                before.equals(diary.getTag())
                                        && beforeOther.equals(otherDiary.getTag()),
                                "rules: owner diaries not mutated");
                        layouts++;
                    }
                }
            }
            p.disconnected = false;
            ownerOnline(level, p, true);
            diarySlot.setStackInSlot(0, diary);
            m.setOwnerUUID(p.getUUID());
            equip(m, ModRegistry.PHOSPHOPHYLLITE.get());
            fixtureHealth(m, 0);
            check(
                    MaidAccessories.phosphophyllitePhase(m) == 0,
                    "rules: dead maid cannot supply equipment");
            check(
                    MaidAccessories.quixotePhase(m) == 2,
                    "rules: phase-only query retains owner phase for dead maid");
            fixtureHealth(m, 20);
            m.discard();
            check(
                    MaidAccessories.phosphophyllitePhase(m) == 0,
                    "rules: removed maid cannot supply equipment");
            check(
                    MaidAccessories.quixotePhase(p) == 0
                            && !MaidAccessories.quixote(p)
                            && MaidAccessories.relicPhase(p) == 0
                            && MaidAccessories.flowerStack(p).isEmpty(),
                    "rules: non-maid fallback");
            System.out.println(
                    "MAID_RULES_PRESSURE_PASS checks="
                            + (checks - initial)
                            + " layouts="
                            + layouts
                            + " masks=64 ownerStates=5");
        } finally {
            p.disconnected = false;
            diarySlot.setStackInSlot(0, diary);
            ownerOnline(level, p, false);
            ownerOnline(level, q, false);
            m.discard();
        }
    }

    private static void calculationPressure(ServerLevel level) throws Exception {
        int initial = checks;
        var p = new TestPlayer(level);
        ownerOnline(level, p, true);
        var diary = diary(p);
        var m = maid(level, p, 30);
        var victim = cow(level);
        boolean rain = level.getLevelData().isRaining(),
                thunder = level.getLevelData().isThundering();
        boolean local = RainConfig.EKI_LOCAL.get(), sky = RainConfig.EKI_SKY.get();
        double chance = RainConfig.IMMUNITY.get();
        try {
            RainConfig.EKI_LOCAL.set(false);
            RainConfig.EKI_SKY.set(false);
            RainConfig.IMMUNITY.set(.37);
            level.setWeatherParameters(6000, 6000, true, false);
            var sources =
                    new net.minecraft.world.damagesource.DamageSource[] {
                        m.damageSources().mobAttack(m), m.damageSources().fall(),
                                m.damageSources().onFire(),
                        m.damageSources().magic(), m.damageSources().genericKill(),
                                m.damageSources()
                                        .arrow(
                                                new net.minecraft.world.entity.projectile.Arrow(
                                                        level, m),
                                                m)
                    };
            float[] amounts = {0, -1, .5F, 10, Float.MAX_VALUE, Float.NaN, Float.POSITIVE_INFINITY};
            for (int trial = 0; trial < 1000; trial++) {
                for (var a : TravailAspect.values())
                    LongTravailData.setWitness(diary, a, (trial & a.mask()) != 0);
                flower(
                        m,
                        Flower.POPPY,
                        Flower.CORNFLOWER,
                        Flower.ALLIUM,
                        Flower.AZURE_BLUET,
                        Flower.TORCHFLOWER,
                        Flower.WHITE_TULIP);
                m.getMaidBauble()
                        .setStackInSlot(
                                1,
                                new ItemStack(
                                        trial % 3 == 0
                                                ? ModRegistry.EKI.get()
                                                : trial % 3 == 1
                                                        ? ModRegistry.ICARUS.get()
                                                        : ModRegistry.THOUSAND_YEARS.get()));
                fixtureHealth(m, trial % 2 == 0 ? 2 : m.getMaxHealth());
                for (var source : sources) {
                    float amount = amounts[trial % amounts.length];
                    double oldBonus = LegacyMaidCombatMath.bonus(victim, source);
                    check(
                            Double.doubleToLongBits(oldBonus)
                                    == Double.doubleToLongBits(
                                            MaidAccessories.bonus(victim, source)),
                            "math: bonus exact");
                    check(
                            Double.doubleToLongBits(
                                            LegacyMaidCombatMath.witherBonus(victim, source))
                                    == Double.doubleToLongBits(
                                            MaidAccessories.witherBonus(victim, source)),
                            "math: wither exact");
                    float oldReduction =
                            LegacyMaidCombatMath.reduce(m, source, amount, trial % 2 == 0);
                    check(
                            Float.floatToIntBits(oldReduction)
                                    == Float.floatToIntBits(
                                            MaidAccessories.reduce(
                                                    m, source, amount, trial % 2 == 0)),
                            "math: reduction exact including nonfinite");
                    long seed = 20261024L + trial;
                    m.getRandom().setSeed(seed);
                    boolean oldImmune = LegacyMaidCombatMath.immune(m, source, amount);
                    long oldNext = m.getRandom().nextLong();
                    m.getRandom().setSeed(seed);
                    boolean newImmune = MaidAccessories.immune(m, source, amount);
                    long newNext = m.getRandom().nextLong();
                    check(oldImmune == newImmune, "math: seeded immunity exact");
                    check(oldNext == newNext, "math: RNG consumption unchanged");
                }
            }
            System.out.println(
                    "MAID_MATH_PRESSURE_PASS checks="
                            + (checks - initial)
                            + " trials=1000 sources=6");
        } finally {
            RainConfig.EKI_LOCAL.set(local);
            RainConfig.EKI_SKY.set(sky);
            RainConfig.IMMUNITY.set(chance);
            level.setWeatherParameters(6000, 6000, rain, thunder);
            ownerOnline(level, p, false);
            m.discard();
            victim.discard();
        }
    }

    private static void periodicPressure(ServerLevel level) throws Exception {
        int initial = checks;
        TimeStopManager.clear();
        var p = new TestPlayer(level);
        ownerOnline(level, p, true);
        diary(p);
        var m = maid(level, p, 34);
        var interval = FlourishingItemsConfig.NUMBERS.get("flowers.oxeye_daisy.intervalSeconds");
        var healing = FlourishingItemsConfig.NUMBERS.get("flowers.oxeye_daisy.healing");
        double oldInterval = interval.get(), oldHealing = healing.get();
        try {
            interval.set(.1);
            healing.set(2.0);
            for (int cycle = 0; cycle < 100; cycle++) {
                equip(m);
                MaidAccessoryRuntime.tick(m);
                double base = m.getMaxHealth();
                flower(m, Flower.PEONY, Flower.OXEYE_DAISY);
                fixtureHealth(m, 2);
                MaidAccessoryRuntime.tick(m);
                double boosted = m.getMaxHealth();
                near(m.getHealth(), 2, "periodic: first tick does not heal");
                MaidAccessoryRuntime.tick(m);
                near(m.getHealth(), 4, "periodic: second tick heals once");
                near(m.getMaxHealth(), boosted, "periodic: modifiers do not accumulate");
                equip(m);
                MaidAccessoryRuntime.tick(m);
                near(m.getMaxHealth(), base, "periodic: unequip restores max health");
                var data =
                        MaidAccessoryLedger.get(level.getServer())
                                .entry(MaidAccessoryRuntime.identity(m));
                data.putInt("StarCooldown", 3);
                data.putInt("StarInterval", 2);
                data.putInt("StarReceivedInterval", 1);
                MaidAccessoryRuntime.tick(m);
                check(
                        data.getInt("StarCooldown") == 2
                                && data.getInt("StarInterval") == 1
                                && data.getInt("StarReceivedInterval") == 0,
                        "periodic: three cooldown clocks tick once");
            }
            System.out.println(
                    "MAID_PERIODIC_PRESSURE_PASS checks=" + (checks - initial) + " cycles=100");
        } finally {
            interval.set(oldInterval);
            healing.set(oldHealing);
            ownerOnline(level, p, false);
            m.discard();
        }
    }

    private static java.lang.reflect.Field runtimeField(String name) throws Exception {
        java.lang.reflect.Field field;
        try {
            field = MaidAccessoryRuntime.class.getDeclaredField(name);
        } catch (NoSuchFieldException moved) {
            field =
                    Class.forName("com.thelongtravail.compat.maid.MaidRuntimeState")
                            .getDeclaredField(name);
        }
        field.setAccessible(true);
        return field;
    }

    private static java.lang.reflect.Field stateField(Object state, String name) throws Exception {
        var field = state.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static void lifecyclePressure(ServerLevel level) throws Exception {
        int initial = checks;
        var runtime = new MaidAccessoryRuntime();
        var stop = new net.minecraftforge.event.server.ServerStoppedEvent(level.getServer());
        runtime.stopped(stop);
        var states = (Map<?, ?>) runtimeField("STATES").get(null);
        var pending = (ArrayDeque<Runnable>) runtimeField("PENDING").get(null);
        var times = (Map<LivingEntity, Map<String, Long>>) runtimeField("EFFECT_TIMES").get(null);
        var p = new TestPlayer(level);
        var q = new TestPlayer(level);
        ownerOnline(level, p, true);
        ownerOnline(level, q, true);
        diary(p);
        diary(q);
        try {
            for (int cycle = 0; cycle < 100; cycle++) {
                var m = maid(level, p, 38);
                equip(m, ModRegistry.AZRAEL.get());
                MaidAccessoryRuntime.tick(m);
                var state = states.get(m);
                stateField(state, "armed").setBoolean(state, true);
                long generation = stateField(state, "generation").getLong(state);
                m.setOwnerUUID(q.getUUID());
                MaidAccessoryRuntime.tick(m);
                check(
                        !stateField(state, "armed").getBoolean(state)
                                && stateField(state, "generation").getLong(state) > generation,
                        "lifecycle: owner change invalidates armed action");
                stateField(state, "armed").setBoolean(state, true);
                generation = stateField(state, "generation").getLong(state);
                equip(m);
                check(
                        !stateField(state, "armed").getBoolean(state)
                                && stateField(state, "generation").getLong(state) > generation,
                        "lifecycle: unequip invalidates armed action");
                if (cycle % 2 == 0) {
                    m.discard();
                    check(!states.containsKey(m), "lifecycle: leave forgets state");
                } else {
                    fixtureHealth(m, 0);
                    runtime.serverTick(
                            new TickEvent.ServerTickEvent(
                                    TickEvent.Phase.END, () -> true, level.getServer()));
                    check(!states.containsKey(m), "lifecycle: dead maid swept");
                    m.discard();
                }
            }
            List<String> order = new ArrayList<>();
            pending.add(
                    () -> {
                        order.add("A");
                        pending.add(() -> order.add("C"));
                    });
            pending.add(() -> order.add("B"));
            runtime.serverTick(
                    new TickEvent.ServerTickEvent(
                            TickEvent.Phase.START, () -> true, level.getServer()));
            check(order.isEmpty(), "lifecycle: START does not drain");
            runtime.serverTick(
                    new TickEvent.ServerTickEvent(
                            TickEvent.Phase.END, () -> true, level.getServer()));
            check(
                    order.equals(List.of("A", "B")),
                    "lifecycle: END drains initial FIFO snapshot only");
            runtime.serverTick(
                    new TickEvent.ServerTickEvent(
                            TickEvent.Phase.END, () -> true, level.getServer()));
            check(
                    order.equals(List.of("A", "B", "C")),
                    "lifecycle: reentrant task waits for next END");
            var m = maid(level, p, 38);
            times.put(m, new HashMap<>(Map.of("probe", Long.MAX_VALUE)));
            pending.add(
                    () -> {
                        throw new AssertionError("stop must discard pending work");
                    });
            runtime.stopped(stop);
            check(
                    states.isEmpty() && times.isEmpty() && pending.isEmpty(),
                    "lifecycle: stop clears caches and pending work");
            m.discard();
            System.out.println(
                    "MAID_LIFECYCLE_PRESSURE_PASS checks=" + (checks - initial) + " cycles=100");
        } finally {
            runtime.stopped(stop);
            ownerOnline(level, p, false);
            ownerOnline(level, q, false);
        }
    }

    @SubscribeEvent
    public void started(ServerStartedEvent event) {
        var server = event.getServer();
        var level = server.overworld();
        boolean pass = false;
        try {
            var p = new TestPlayer(level);
            p.setPos(0, 120, 0);
            ownerOnline(level, p, true);
            var diary = diary(p);
            var diaryBefore = diary.copy();
            var m = maid(level, p, 0);
            var other = maid(level, p, 2);
            var victim = cow(level);
            accessPressure(level);
            rulePressure(level);
            // 本场景显式设置 99% 减伤。
            var reduction = FarReachItemsConfig.NUMBERS.get("don_quixote.reduction");
            double previousReduction = reduction.get();
            try {
                reduction.set(.99);
                quixoteChecks(level);
            } finally {
                reduction.set(previousReduction);
            }
            conservativeOptimizationChecks(level, p);
            lanternRegression(level, p);
            check(MaidAccessories.isMaid(m), "optional facade detects maid");
            for (var item : MaidAccessoryRuntime.items())
                check(
                        m.getMaidBauble().isItemValid(0, new ItemStack(item)),
                        "native bauble registered " + ForgeRegistries.ITEMS.getKey(item));
            check(
                    !m.getMaidBauble()
                            .isItemValid(0, new ItemStack(ModRegistry.LONG_TRAVAIL.get())),
                    "diary excluded from native baubles");
            check(
                    !m.getMaidBauble()
                            .isItemValid(0, new ItemStack(ModRegistry.BOOK_OF_DEAD.get())),
                    "revival book excluded");
            for (var entry : ForgeRegistries.ITEMS.getEntries())
                if (entry.getKey().location().getNamespace().equals("the_long_travail")) {
                    var e =
                            new CurioEquipEvent(
                                    new ItemStack(entry.getValue()),
                                    new SlotContext("curio", m, 0, false, true));
                    MinecraftForge.EVENT_BUS.post(e);
                    check(
                            e.getResult() == Event.Result.DENY,
                            "namespace denied " + entry.getKey().location());
                }
            var allowed =
                    new CurioEquipEvent(
                            new ItemStack(ModRegistry.EKI.get()),
                            new SlotContext("curio", p, 0, false, true));
            MinecraftForge.EVENT_BUS.post(allowed);
            check(allowed.getResult() != Event.Result.DENY, "player Curios not globally denied");
            // 駅的位置开关与主人资格实时查询。
            level.setWeatherParameters(0, 6000, true, true);
            RainConfig.EKI_LOCAL.set(false);
            RainConfig.EKI_SKY.set(false);
            RainConfig.IMMUNITY.set(1.0);
            equip(m);
            m.getAvailableInv(false).setStackInSlot(0, new ItemStack(ModRegistry.EKI.get()));
            m.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModRegistry.EKI.get()));
            check(
                    MaidAccessoryRuntime.eki(m) == MaidAccessoryRuntime.Phase.INACTIVE,
                    "maid inventory and hand Eki inactive");
            var ci = CuriosApi.getCuriosInventory(m).resolve().orElseThrow();
            ci.setCurios(
                    Map.of(
                            "curio",
                            new CurioStacksHandler(
                                    ci, "curio", 1, true, false, true, ICurio.DropRule.DEFAULT)));
            ci.getStacksHandler("curio")
                    .orElseThrow()
                    .getStacks()
                    .setStackInSlot(0, new ItemStack(ModRegistry.EKI.get()));
            check(
                    MaidAccessoryRuntime.eki(m) == MaidAccessoryRuntime.Phase.INACTIVE,
                    "forced maid Curios Eki inactive");
            equip(m, ModRegistry.EKI.get());
            equip(other, ModRegistry.EKI.get());
            check(
                    MaidAccessoryRuntime.eki(m) == MaidAccessoryRuntime.Phase.WITNESS
                            && MaidAccessoryRuntime.eki(other)
                                    == MaidAccessoryRuntime.Phase.WITNESS,
                    "one diary serves two separately equipped maids");
            check(MaidAccessories.immune(m, m.damageSources().generic(), 10), "Eki witness immune");
            check(
                    !MaidAccessories.immune(m, m.damageSources().genericKill(), 10),
                    "Eki does not block generic kill");
            check(
                    ItemStack.matches(diary, diaryBefore),
                    "initial eligibility checks do not write diary");
            LongTravailData.setWitness(diary, TravailAspect.ABYSS, false);
            check(
                    MaidAccessoryRuntime.eki(m) == MaidAccessoryRuntime.Phase.MALICE,
                    "owner witness change immediate");
            near(
                    MaidAccessories.bonus(victim, m.damageSources().mobAttack(m)),
                    RainConfig.DAMAGE.get(),
                    "Eki malice bonus");
            MaidAccessoryRuntime.tick(m);
            near(
                    m.getAttributeValue(Attributes.MOVEMENT_SPEED),
                    m.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue()
                            * (1 + RainConfig.SPEED.get()),
                    "Eki maid speed");
            ownerOnline(level, p, false);
            check(
                    MaidAccessoryRuntime.eki(m) == MaidAccessoryRuntime.Phase.INACTIVE,
                    "offline owner disables diary branch");
            MaidAccessoryRuntime.tick(m);
            near(
                    m.getAttributeValue(Attributes.MOVEMENT_SPEED),
                    m.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue(),
                    "offline owner removes speed");
            ownerOnline(level, p, true);
            var originalOwner = m.getOwnerUUID();
            m.setOwnerUUID(UUID.randomUUID());
            check(
                    MaidAccessoryRuntime.eki(m) == MaidAccessoryRuntime.Phase.INACTIVE,
                    "new owner has no inherited diary");
            m.setOwnerUUID(originalOwner);
            var ds =
                    CuriosApi.getCuriosInventory(p)
                            .resolve()
                            .orElseThrow()
                            .getStacksHandler("travel_diary")
                            .orElseThrow()
                            .getStacks();
            ds.setStackInSlot(0, ItemStack.EMPTY);
            check(
                    MaidAccessoryRuntime.eki(m) == MaidAccessoryRuntime.Phase.INACTIVE,
                    "unequipped owner diary disables maid");
            ds.setStackInSlot(0, diary);
            LongTravailData.setWitness(diary, TravailAspect.ABYSS, true);
            diaryBefore = diary.copy();
            p.getInventory().add(new ItemStack(ModRegistry.EKI.get()));
            check(
                    com.thelongtravail.abyss.RainState.carrying(p),
                    "player Eki inventory carry retained");
            // 配花战斗入口、数据刷新、去重与清理。
            equip(other);
            equip(m);
            m.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            level.setWeatherParameters(6000, 0, false, false);
            var crown = flower(m, Flower.POPPY, Flower.ORANGE_TULIP, Flower.PEONY);
            MaidAccessoryRuntime.tick(m);
            near(
                    m.getMaxHealth(),
                    m.getAttribute(Attributes.MAX_HEALTH).getBaseValue()
                            * (1 + FlourishingItemsConfig.value(Flower.PEONY)),
                    "peony max health");
            near(
                    m.getAttributeValue(Attributes.ATTACK_SPEED),
                    m.getAttribute(Attributes.ATTACK_SPEED).getBaseValue()
                            * (1 + FlourishingItemsConfig.value(Flower.ORANGE_TULIP)),
                    "orange tulip attack speed");
            float hp = victim.getHealth();
            victim.invulnerableTime = 0;
            victim.hurt(m.damageSources().mobAttack(m), 10);
            near(
                    hp - victim.getHealth(),
                    10 * (1 + FlourishingItemsConfig.value(Flower.POPPY)),
                    "native maid flower actual damage");
            m.getMaidBauble().setStackInSlot(1, crown.copy());
            near(
                    MaidAccessories.bonus(victim, m.damageSources().mobAttack(m)),
                    FlourishingItemsConfig.value(Flower.POPPY),
                    "duplicate native crowns do not stack");
            FlowerData.write(
                    crown, List.of(Flower.BLUE_ORCHID, Flower.PINK_TULIP, Flower.OXEYE_DAISY));
            MaidAccessoryRuntime.tick(m);
            near(
                    m.getAttributeValue(ForgeMod.SWIM_SPEED.get()),
                    1 + FlourishingItemsConfig.value(Flower.BLUE_ORCHID),
                    "in-place flower NBT refresh");
            fixtureHealth(m, 5);
            m.heal(2);
            near(
                    m.getHealth(),
                    5 + 2 * (1 + FlourishingItemsConfig.value(Flower.PINK_TULIP)),
                    "pink tulip healing event");
            m.getMaidBauble().setStackInSlot(1, ItemStack.EMPTY);
            FlowerData.write(crown, List.of(Flower.PEONY));
            MaidAccessoryRuntime.tick(m);
            m.heal(100);
            near(m.getHealth(), m.getMaxHealth(), "heal to peony boosted maximum");
            equip(m);
            MaidAccessoryRuntime.tick(m);
            near(m.getAttributeValue(ForgeMod.SWIM_SPEED.get()), 1, "unequip clears swim modifier");
            near(
                    m.getMaxHealth(),
                    m.getAttribute(Attributes.MAX_HEALTH).getBaseValue(),
                    "unequip clears health modifier");
            check(m.getHealth() <= m.getMaxHealth(), "unequip clamps health to reduced maximum");
            flower(m, Flower.LILY_OF_THE_VALLEY, Flower.WITHER_ROSE, Flower.LILAC);
            victim.invulnerableTime = 0;
            victim.hurt(m.damageSources().mobAttack(m), 1);
            check(
                    victim.hasEffect(MobEffects.POISON) && victim.hasEffect(MobEffects.WITHER),
                    "maid hit applies poison and wither");
            near(
                    victim.getEffect(MobEffects.POISON).getDuration(),
                    Math.ceil(
                            FlourishingItemsConfig.ticks(
                                            FlourishingItemsConfig.extra(
                                                    Flower.LILY_OF_THE_VALLEY, "durationSeconds"))
                                    * (1 + FlourishingItemsConfig.value(Flower.LILAC))),
                    "lilac extends poison");
            var incoming = new MobEffectInstance(MobEffects.WEAKNESS, 100);
            victim.addEffect(incoming, m);
            near(incoming.getDuration(), 100, "effect input remains unmodified");
            flower(m, Flower.ALLIUM, Flower.AZURE_BLUET, Flower.TORCHFLOWER);
            near(
                    MaidAccessories.reduce(m, m.damageSources().magic(), 10, true),
                    10 * (1 - FlourishingItemsConfig.value(Flower.ALLIUM)),
                    "allium poison reduction");
            near(
                    MaidAccessories.reduce(m, m.damageSources().fall(), 10, false),
                    10 * (1 - FlourishingItemsConfig.value(Flower.AZURE_BLUET)),
                    "azure bluet fall reduction");
            near(
                    MaidAccessories.reduce(m, m.damageSources().inFire(), 10, false),
                    10 * (1 - FlourishingItemsConfig.value(Flower.TORCHFLOWER)),
                    "torchflower fire reduction");
            flower(m, Flower.RED_TULIP, Flower.WHITE_TULIP, Flower.PINK_PETALS);
            fixtureHealth(m, 2);
            MaidAccessoryRuntime.tick(m);
            near(
                    MaidAccessories.bonus(victim, m.damageSources().mobAttack(m)),
                    FlourishingItemsConfig.value(Flower.RED_TULIP),
                    "low health red tulip");
            near(
                    MaidAccessories.reduce(m, m.damageSources().generic(), 10, false),
                    10 * (1 - FlourishingItemsConfig.value(Flower.WHITE_TULIP)),
                    "low health white tulip");
            near(
                    m.getAttributeValue(Attributes.MOVEMENT_SPEED),
                    m.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue()
                            * (1 + FlourishingItemsConfig.value(Flower.PINK_PETALS)),
                    "pink petals movement");
            flower(m, Flower.SUNFLOWER, Flower.CORNFLOWER);
            level.setDayTime(6000);
            var arrow = new net.minecraft.world.entity.projectile.Arrow(level, m);
            var arrowDamage = m.damageSources().arrow(arrow, m);
            near(
                    MaidAccessories.bonus(victim, arrowDamage),
                    FlourishingItemsConfig.value(Flower.SUNFLOWER)
                            + FlourishingItemsConfig.value(Flower.CORNFLOWER),
                    "maid projectile and sunlight bonuses");
            MaidAccessoryRuntime.tick(m);
            near(
                    m.getAttributeValue(Attributes.MOVEMENT_SPEED),
                    m.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue()
                            * (1 + FlourishingItemsConfig.extra(Flower.SUNFLOWER, "speedBonus")),
                    "sunflower movement");
            flower(m, Flower.OXEYE_DAISY);
            fixtureHealth(m, 2);
            for (int i = 0;
                    i
                            < FlourishingItemsConfig.ticks(
                                    FlourishingItemsConfig.extra(
                                            Flower.OXEYE_DAISY, "intervalSeconds"));
                    i++) MaidAccessoryRuntime.tick(m);
            near(
                    m.getHealth(),
                    2 + FlourishingItemsConfig.value(Flower.OXEYE_DAISY),
                    "daisy timed regeneration");
            flower(m, Flower.ROSE_BUSH);
            fixtureHealth(m, m.getMaxHealth());
            m.invulnerableTime = 0;
            hp = victim.getHealth();
            m.hurt(m.damageSources().mobAttack(victim), 1);
            near(
                    hp - victim.getHealth(),
                    m.getMaxHealth() * FlourishingItemsConfig.value(Flower.ROSE_BUSH),
                    "rose actual retaliation");
            m.invulnerableTime = 0;
            hp = victim.getHealth();
            m.hurt(m.damageSources().mobAttack(victim), 1);
            near(victim.getHealth(), hp, "rose retaliation cooldown");
            flower(m, Flower.PITCHER_PLANT);
            fixtureHealth(m, 2);
            var zombie = EntityType.ZOMBIE.create(level);
            zombie.setPos(4, 120, 0);
            zombie.setNoAi(true);
            level.addFreshEntity(zombie);
            zombie.hurt(m.damageSources().mobAttack(m), 1000);
            new MaidAccessoryRuntime()
                    .serverTick(
                            new TickEvent.ServerTickEvent(TickEvent.Phase.END, () -> true, server));
            near(
                    m.getHealth(),
                    2 + m.getMaxHealth() * FlourishingItemsConfig.value(Flower.PITCHER_PLANT),
                    "pitcher heals on confirmed hostile death");
            var foreignOwner =
                    new TestPlayer(server.getLevel(net.minecraft.world.level.Level.NETHER));
            ownerOnline(level, foreignOwner, true);
            diary(foreignOwner);
            m.setOwnerUUID(foreignOwner.getUUID());
            equip(m, ModRegistry.THOUSAND_YEARS.get());
            check(MaidAccessories.seesUndead(m), "cross-dimension online owner diary");
            m.setOwnerUUID(p.getUUID());
            ownerOnline(level, foreignOwner, false);
            // 伊卡洛斯基础效果独立生效，魔法免疫需要见证。
            equip(m, ModRegistry.ICARUS.get());
            ownerOnline(level, p, false);
            check(
                    MaidAccessories.immune(m, m.damageSources().fall(), 5),
                    "Icarus base fall immunity without owner");
            check(
                    !MaidAccessories.immune(m, m.damageSources().magic(), 5),
                    "Icarus no borrowed witness while offline");
            victim.invulnerableTime = 0;
            victim.hurt(m.damageSources().mobAttack(m), 1);
            check(victim.isOnFire(), "Icarus actual hit ignites without owner");
            ownerOnline(level, p, true);
            check(
                    MaidAccessories.immune(m, m.damageSources().magic(), 5),
                    "Icarus owner witness magic immunity");
            // 一千年后接入原有附魔计算。
            equip(m, ModRegistry.THOUSAND_YEARS.get());
            victim.removeAllEffects();
            check(MaidAccessories.seesUndead(m), "maid sees undead from owner witness");
            var sword = new ItemStack(Items.DIAMOND_SWORD);
            sword.enchant(Enchantments.SMITE, 5);
            m.setItemSlot(EquipmentSlot.MAINHAND, sword);
            victim.invulnerableTime = 0;
            hp = victim.getHealth();
            m.doHurtTarget(victim);
            double with = hp - victim.getHealth();
            equip(m);
            victim.invulnerableTime = 0;
            hp = victim.getHealth();
            m.doHurtTarget(victim);
            double without = hp - victim.getHealth();
            near(with - without, 12.5, "real maid melee Smite on living cow");
            check(ItemStack.matches(diary, diaryBefore), "combat and flowers do not write diary");
            equip(m, ModRegistry.THOUSAND_YEARS.get());
            LongTravailData.setWitness(diary, TravailAspect.UNDERWORLD, false);
            victim.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 2));
            near(
                    MaidAccessories.witherBonus(victim, m.damageSources().mobAttack(m)),
                    3 * UnderworldItemsConfig.WITHER_BONUS.get(),
                    "Thousand Years malice wither levels");
            LongTravailData.setWitness(diary, TravailAspect.UNDERWORLD, true);
            diaryBefore = diary.copy();
            // 女仆游泳分支读取配花属性。
            var swimmer = maid(level, p, 30);
            swimmer.setNoAi(false);
            for (int x = 28; x <= 33; x++)
                for (int z = -2; z <= 2; z++)
                    for (int y = 119; y <= 123; y++)
                        level.setBlock(
                                new net.minecraft.core.BlockPos(x, y, z),
                                Blocks.WATER.defaultBlockState(),
                                3);
            swimmer.setPos(30, 120, 0);
            swimmer.baseTick();
            swimmer.getSwimManager().setWantToSwim(true);
            check(
                    swimmer.isInWater() && swimmer.getSwimManager().wantToSwim(),
                    "real maid active swimming branch prepared");
            swimmer.setDeltaMovement(Vec3.ZERO);
            swimmer.travel(new Vec3(1, 0, 0));
            double swimBase = swimmer.getDeltaMovement().horizontalDistance();
            swimmer.setPos(30, 120, 0);
            flower(swimmer, Flower.BLUE_ORCHID);
            MaidAccessoryRuntime.tick(swimmer);
            swimmer.setDeltaMovement(Vec3.ZERO);
            swimmer.travel(new Vec3(1, 0, 0));
            near(
                    swimmer.getDeltaMovement().horizontalDistance() / swimBase,
                    1 + FlourishingItemsConfig.value(Flower.BLUE_ORCHID),
                    "blue orchid actual active swimming multiplier");
            swimmer.discard();
            // 剑与灯基础效果及灯光数据开关。
            equip(m);
            m.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100));
            equip(m, ModRegistry.SWORD_AND_LANTERN.get());
            MaidAccessoryRuntime.tick(m);
            check(!m.hasEffect(MobEffects.DARKNESS), "lantern cleans existing darkness");
            check(MaidAccessories.lantern(m), "native maid lamp active");
            com.thelongtravail.valley.LanternSources.toggle(m.getMaidBauble().getStackInSlot(0));
            check(!MaidAccessories.lantern(m), "lamp honors existing disabled NBT");
            m.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100));
            check(!m.hasEffect(MobEffects.BLINDNESS), "lantern blocks blindness");
            equip(m);
            m.setItemSlot(
                    EquipmentSlot.MAINHAND, new ItemStack(ModRegistry.SWORD_AND_LANTERN.get()));
            check(!MaidAccessories.lantern(m), "maid held lamp inactive");
            // 东海道五十三次使用女仆原生槽位，治疗与防火不依赖日记。
            equip(m, ModRegistry.TOKAIDO.get());
            RainConfig.TOKAIDO_LOCAL.set(false);
            RainConfig.TOKAIDO_SKY.set(false);
            level.setWeatherParameters(0, 6000, true, true);
            ownerOnline(level, p, false);
            check(
                    MaidAccessories.immune(m, m.damageSources().inFire(), 5),
                    "Tokaido base fire immunity without diary");
            fixtureHealth(m, 2);
            for (int i = 0; i < RainConfig.ticks(RainConfig.HEAL_SECONDS.get()); i++)
                MaidAccessoryRuntime.tick(m);
            check(m.getHealth() > 2, "Tokaido heals maid");
            ownerOnline(level, p, true);
            // 星之声使用必定触发概率；阻止的伤害不触发，实际吸收值损失触发。
            equip(m, ModRegistry.STAR_VOICE.get());
            equip(other, ModRegistry.STAR_VOICE.get());
            BoundlessItemsConfig.NUMBERS.get("star.witnessChance").set(1.0);
            BoundlessItemsConfig.NUMBERS.get("star.radius").set(8.0);
            var identity = MaidAccessoryRuntime.identity(m);
            var otherId = MaidAccessoryRuntime.identity(other);
            check(!identity.equals(otherId), "independent stable companion IDs");
            var invulnerable = cow(level);
            invulnerable.setInvulnerable(true);
            invulnerable.hurt(m.damageSources().mobAttack(m), 1);
            check(!TimeStopManager.locked(identity), "rejected hit does not trigger star");
            victim.setInvulnerable(false);
            victim.invulnerableTime = 0;
            victim.setAbsorptionAmount(10);
            victim.hurt(m.damageSources().mobAttack(m), 1);
            check(TimeStopManager.locked(identity), "absorption damage triggers maid star");
            check(
                    !TimeStopManager.frozen(m)
                            && !TimeStopManager.frozen(other)
                            && !TimeStopManager.frozen(p),
                    "caster owner and sibling maid exempt");
            check(TimeStopManager.frozen(victim), "hostile target frozen in maid field");
            check(!TimeStopManager.locked(otherId), "sibling has independent trigger lock");
            var ledger = MaidAccessoryLedger.get(server);
            int cooldown = ledger.entry(identity).getInt("StarCooldown");
            MaidAccessoryRuntime.tick(m);
            near(
                    ledger.entry(identity).getInt("StarCooldown"),
                    cooldown,
                    "star cooldown paused while group locked");
            // 胶片重建走女仆实际入口，生成新实体 UUID 并清空饰品。
            UUID entityId = m.getUUID();
            var film = ItemFilm.maidToFilm(m);
            m.discard();
            ItemFilm.filmToMaid(film, level, new net.minecraft.core.BlockPos(10, 120, 0), p);
            EntityMaid restored = null;
            for (var entity : level.getAllEntities())
                if (entity instanceof EntityMaid candidate
                        && candidate != other
                        && MaidAccessoryRuntime.identity(candidate).equals(identity))
                    restored = candidate;
            check(
                    restored != null,
                    "real film reconstruction retains stable compatibility identity");
            check(!restored.getUUID().equals(entityId), "film keeps native new entity UUID");
            check(
                    MaidAccessoryRuntime.equipped(restored, ModRegistry.STAR_VOICE.get()).isEmpty(),
                    "film does not duplicate removed equipment");
            equip(restored, ModRegistry.STAR_VOICE.get());
            check(
                    TimeStopManager.locked(MaidAccessoryRuntime.identity(restored)),
                    "film does not bypass active group lock");
            var saved = ledger.save(new CompoundTag());
            var reload = MaidAccessoryLedger.load(saved);
            near(
                    reload.entry(identity).getInt("StarCooldown"),
                    cooldown,
                    "saved ledger retains cooldown");
            TimeStopManager.stopping(server);
            check(!TimeStopManager.locked(identity), "group ends on stop");
            near(
                    ledger.entry(identity).getInt("StarCooldown"),
                    cooldown,
                    "ended field assigns offline cooldown to stable ID");
            MaidAccessoryRuntime.tick(restored);
            near(
                    ledger.entry(identity).getInt("StarCooldown"),
                    cooldown - 1,
                    "restored maid decrements own cooldown");
            // 死告天使实际命中后延迟处决，女仆行为不修改主人进度。
            equip(restored, ModRegistry.AZRAEL.get());
            AzraelConfig.TARGET.set(1.0);
            AzraelConfig.SELF.set(0.0);
            var doomed = cow(level);
            doomed.invulnerableTime = 0;
            doomed.hurt(restored.damageSources().mobAttack(restored), 1);
            check(doomed.isAlive(), "Azrael execution deferred until tick end");
            new MaidAccessoryRuntime()
                    .serverTick(
                            new TickEvent.ServerTickEvent(TickEvent.Phase.END, () -> true, server));
            check(!doomed.isAlive(), "maid Azrael executes at tick end");
            check(ItemStack.matches(diary, diaryBefore), "maid effects never mutate owner diary");
            LongTravailData.setWitness(diary, TravailAspect.DEEP_VALLEY, false);
            AzraelConfig.MALICE.set(1.0);
            restored.setHealth(restored.getMaxHealth());
            restored.invulnerableTime = 0;
            var aggressor = cow(level);
            restored.hurt(restored.damageSources().mobAttack(aggressor), 1);
            MaidAccessoryRuntime.equipmentChanged(restored, new ItemStack(ModRegistry.EKI.get()));
            var first = cow(level);
            var second = cow(level);
            first.hurt(restored.damageSources().mobAttack(restored), 1);
            second.hurt(restored.damageSources().mobAttack(restored), 1);
            new MaidAccessoryRuntime()
                    .serverTick(
                            new TickEvent.ServerTickEvent(TickEvent.Phase.END, () -> true, server));
            check(
                    !first.isAlive() && second.isAlive(),
                    "unrelated accessory change preserves Azrael opportunity; only first actual hit consumes it first="
                            + first.getHealth()
                            + " second="
                            + second.getHealth());
            restored.invulnerableTime = 0;
            restored.hurt(restored.damageSources().mobAttack(aggressor), 1);
            var stale = cow(level);
            stale.hurt(restored.damageSources().mobAttack(restored), 1);
            equip(restored);
            new MaidAccessoryRuntime()
                    .serverTick(
                            new TickEvent.ServerTickEvent(TickEvent.Phase.END, () -> true, server));
            check(stale.isAlive(), "queued Azrael execution invalidated by unequip");
            equip(restored, ModRegistry.STAR_VOICE.get());
            LongTravailData.setWitness(diary, TravailAspect.BOUNDLESS, false);
            BoundlessItemsConfig.NUMBERS.get("star.maliceChance").set(1.0);
            restored.invulnerableTime = 0;
            restored.hurt(restored.damageSources().mobAttack(aggressor), 1);
            check(TimeStopManager.locked(identity), "star malice actual received damage triggers");
            TimeStopManager.stopping(server);
            var nativeSync = other.getMaidBauble().getSyncClientBauble(other);
            check(
                    nativeSync.size() == 1,
                    "registered non-lantern bauble participates in native initial sync");
            equip(other, ModRegistry.SWORD_AND_LANTERN.get());
            check(
                    other.getMaidBauble().getSyncClientBauble(other).size() == 1,
                    "lantern uses existing maid bauble sync");
            if (net.minecraftforge.fml.ModList.get().isLoaded("touhou_little_maid_personality"))
                socialChecks(level);
            receivedChecks(level);
            calculationPressure(level);
            periodicPressure(level);
            lifecyclePressure(level);
            ownerOnline(level, p, false);
            pass = true;
        } catch (Throwable failure) {
            failure.printStackTrace();
            results.add("FAIL " + failure);
        } finally {
            try {
                java.nio.file.Files.writeString(
                        java.nio.file.Path.of("maid-compat-result.txt"), pass ? "PASS" : "FAIL");
                java.nio.file.Files.writeString(
                        java.nio.file.Path.of("maid-compat-checks.txt"),
                        String.join("\n", results) + "\nCHECKS=" + checks);
            } catch (Exception failure) {
                throw new RuntimeException(failure);
            }
            server.halt(false);
        }
    }
}
