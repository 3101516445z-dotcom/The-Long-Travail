package com.thelongtravail.mixin;

import com.thelongtravail.valley.ExecutionStorage;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityTickList;
import it.unimi.dsi.fastutil.ints.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityTickList.class)
public abstract class ExecutionTickListMixin implements ExecutionStorage {
    @Shadow private Int2ObjectMap<Entity> active;
    @Shadow private Int2ObjectMap<Entity> passive;
    @Shadow private Int2ObjectMap<Entity> iterated;

    // 其他模组可能换用无序映射；在原版优化方法假定映射有序之前，先保留运行中的快照。
    @Inject(method = "ensureActiveIsNotIterated", at = @At("HEAD"), cancellable = true)
    private void travail$protectForeignMap(CallbackInfo ci) {
        if (active == iterated && !(active instanceof Int2ObjectLinkedOpenHashMap)) {
            passive = active;
            active = new Int2ObjectLinkedOpenHashMap<>(active);
            ci.cancel();
        }
    }

    public void travail$erase(Entity e) {
        // 不修改正在遍历的映射；旧快照中的实体由服务端游戏刻检查拦截。
        if (active == iterated && active.get(e.getId()) == e) active = new Int2ObjectLinkedOpenHashMap<>(active);
        if (active.get(e.getId()) == e) active.remove(e.getId());
        if (passive != iterated && passive.get(e.getId()) == e) passive.remove(e.getId());
    }

    public boolean travail$contains(Entity e) {
        return active.get(e.getId()) == e;
    }
}
