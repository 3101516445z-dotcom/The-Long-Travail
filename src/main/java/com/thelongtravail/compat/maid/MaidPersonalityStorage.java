package com.thelongtravail.compat.maid;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;

// 只排除苦旅牡丹的动态生命加成，其他修饰符仍由性格模组管理。
public final class MaidPersonalityStorage {
    public static final String MARKER = "TravailDynamicHealthExcluded";
    public static final String BASE = "TravailIndependentHealthTarget";
    private static final RangedAttribute CALCULATION =
            new RangedAttribute(
                    "travail.unbounded_health_calculation", 1, -Double.MAX_VALUE, Double.MAX_VALUE);
    private static final UUID PEONY = UUID.fromString("193ab8d0-46cc-4179-bb32-f0317ed65401");
    private static final UUID FLOOR =
            UUID.nameUUIDFromBytes(
                    "touhou_little_maid_personality.store_freeze_health"
                            .getBytes(StandardCharsets.UTF_8));

    private MaidPersonalityStorage() {}

    public static void snapshot(LivingEntity maid, CompoundTag data) {
        if (maid == null || data == null) return;
        var original = maid.getAttribute(Attributes.MAX_HEALTH);
        if (original == null) return;
        boolean peony = original.getModifier(PEONY) != null;
        // 按原版运算顺序重算独立生命上限，不修改实体属性。
        var independent = new AttributeInstance(CALCULATION, ignored -> {});
        independent.setBaseValue(original.getBaseValue());
        for (var modifier : original.getModifiers())
            if (!modifier.getId().equals(PEONY) && !modifier.getId().equals(FLOOR))
                independent.addTransientModifier(modifier);
        var forge = data.getCompound("ForgeData");
        // stamp 已生成两份快照；两份都须校正，确保解冻与直接恢复的结果一致。
        for (String key : new String[] {"PersonalityStoreFreeze", "TlmpStoreHealthFix"}) {
            if (!forge.contains(key, Tag.TAG_COMPOUND)) continue;
            var snapshot = forge.getCompound(key);
            var modifiers = snapshot.getList("HealthModifiers", Tag.TAG_COMPOUND);
            for (int i = modifiers.size() - 1; i >= 0; i--) {
                var entry = modifiers.getCompound(i);
                if (entry.hasUUID("UUID") && PEONY.equals(entry.getUUID("UUID"))
                        || entry.hasUUID("Id") && PEONY.equals(entry.getUUID("Id")))
                    modifiers.remove(i);
            }
            snapshot.putDouble(BASE, independent.getValue());
            if (peony) {
                snapshot.putDouble(
                        "WantMax",
                        (float) Attributes.MAX_HEALTH.sanitizeValue(independent.getValue()));
                snapshot.putBoolean(MARKER, true);
            }
        }
    }

    private static double target(LivingEntity maid, CompoundTag tag, String fallback) {
        if (!tag.contains(BASE, Tag.TAG_ANY_NUMERIC)) tag.putDouble(BASE, tag.getDouble(fallback));
        var health = maid.getAttribute(Attributes.MAX_HEALTH);
        var peony = health == null ? null : health.getModifier(PEONY);
        double amount = tag.getDouble(BASE) * (peony == null ? 1 : 1 + peony.getAmount());
        // 与实体生命上限保持相同的 float 精度，避免精度差异误触发保底修正或持续重试。
        return (float) Attributes.MAX_HEALTH.sanitizeValue(amount);
    }

    private static boolean activate(LivingEntity maid, CompoundTag tag) {
        if (tag.getBoolean(MARKER)) return true;
        var health = maid.getAttribute(Attributes.MAX_HEALTH);
        if (!tag.contains(BASE, Tag.TAG_ANY_NUMERIC)
                || health == null
                || health.getModifier(PEONY) == null) return false;
        tag.putBoolean(MARKER, true);
        return true;
    }

    public static void capHealth(LivingEntity maid, CompoundTag request) {
        if (!activate(maid, request)) return;
        // 单独保存未叠加牡丹的目标值，用于恢复其他模组暂时缺失的加成。
        // 若混入牡丹加成，卸下饰品后可能再次恢复已经移除的加成。
        request.putDouble("WantMax", target(maid, request, "WantMax"));
        double maximum = Math.max(maid.getMaxHealth(), request.getDouble("WantMax"));
        request.putDouble("WantHealth", Math.min(request.getDouble("WantHealth"), maximum));
    }

    public static void rememberFloor(LivingEntity maid, CompoundTag request) {
        if (!request.contains(BASE, Tag.TAG_ANY_NUMERIC)) return;
        var data = maid.getPersistentData();
        if (!data.contains("TlmpStoreHealthFloor", Tag.TAG_COMPOUND)) return;
        var floor = data.getCompound("TlmpStoreHealthFloor");
        floor.putDouble(BASE, request.getDouble(BASE));
        if (request.getBoolean(MARKER)) floor.putBoolean(MARKER, true);
    }

    public static void refreshFloor(LivingEntity maid, CompoundTag data) {
        if (!data.contains("TlmpStoreHealthFloor", Tag.TAG_COMPOUND)) return;
        var floor = data.getCompound("TlmpStoreHealthFloor");
        if (activate(maid, floor)) floor.putDouble("Want", target(maid, floor, "Want"));
    }
}
