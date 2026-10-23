package com.thelongtravail.mixin;

import com.thelongtravail.valley.ExecutionStorage;
import net.minecraft.world.entity.Entity;
import net.minecraft.util.ClassInstanceMultiMap;
import java.util.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

@Mixin(ClassInstanceMultiMap.class)
public abstract class ExecutionMultiMapMixin<T> implements ExecutionStorage {
    @Shadow @Final private Map<Class<?>, List<T>> byClass;
    @Shadow @Final @Mutable private List<T> allInstances;
    @Unique private boolean travail$expectsFastList;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void travail$rememberListType(CallbackInfo ci) {
        travail$expectsFastList = allInstances instanceof ObjectArrayList;
    }

    @Unique
    private void travail$normalizeList() {
        if (travail$expectsFastList && !(allInstances instanceof ObjectArrayList)) {
            var previous = allInstances;
            allInstances = new ObjectArrayList<>(previous);
            byClass.replaceAll((type, list) -> list == previous ? allInstances : list);
        }
    }

    @Inject(method = "find", at = @At("HEAD"))
    private <S> void travail$beforeFind(Class<S> type, CallbackInfoReturnable<Collection<S>> ci) {
        travail$normalizeList();
    }

    public void travail$erase(Entity e) {
        travail$normalizeList();
        // 保留查询迭代器正在使用的旧列表，避免遍历中途失效。
        var replacements = new IdentityHashMap<List<T>, List<T>>();
        var old = allInstances;
        allInstances = travail$without(old, e);
        replacements.put(old, allInstances);
        for (var entry : byClass.entrySet()) {
            var list = entry.getValue();
            var replacement = replacements.get(list);
            if (replacement == null) {
                replacement = travail$without(list, e);
                replacements.put(list, replacement);
            }
            if (list != replacement) entry.setValue(replacement);
        }
    }

    @Unique
    private List<T> travail$without(List<T> list, Entity entity) {
        if (!travail$has(list, entity)) return list;
        List<T> copy =
                list instanceof ObjectArrayList
                        ? new ObjectArrayList<>(list)
                        : new ArrayList<>(list);
        copy.removeIf(value -> value == entity);
        return copy;
    }

    @Unique
    private boolean travail$has(List<T> list, Entity entity) {
        for (var value : list) if (value == entity) return true;
        return false;
    }

    public Set<Entity> travail$present(Set<Entity> targets) {
        Set<Entity> found = Collections.newSetFromMap(new IdentityHashMap<>());
        Set<List<T>> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        visited.add(allInstances);
        visited.addAll(byClass.values());
        for (var list : visited)
            for (var value : list)
                if (value instanceof Entity entity && targets.contains(entity)) found.add(entity);
        return found;
    }

    public boolean travail$contains(Entity e) {
        if (travail$has(allInstances, e)) return true;
        for (var list : byClass.values())
            if (list != allInstances && travail$has(list, e)) return true;
        return false;
    }
}
