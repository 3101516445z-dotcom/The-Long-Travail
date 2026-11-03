package com.thelongtravail.compat;

public final class CompatBootstrap {
    public static void initialize() {
        com.thelongtravail.compat.fluid.FluidStorageAdapters.initialize();
        if (net.minecraftforge.fml.ModList.get().isLoaded("touhou_little_maid")) {
            com.thelongtravail.integration.MaidIntegration.install(
                    new com.thelongtravail.compat.maid.MaidIntegrationProvider());
            com.thelongtravail.compat.maid.MaidAccessories.init();
        }
    }

    private CompatBootstrap() {}
}
