package com.thelongtravail.flourishing;

import com.thelongtravail.config.FlourishingItemsConfig;
import com.thelongtravail.registry.ModRegistry;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import top.theillusivec4.curios.api.CuriosApi;

import java.util.*;

public final class Affection {
    public static final String TARGET = "BoundEntity", OWNER = "BoundOwner";

    private record Check(
            UUID target, LivingEntity entity, ItemStack stack, long tick, boolean valid) {}

    private static final Map<ServerPlayer, Check> CHECKS = new WeakHashMap<>();
    private static final Map<UUID, UUID> ACTIVE = new HashMap<>();

    public static UUID owner(Entity e) {
        if (e instanceof Player) return e.getUUID();
        if (!(e instanceof OwnableEntity)) return null;
        Set<UUID> visited = new HashSet<>();
        for (int i = 0; i < 16 && e != null && visited.add(e.getUUID()); i++) {
            if (e instanceof Player) return e.getUUID();
            if (!(e instanceof OwnableEntity owned)) return null;
            Entity next = owned.getOwner();
            if (next == null) return owned.getOwnerUUID();
            e = next;
        }
        return null;
    }

    public static ServerPlayer attacker(Entity e) {
        if (e instanceof ServerPlayer p) return p;
        if (!(e instanceof net.minecraft.world.entity.projectile.Projectile)
                && !(e instanceof OwnableEntity)) return null;
        Set<UUID> visited = new HashSet<>();
        for (int i = 0; i < 16 && e != null && visited.add(e.getUUID()); i++) {
            if (e instanceof ServerPlayer p) return p;
            if (e instanceof net.minecraft.world.entity.projectile.Projectile projectile)
                e = projectile.getOwner();
            else if (e instanceof OwnableEntity owned) e = owned.getOwner();
            else return null;
        }
        return null;
    }

    public static ItemStack equipped(Player p, boolean spring) {
        var reference = spring ? ModRegistry.SPRING_CORNER : ModRegistry.AFFECTION;
        if (!reference.isPresent()) return ItemStack.EMPTY;
        var item = reference.get();
        var slots =
                spring
                        ? FlourishingItemsConfig.SPRING_SLOTS.get()
                        : FlourishingItemsConfig.AFFECTION_SLOTS.get();
        return CuriosApi.getCuriosInventory(p)
                .map(
                        inv -> {
                            for (var result : inv.findCurios(item)) {
                                var context = result.slotContext();
                                if (!context.cosmetic()
                                        && slots.contains(context.identifier())
                                        && (spring || ownerAllowed(result.stack(), p)))
                                    return result.stack();
                            }
                            return ItemStack.EMPTY;
                        })
                .orElse(ItemStack.EMPTY);
    }

    public static boolean ownerAllowed(ItemStack s, Player p) {
        return !s.hasTag()
                || !s.getTag().hasUUID(OWNER)
                || s.getTag().getUUID(OWNER).equals(p.getUUID());
    }

    public static void clear(ItemStack s) {
        if (s.hasTag()) {
            s.getTag().remove(TARGET);
            s.getTag().remove(OWNER);
            s.getTag().remove("BoundName");
        }
    }

    public static InteractionResult bind(ItemStack s, Player p, LivingEntity target) {
        if (p.level().isClientSide) return InteractionResult.SUCCESS;
        if (s.hasTag() && s.getTag().hasUUID(TARGET)) {
            if (s.getTag().getUUID(TARGET).equals(target.getUUID())) {
                clear(s);
                p.displayClientMessage(
                        com.thelongtravail.text.ActionMessages.text(
                                "message.the_long_travail.affection.cleared"),
                        true);
            } else
                p.displayClientMessage(
                        com.thelongtravail.text.ActionMessages.text(
                                "message.the_long_travail.affection.faithful"),
                        true);
        } else if (target.isAlive()
                && !(target instanceof Player)
                && p.getUUID().equals(owner(target))
                && bindingTypeAllowed(target)) {
            s.getOrCreateTag().putUUID(TARGET, target.getUUID());
            s.getOrCreateTag().putUUID(OWNER, p.getUUID());
            s.getOrCreateTag().putString("BoundName", target.getName().getString());
            AffectionDeaths.get(p.getServer()).watch(target.getUUID());
            p.displayClientMessage(
                    com.thelongtravail.text.ActionMessages.text(
                            "message.the_long_travail.affection.bound", target.getName()),
                    true);
        } else
            p.displayClientMessage(
                    com.thelongtravail.text.ActionMessages.text(
                            "message.the_long_travail.affection.invalid"),
                    true);
        if (p instanceof ServerPlayer server) {
            reset(server);
            target(server);
        }
        return InteractionResult.CONSUME;
    }

