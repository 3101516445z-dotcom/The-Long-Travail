package com.thelongtravail.compat.apotheosis;

import java.util.List;
import java.util.Set;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import net.minecraftforge.fml.loading.FMLLoader;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;

// 仅协调铁砧的等级限制；费用封顶和免费取出始终由苦旅原有逻辑处理。
public final class AnvilCompatPlugin implements IMixinConfigPlugin {
    @Override
    public boolean shouldApplyMixin(String target, String mixin) {
        String provider;
        if (mixin.endsWith(".AnvilLevelLimitMixin"))
            provider = "dev.shadowsoffire.apotheosis.mixin.AnvilMenuMixin";
        else if (mixin.endsWith(".AnvilScreenMixin"))
            provider = "dev.shadowsoffire.apotheosis.mixin.client.AnvilScreenMixin";
        else return true;

        // Mixin 加载早于运行期 ModList 可用；使用加载期列表，不初始化神化或 Minecraft 类。
        if (FMLLoader.getLoadingModList().getMods().stream()
                .noneMatch(mod -> mod.getModId().equals("apotheosis"))) return true;
        try {
            ClassNode node =
                    MixinService.getService().getBytecodeProvider().getClassNode(provider, true);
            if (!removesLevelLimit(node))
                throw new IllegalStateException(
                        "Unsupported Apotheosis anvil level-limit implementation: " + provider);
            org.slf4j.LoggerFactory.getLogger("the_long_travail.compat")
                    .info(
                            "Skipping {}: Apotheosis supplies the unlimited anvil level threshold",
                            mixin);
            return false;
        } catch (ClassNotFoundException | java.io.IOException failure) {
            throw new IllegalStateException(
                    "Unable to verify Apotheosis anvil compatibility: " + provider, failure);
        }
    }

    static boolean removesLevelLimit(ClassNode node) {
        for (var method : node.methods) {
            if (!method.name.equals("apoth_removeLevelCap") || !method.desc.equals("(I)I"))
                continue;
            var instructions = new java.util.ArrayList<org.objectweb.asm.tree.AbstractInsnNode>();
            for (var instruction : method.instructions)
                if (instruction.getOpcode() >= 0) instructions.add(instruction);
            return instructions.size() == 2
                    && instructions.get(0) instanceof LdcInsnNode constant
                    && Integer.valueOf(Integer.MAX_VALUE).equals(constant.cst)
                    && instructions.get(1).getOpcode() == Opcodes.IRETURN;
        }
        return false;
    }

    @Override
    public void onLoad(String mixinPackage) {}

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public void acceptTargets(Set<String> mine, Set<String> others) {}

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {}

    @Override
    public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
}
