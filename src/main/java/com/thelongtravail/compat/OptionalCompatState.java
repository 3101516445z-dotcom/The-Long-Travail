package com.thelongtravail.compat;

import java.io.IOException;
import org.slf4j.LoggerFactory;

public final class OptionalCompatState {
    public static final Gate GRAVESTONE = new Gate("Gravestone item reconciliation");
    public static final Gate CAELUS = new Gate("Caelus Icarus flight adapter");
    public static final Gate APOTHEOSIS_ANVIL =
            new Gate("Apotheosis anvil level-limit coordination");

    @FunctionalInterface
    public interface Probe {
        boolean supported() throws ClassNotFoundException, IOException;
    }

    public static final class Gate {
        private enum Status {
            UNCHECKED,
            ABSENT,
            ENABLED,
            DISABLED
        }

        private final String feature;
        private volatile Status status = Status.UNCHECKED;

        Gate(String feature) {
            this.feature = feature;
        }

        public synchronized boolean check(boolean installed, Probe probe) {
            if (status != Status.UNCHECKED) return enabled();
            if (!installed) {
                status = Status.ABSENT;
                return false;
            }
            try {
                if (probe.supported()) status = Status.ENABLED;
                else disable("unsupported method, field or instruction layout");
            } catch (ClassNotFoundException | IOException | LinkageError failure) {
                disable(failure.toString());
            }
            return enabled();
        }

        public boolean enabled() {
            return status == Status.ENABLED;
        }

        public synchronized void disable(String reason) {
            if (status == Status.DISABLED) return;
            status = Status.DISABLED;
            LoggerFactory.getLogger("the_long_travail.compat")
                    .warn("Disabled {} for this launch: {}", feature, reason);
        }
    }

    private OptionalCompatState() {}
}
