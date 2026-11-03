package travail.underworldscope;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.thelongtravail.compat.maid.MaidAccessoryRuntime;
import com.thelongtravail.registry.ModRegistry;
import java.util.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

// 使用真实女仆及原生饰品栏；仅在线主人索引使用现有隔离夹具的注册方式。
final class MaidAttackScopeProbe {
    @SuppressWarnings("unchecked")
    static void run(
            MinecraftServer server, ServerPlayer owner, ItemStack weapon, List<String> lines)
            throws Exception {
        var field = net.minecraft.server.players.PlayerList.class.getDeclaredField("playersByUUID");
        field.setAccessible(true);
        var online = (Map<UUID, ServerPlayer>) field.get(server.getPlayerList());
        var previous = online.put(owner.getUUID(), owner);
        var maid =
                (EntityMaid)
                        ForgeRegistries.ENTITY_TYPES
                                .getValue(new ResourceLocation("touhou_little_maid", "maid"))
                                .create(server.overworld());
        try {
            maid.setTame(true);
            maid.setOwnerUUID(owner.getUUID());
            maid.setNoAi(true);
            maid.setPos(owner.getX() + 1, owner.getY(), owner.getZ());
            server.overworld().getChunkAt(maid.blockPosition());
            SuccessfulAttackProbe.check(server.overworld().addFreshEntity(maid), "spawn real maid");
            maid.getMaidBauble().clearAll();
            maid.getMaidBauble().setStackInSlot(0, new ItemStack(ModRegistry.THOUSAND_YEARS.get()));
            MaidAccessoryRuntime.equipmentChanged(maid);
            SuccessfulAttackProbe.verify(owner, maid, weapon, true, 100, "qualified maid", lines);
            maid.getMaidBauble().clearAll();
            MaidAccessoryRuntime.equipmentChanged(maid);
            SuccessfulAttackProbe.verify(owner, maid, weapon, false, 100, "unequipped maid", lines);
        } finally {
            maid.discard();
            if (previous == null) online.remove(owner.getUUID());
            else online.put(owner.getUUID(), previous);
        }
    }
}
