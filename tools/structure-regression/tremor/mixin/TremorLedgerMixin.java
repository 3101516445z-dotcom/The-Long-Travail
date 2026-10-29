package travail.smoke.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.thelongtravail.valley.PlayerBlockBreaker;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.spongepowered.asm.mixin.Mixin;
import travail.smoke.TremorPressure;
import java.util.List;

@Mixin(value = PlayerBlockBreaker.class, remap = false)
public abstract class TremorLedgerMixin {
    @WrapMethod(method = "resolveDrops", remap = false)
    private static List<ItemStack> observe(
            BlockState state,
            LootParams.Builder builder,
            Operation<List<ItemStack>> lootOperation,
            Operation<List<ItemStack>> original) {
        var drops = original.call(state, builder, lootOperation);
        TremorPressure.observe(
                drops,
                builder.getLevel(),
                BlockPos.containing(builder.getParameter(LootContextParams.ORIGIN)));
        return drops;
    }
}
