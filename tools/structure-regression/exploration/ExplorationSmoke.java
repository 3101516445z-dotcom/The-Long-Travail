package travail.smoke;

import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.common.Mod;

@Mod("travail_smoke")
public final class ExplorationSmoke {
    public ExplorationSmoke() {
        MinecraftForge.EVENT_BUS.addListener(this::started);
    }

    private void started(ServerStartedEvent event) {
        String result = "NOT_RUN";
        try {
            JourneyQuerySmoke.run(event.getServer().overworld());
            WayguideCursorChecks.run(event.getServer().overworld());
            result = "PASS";
        } catch (Throwable failure) {
            failure.printStackTrace();
            result = "FAIL " + failure;
        } finally {
            try {
                Files.writeString(Path.of("exploration-result.txt"), result);
            } catch (Exception failure) {
                throw new RuntimeException(failure);
            } finally {
                event.getServer().halt(false);
            }
        }
    }
}
