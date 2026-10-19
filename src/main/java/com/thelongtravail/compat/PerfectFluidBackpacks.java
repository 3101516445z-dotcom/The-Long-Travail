package com.thelongtravail.compat;

import com.thelongtravail.abyss.PerfectFluidCopies;
import com.thelongtravail.abyss.PerfectFluidRecipe;
import com.thelongtravail.registry.ModRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = "the_long_travail")
public final class PerfectFluidBackpacks {
    private PerfectFluidBackpacks() {}

    private static boolean reference(CompoundTag item) {
        return item.getString("id").startsWith("sophisticatedbackpacks:")
                && item.contains("Count", Tag.TAG_ANY_NUMERIC)
                && item.getCompound("tag").hasUUID("contentsUuid");
    }

    public static boolean hasReferences(Tag node) {
        if (!ModList.get().isLoaded("sophisticatedbackpacks")) return false;
        if (node instanceof CompoundTag c) {
            if (reference(c)) return true;
            for (String key : c.getAllKeys()) if (hasReferences(c.get(key))) return true;
        } else if (node instanceof ListTag list) {
            for (Tag child : list) if (hasReferences(child)) return true;
        }
        return false;
    }

    public static void reserveIds(Tag node) {
        if (!ModList.get().isLoaded("sophisticatedbackpacks")) return;
        if (node instanceof CompoundTag c) {
            if (reference(c)) c.getCompound("tag").putUUID("contentsUuid", UUID.randomUUID());
            for (String key : c.getAllKeys()) reserveIds(c.get(key));
        } else if (node instanceof ListTag list) {
            for (Tag child : list) reserveIds(child);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void crafted(PlayerEvent.ItemCraftedEvent event) {
        if (event.getEntity().level().isClientSide
                || !ModList.get().isLoaded("sophisticatedbackpacks")
                || !(event.getInventory() instanceof CraftingContainer grid)) return;
        var level = event.getEntity().level();
        if (!(level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level).orElse(null)
                instanceof PerfectFluidRecipe)) return;
        for (var source : grid.getItems()) {
            if (source.isEmpty() || source.is(ModRegistry.PERFECT_FLUID.get())) continue;
            CompoundTag original = source.save(new CompoundTag());
            if (!hasReferences(original)) return;
            PerfectFluidCopies.clearFluid(original);
            // Shift 事件提供领取前的输出副本，其预留 UUID 与移入背包的物品相同。
            CompoundTag output = event.getCrafting().save(new CompoundTag());
            try {
                Object storage = Api.GET.invoke(null);
                var pending = new LinkedHashMap<UUID, CompoundTag>();
                copyPairs(original, output, storage, pending);
                for (var entry : pending.entrySet()) {
                    // 信息请求可能已经创建空的占位库存，因此须先移除占位，再写入完整库存。
                    Api.REMOVE.invoke(storage, entry.getKey());
                    Api.SET.invoke(storage, entry.getKey(), entry.getValue());
                }
                if (!pending.isEmpty()) ((SavedData) storage).setDirty();
            } catch (ReflectiveOperationException failure) {
                throw new IllegalStateException(
                        "Cannot copy Sophisticated Backpacks storage for Perfect Fluid", failure);
            }
            return;
        }
    }

    private static void copyPairs(Tag source, Tag output, Object storage, Map<UUID, CompoundTag> pending)
            throws ReflectiveOperationException {
        if (source instanceof CompoundTag a && output instanceof CompoundTag b) {
            if (reference(a) && reference(b)) {
                UUID oldId = a.getCompound("tag").getUUID("contentsUuid");
                UUID newId = b.getCompound("tag").getUUID("contentsUuid");
                if (oldId.equals(newId))
                    throw new IllegalStateException("Perfect Fluid output must have an independent backpack UUID");
                cloneContents(oldId, newId, storage, new HashMap<>(), pending);
            }
            for (String key : a.getAllKeys()) copyPairs(a.get(key), b.get(key), storage, pending);
        } else if (source instanceof ListTag a && output instanceof ListTag b) {
            Map<Integer, CompoundTag> bySlot = null;
            for (int i = 0; i < a.size(); i++) {
                Tag original = a.get(i);
                if (original instanceof CompoundTag item && item.contains("Slot", Tag.TAG_ANY_NUMERIC)) {
                    // Capability 反序列化后可能省略空槽，须按 Slot 字段配对，不能依赖列表顺序。
                    if (bySlot == null) {
                        bySlot = new HashMap<>();
                        for (Tag candidate : b) {
                            if (candidate instanceof CompoundTag other && other.contains("Slot", Tag.TAG_ANY_NUMERIC))
                                bySlot.putIfAbsent(other.getInt("Slot"), other);
                        }
                    }
                    CompoundTag matched = bySlot.get(item.getInt("Slot"));
                    if (matched != null) copyPairs(original, matched, storage, pending);
                } else if (i < b.size()) copyPairs(original, b.get(i), storage, pending);
            }
        }
    }

    private static void cloneContents(
            UUID oldId, UUID newId, Object storage, Map<UUID, UUID> ids, Map<UUID, CompoundTag> pending)
            throws ReflectiveOperationException {
        ids.put(oldId, newId);
        CompoundTag contents = ((CompoundTag) Api.CONTENTS.invoke(storage, oldId)).copy();
        PerfectFluidCopies.clearFluid(contents);
        pending.put(newId, contents);
        cloneNested(contents, storage, ids, pending);
    }

    private static void cloneNested(Tag node, Object storage, Map<UUID, UUID> ids, Map<UUID, CompoundTag> pending)
            throws ReflectiveOperationException {
        if (node instanceof CompoundTag c) {
            if (reference(c)) {
                CompoundTag tag = c.getCompound("tag");
                UUID oldId = tag.getUUID("contentsUuid");
                UUID newId = ids.get(oldId);
                if (newId == null) {
                    newId = UUID.randomUUID();
                    cloneContents(oldId, newId, storage, ids, pending);
                }
                tag.putUUID("contentsUuid", newId);
            }
            for (String key : c.getAllKeys()) cloneNested(c.get(key), storage, ids, pending);
        } else if (node instanceof ListTag list) {
            for (Tag child : list) cloneNested(child, storage, ids, pending);
        }
    }

    private static final class Api {
        static final Method GET;
        static final Method CONTENTS;
        static final Method SET;
        static final Method REMOVE;

        static {
            try {
                Class<?> type = Class.forName("net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackStorage");
                GET = type.getMethod("get");
                CONTENTS = type.getMethod("getOrCreateBackpackContents", UUID.class);
                SET = type.getMethod("setBackpackContents", UUID.class, CompoundTag.class);
                REMOVE = type.getMethod("removeBackpackContents", UUID.class);
            } catch (ReflectiveOperationException failure) {
                throw new ExceptionInInitializerError(failure);
            }
        }
    }
}
