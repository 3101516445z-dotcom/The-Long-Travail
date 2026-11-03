package travail.underworldscope;

import java.nio.file.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.*;

@Mod("travail_smoke")
public final class UnderworldScopeSmoke {
    private MinecraftServer server;
    private boolean done;
    private static final DeferredRegister<Enchantment> ENCHANTMENTS =
            DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, "travail_smoke");

    static {
        ENCHANTMENTS.register(
                "broken",
                () ->
                        new Enchantment(
                                Enchantment.Rarity.COMMON,
                                EnchantmentCategory.WEAPON,
                                new EquipmentSlot[] {EquipmentSlot.MAINHAND}) {
                            @Override
                            public void doPostAttack(
                                    LivingEntity attacker, Entity target, int level) {
                                if (UnderworldScopeProbe.attackProbe != null)
                                    UnderworldScopeProbe.attackProbe.run();
                            }

                            @Override
                            public void doPostHurt(
                                    LivingEntity victim, Entity attacker, int level) {
                                if (UnderworldScopeProbe.defenseProbe != null)
                                    UnderworldScopeProbe.defenseProbe.run();
                            }
                        });
    }

    public UnderworldScopeSmoke() {
        ENCHANTMENTS.register(
                net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus());
        MinecraftForge.EVENT_BUS.addListener(
                (ServerStartedEvent event) -> server = event.getServer());
        MinecraftForge.EVENT_BUS.addListener(this::tick);
    }

    private void tick(TickEvent.ServerTickEvent event) {
        if (server == null || done || event.phase != TickEvent.Phase.END) return;
        done = true;
        String result;
        try {
            UnderworldScopeProbe.run(server);
            result = "PASS underworld scope";
        } catch (Throwable error) {
            error.printStackTrace();
            result = "FAIL " + error;
        }
        System.out.println(result);
        try {
            Files.writeString(Path.of("underworld-scope-result.txt"), result);
        } catch (Exception error) {
            throw new RuntimeException(error);
        }
        server.halt(false);
    }
}
