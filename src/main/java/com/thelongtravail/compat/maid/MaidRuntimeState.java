package com.thelongtravail.compat.maid;

import static com.thelongtravail.compat.maid.MaidAccess.identity;
import static com.thelongtravail.compat.maid.MaidAccessoryRules.has;
import static com.thelongtravail.compat.maid.MaidAccessoryRules.phase;

import com.github.tartaricacid.touhoulittlemaid.api.event.MaidAndItemTransformEvent;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.thelongtravail.TravailAspect;
import com.thelongtravail.compat.maid.MaidAccessoryRuntime.Phase;
import com.thelongtravail.registry.ModRegistry;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;

// 管理女仆临时状态、延后任务、身份恢复及生命周期清理。
final class MaidRuntimeState {
    private static final String ID = MaidAccess.IDENTITY_KEY;
    private static final Map<EntityMaid, State> STATES = new WeakHashMap<>();
    private static final Map<LivingEntity, Map<String, Long>> EFFECT_TIMES = new WeakHashMap<>();
    private static final ArrayDeque<Runnable> PENDING = new ArrayDeque<>();

    static final class State {
        int heal, rainHeal;
        long rose = Long.MIN_VALUE, kill = Long.MIN_VALUE;
        boolean armed;
        Phase azrael = Phase.INACTIVE;
        UUID owner;
        long generation;
    }

    public static void watch(EntityMaid m) {
        MaidBaubleSync.watch(m);
        if (!m.level().isClientSide) STATES.computeIfAbsent(m, k -> new State());
    }

    public static void equipmentChanged(EntityMaid m) {
        equipmentChanged(m, new ItemStack(ModRegistry.DON_QUIXOTE.get()));
        equipmentChanged(m, new ItemStack(ModRegistry.AZRAEL.get()));
        equipmentChanged(m, new ItemStack(ModRegistry.SWORD_AND_LANTERN.get()));
    }

    public static void equipmentChanged(EntityMaid m, ItemStack changed) {
        watch(m);
        if (changed.is(ModRegistry.DON_QUIXOTE.get()))
            com.thelongtravail.farreach.DonQuixoteState.forget(m);
        var s = STATES.get(m);
        if (s != null && changed.is(ModRegistry.AZRAEL.get())) {
            s.armed = false;
            s.generation++;
        }
        if (changed.is(ModRegistry.SWORD_AND_LANTERN.get())) MaidLanternCombat.forget(m);
    }

    public static int quixoteCooldown(LivingEntity e, int ticks) {
        if (!(e instanceof EntityMaid m)) return 0;
        var store = MaidAccessoryLedger.get(m.getServer());
        var data =
                ticks < 0
                        ? (m.getPersistentData().hasUUID(ID)
                                ? store.find(m.getPersistentData().getUUID(ID))
                                : null)
                        : store.entry(identity(m));
        if (data == null) return 0;
        if (ticks >= 0) {
            data.putInt("QuixoteCooldown", ticks);
            store.setDirty();
        }
        return data.getInt("QuixoteCooldown");
    }

    static CompoundTag ledger(EntityMaid m) {
        return MaidAccessoryLedger.get(m.getServer()).entry(identity(m));
    }

    public static void starCooldown(LivingEntity e, int ticks) {
        var m = (EntityMaid) e;
        var tag = ledger(m);
        tag.putInt("StarCooldown", Math.max(ticks, tag.getInt("StarCooldown")));
        MaidAccessoryLedger.get(m.getServer()).setDirty();
    }

    static State state(EntityMaid m) {
        watch(m);
        var s = STATES.get(m);
        var phase =
                has(m, ModRegistry.AZRAEL.get())
                        ? phase(m, TravailAspect.DEEP_VALLEY)
                        : Phase.INACTIVE;
        boolean changedOwner = !Objects.equals(s.owner, m.getOwnerUUID());
        if (phase != s.azrael || changedOwner) {
            s.armed = false;
            s.azrael = phase;
            s.owner = m.getOwnerUUID();
            s.generation++;
        }
        if (changedOwner) MaidLanternCombat.forget(m);
        return s;
    }

    public static void leave(EntityLeaveLevelEvent e) {
        if (!e.getLevel().isClientSide && e.getEntity() instanceof EntityMaid m) {
            forget(m);
        }
    }

    public static void transformOut(MaidAndItemTransformEvent.ToItem e) {
        if (!e.getMaid().level().isClientSide) e.getData().putUUID(ID, identity(e.getMaid()));
    }

    public static void transformIn(MaidAndItemTransformEvent.ToMaid e) {
        if (e.getMaid().level().isClientSide || !e.getData().hasUUID(ID)) return;
        var id = e.getData().getUUID(ID);
        e.getMaid().getPersistentData().putUUID(ID, id);
        var forge = e.getData().getCompound("ForgeData");
        forge.putUUID(ID, id);
        e.getData().put("ForgeData", forge);
    }

    public static void stopped(net.minecraftforge.event.server.ServerStoppedEvent e) {
        STATES.clear();
        EFFECT_TIMES.clear();
        PENDING.clear();
        MaidLanternCombat.clear();
    }

    static void defer(Runnable action) {
        PENDING.add(action);
    }

    static Map<String, Long> effectTimes(LivingEntity victim) {
        return EFFECT_TIMES.computeIfAbsent(victim, k -> new HashMap<>());
    }

    static void forget(EntityMaid m) {
        STATES.remove(m);
        com.thelongtravail.farreach.DonQuixoteState.forget(m);
        MaidLanternCombat.forget(m);
    }

    static List<EntityMaid> tracked() {
        return List.copyOf(STATES.keySet());
    }

    static void drainPending() {
        int count = PENDING.size();
        while (count-- > 0 && !PENDING.isEmpty()) PENDING.removeFirst().run();
    }

    private MaidRuntimeState() {}
}
