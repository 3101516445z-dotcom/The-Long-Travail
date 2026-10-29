package travail.smoke;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** 仅由隔离测试模组注册。 */
final class TremorFaultBlocks {
    static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, "travail_smoke");
    static final RegistryObject<Block> REFUSING =
            BLOCKS.register(
                    "refusing",
                    () ->
                            new Block(Block.Properties.of().strength(1)) {
                                @Override
                                public boolean onDestroyedByPlayer(
                                        BlockState state,
                                        Level level,
                                        BlockPos pos,
                                        Player player,
                                        boolean harvest,
                                        FluidState fluid) {
                                    return false;
                                }
                            });
    static final RegistryObject<Block> REPLACING =
            BLOCKS.register(
                    "replacing",
                    () ->
                            new Block(Block.Properties.of().strength(1)) {
                                @Override
                                public boolean onDestroyedByPlayer(
                                        BlockState state,
                                        Level level,
                                        BlockPos pos,
                                        Player player,
                                        boolean harvest,
                                        FluidState fluid) {
                                    level.setBlock(pos, Blocks.DIRT.defaultBlockState(), 3);
                                    return false;
                                }
                            });
    static final RegistryObject<Block> NEIGHBOR =
            BLOCKS.register(
                    "neighbor",
                    () ->
                            new Block(Block.Properties.of().strength(1)) {
                                @Override
                                public void onRemove(
                                        BlockState state,
                                        Level level,
                                        BlockPos pos,
                                        BlockState replacement,
                                        boolean moving) {
                                    super.onRemove(state, level, pos, replacement, moving);
                                    if (replacement.isAir()
                                            && level.getBlockState(pos.east()).is(Blocks.DIRT))
                                        level.destroyBlock(pos.east(), true);
                                }
                            });
    static Runnable afterRemoval;
    static final RegistryObject<Block> ACTOR_MUTATION =
            BLOCKS.register(
                    "actor_mutation",
                    () ->
                            new Block(Block.Properties.of().strength(1)) {
                                @Override
                                public void onRemove(
                                        BlockState state,
                                        Level level,
                                        BlockPos pos,
                                        BlockState replacement,
                                        boolean moving) {
                                    super.onRemove(state, level, pos, replacement, moving);
                                    if (replacement.isAir() && afterRemoval != null) {
                                        var action = afterRemoval;
                                        afterRemoval = null;
                                        action.run();
                                    }
                                }

                                @Override
                                public java.util.List<net.minecraft.world.item.ItemStack> getDrops(
                                        BlockState state,
                                        net.minecraft.world.level.storage.loot.LootParams.Builder
                                                builder) {
                                    return java.util.List.of(
                                            new net.minecraft.world.item.ItemStack(
                                                    net.minecraft.world.item.Items.COBBLESTONE));
                                }
                            });
    static int observedFortune;
    static int extremeFortune;
    static int negativeFortune;
    static float observedLuck;
    static final RegistryObject<Block> LOOT_PROBE =
            BLOCKS.register(
                    "loot_probe",
                    () ->
                            new Block(Block.Properties.of().strength(1)) {
                                @Override
                                public java.util.List<net.minecraft.world.item.ItemStack> getDrops(
                                        BlockState state,
                                        net.minecraft.world.level.storage.loot.LootParams.Builder
                                                builder) {
                                    var tool =
                                            builder.getParameter(
                                                    net.minecraft.world.level.storage.loot
                                                            .parameters.LootContextParams.TOOL);
                                    var params =
                                            builder.withParameter(
                                                            net.minecraft.world.level.storage.loot
                                                                    .parameters.LootContextParams
                                                                    .BLOCK_STATE,
                                                            state)
                                                    .create(
                                                            net.minecraft.world.level.storage.loot
                                                                    .parameters.LootContextParamSets
                                                                    .BLOCK);
                                    observedLuck = params.getLuck();
                                    var context =
                                            new net.minecraft.world.level.storage.loot.LootContext
                                                            .Builder(params)
                                                    .create(null);
                                    extremeFortune =
                                            com.thelongtravail.valley.PlayerBlockBreaker
                                                    .fortuneForLoot(Integer.MAX_VALUE, context);
                                    negativeFortune =
                                            com.thelongtravail.valley.PlayerBlockBreaker
                                                    .fortuneForLoot(-1, context);
                                    // 将概率设为一，使实际二项式战利品公式固定返回 1 + 有效时运等级。
                                    var result =
                                            net.minecraft.world.level.storage.loot.functions
                                                    .ApplyBonusCount
                                                    .addBonusBinomialDistributionCount(
                                                            net.minecraft.world.item.enchantment
                                                                    .Enchantments.BLOCK_FORTUNE,
                                                            1F,
                                                            0)
                                                    .build()
                                                    .apply(
                                                            new net.minecraft.world.item.ItemStack(
                                                                    net.minecraft.world.item.Items
                                                                            .DIAMOND),
                                                            context);
                                    observedFortune = result.getCount() - 1;
                                    return java.util.List.of(
                                            new net.minecraft.world.item.ItemStack(
                                                    net.minecraft.world.item.Items.DIAMOND,
                                                    observedFortune + 1));
                                }
                            });
}