    private static boolean bindingTypeAllowed(LivingEntity target) {
        var typeId =
                ForgeRegistries.ENTITY_TYPES
                        .getResourceKey(target.getType())
                        .map(net.minecraft.resources.ResourceKey::location)
                        .orElse(null);
        return typeId != null
                && !FlourishingItemsConfig.BLACKLIST.get().contains(typeId.toString());
    }

    public static LivingEntity target(ServerPlayer p) {
        ItemStack s = equipped(p, false);
        if (!s.isEmpty()) AffectionDeaths.get(p.server).clean(s);
        if (!p.isAlive()
                || p.isSpectator()
                || s.isEmpty()
                || !s.hasTag()
                || !s.getTag().hasUUID(TARGET)) {
            reset(p);
            return null;
        }
        UUID id = s.getTag().getUUID(TARGET);
        Entity e = p.serverLevel().getEntity(id);
        if (!(e instanceof LivingEntity living)) {
            reset(p);
            return null;
        }
        if (!living.isAlive()) {
            reset(p);
            return null;
        }
        long now = p.level().getGameTime();
        Check c = CHECKS.get(p);
        if (c == null
                || c.entity != living
                || c.stack != s
                || !c.target.equals(id)
                || now - c.tick >= 20) {
            reset(p);
            c =
                    new Check(
                            id,
                            living,
                            s,
                            now,
                            p.getUUID().equals(owner(living))
                                    && !FlourishingItemsConfig.BLACKLIST
                                            .get()
                                            .contains(
                                                    ForgeRegistries.ENTITY_TYPES
                                                            .getKey(living.getType())
                                                            .toString()));
            CHECKS.put(p, c);
        }
        double range = FlourishingItemsConfig.get("affection.range");
        if (c.valid && p.distanceToSqr(living) <= range * range) {
            ACTIVE.put(id, p.getUUID());
            return living;
        }
        ACTIVE.remove(id, p.getUUID());
        return null;
    }

    public static ServerPlayer master(LivingEntity e) {
        UUID id = ACTIVE.get(e.getUUID());
        if (id == null || e.level().getServer() == null) return null;
        ServerPlayer p = e.level().getServer().getPlayerList().getPlayer(id);
        if (p == null) {
            ACTIVE.remove(e.getUUID(), id);
            return null;
        }
        return target(p) == e ? p : null;
    }

    public static double lost(LivingEntity e) {
        return Math.max(0, Math.min(1, 1 - e.getHealth() / Math.max(.001, e.getMaxHealth())));
    }

    public static void reset(ServerPlayer p) {
        Check old = CHECKS.remove(p);
        if (old != null) ACTIVE.remove(old.target, p.getUUID());
    }

    // 仅释放运行时实体引用；卸载和跨维度不等同于死亡，不能清除物品绑定。
    public static void forget(Entity entity) {
        if (entity instanceof ServerPlayer player) reset(player);
        var iterator = CHECKS.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (entry.getValue().entity == entity) {
                ACTIVE.remove(entry.getValue().target, entry.getKey().getUUID());
                iterator.remove();
            }
        }
    }

    public static void resetAll() {
        CHECKS.clear();
        ACTIVE.clear();
    }

    private Affection() {}
}
