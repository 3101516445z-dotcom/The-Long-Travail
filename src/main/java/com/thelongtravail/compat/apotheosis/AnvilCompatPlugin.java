package com.thelongtravail.compat.apotheosis;

import com.thelongtravail.compat.OptionalCompatChecks;
import com.thelongtravail.compat.OptionalCompatState;
import java.util.List;
import java.util.Set;
import net.minecraftforge.fml.loading.FMLLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;

public final class AnvilCompatPlugin implements IMixinConfigPlugin {
    @Override
    public boolean shouldApplyMixin(String target, String mixin) {
        if (!mixin.endsWith(".AnvilLevelLimitMixin") && !mixin.endsWith(".AnvilScreenMixin"))
            return true;

        // Mixin 加载时，运行阶段的 ModList 尚不可用；须使用加载阶段的模组列表，避免提前初始化神化或 Minecraft 类。
        if (FMLLoader.getLoadingModList().getMods().stream()
                .noneMatch(mod -> mod.getModId().equals("apotheosis"))) return true;
        OptionalCompatState.APOTHEOSIS_ANVIL.check(
                true,
                () -> {
                    var bytecode = MixinService.getService().getBytecodeProvider();
                    boolean server =
                            removesLevelLimit(
                                    bytecode.getClassNode(
                                            "dev.shadowsoffire.apotheosis.mixin.AnvilMenuMixin", true));
                    return server
                            && (!FMLLoader.getDist().isClient()
                                    || removesLevelLimit(
                                            bytecode.getClassNode(
                                                    "dev.shadowsoffire.apotheosis.mixin.client.AnvilScreenMixin",
                                                    true)));
                });
        return false;
    }

    static boolean removesLevelLimit(ClassNode node) {
        return OptionalCompatChecks.anvil(node);
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
