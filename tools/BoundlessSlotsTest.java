import com.thelongtravail.helper.TravailCurios;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import java.lang.reflect.Proxy;
import java.util.*;

public class BoundlessSlotsTest {
    static final UUID OWN = UUID.fromString("b241372a-b39e-479d-b497-f46f92ee55fa");
    static class Inventory {
        Map<UUID, AttributeModifier> modifiers = new HashMap<>();
        Set<AttributeModifier> permanent = new HashSet<>(), cached = new HashSet<>();
        int adds, removes, evacuated;
        ICurioStacksHandler handler = (ICurioStacksHandler) Proxy.newProxyInstance(
                ICurioStacksHandler.class.getClassLoader(), new Class<?>[]{ICurioStacksHandler.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getModifiers": return modifiers;
                        case "getPermanentModifiers": return permanent;
                        case "getCachedModifiers": return cached;
                        case "removeModifier": {
                            var old = modifiers.remove((UUID) args[0]);
                            permanent.remove(old); removes++; return null;
                        }
                        case "addPermanentModifier": {
                            var added = (AttributeModifier) args[0];
                            modifiers.put(added.getId(), added); permanent.add(added); adds++; return null;
                        }
                        default: throw new AssertionError("Unexpected mutation: " + method.getName());
                    }
                });
        void reconcile(int desired) {
            TravailCurios.reconcileExtraSlots(handler, desired, count -> {
                check(modifiers.containsKey(OWN), "Evacuate before removing occupied slots");
                evacuated += count;
            });
        }
    }
    static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    public static void main(String[] args) {
        Inventory inv = new Inventory();
        var other = new AttributeModifier(UUID.randomUUID(), "other mod", 2, AttributeModifier.Operation.ADDITION);
        inv.modifiers.put(other.getId(), other);
        inv.reconcile(1);
        var first = inv.modifiers.get(OWN);
        for (int i = 0; i < 1000; i++) inv.reconcile(1); // 模拟 NBT 变化后反复触发 tick 和 onEquip。
        check(inv.adds == 1 && inv.removes == 0 && inv.evacuated == 0, "Unchanged bonus must not churn or eject items");
        check(inv.modifiers.get(OWN) == first, "Keep the same modifier instance");
        inv.reconcile(3);
        check(inv.evacuated == 0 && inv.modifiers.get(OWN).getAmount() == 3, "Increasing slots must not eject items");
        inv.reconcile(1);
        check(inv.evacuated == 2, "Only evacuate the reduced capacity");
        inv.reconcile(0);
        check(inv.evacuated == 3 && !inv.modifiers.containsKey(OWN), "Actual loss removes all owned slots");
        int removals = inv.removes;
        inv.reconcile(0);
        check(inv.removes == removals && inv.evacuated == 3, "Removal is idempotent");
        check(inv.modifiers.get(other.getId()) == other, "Other mods' slots remain untouched");

        Inventory migrated = new Inventory();
        migrated.modifiers.put(OWN, first); migrated.cached.add(first);
        migrated.reconcile(1);
        check(migrated.cached.isEmpty() && migrated.permanent.contains(first), "Migrate cached transient slot in place");
        check(migrated.adds == 0 && migrated.removes == 0 && migrated.evacuated == 0, "Migration must preserve occupied slots");
        System.out.println("PASS: 1000 unchanged refreshes, growth, shrink, removal, migration, unrelated modifiers.");
    }
}
