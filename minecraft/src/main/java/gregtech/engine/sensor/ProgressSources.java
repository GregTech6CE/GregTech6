package gregtech.engine.sensor;

import gregtech.api.machine.MachineControl;
import gregtech.api.sensor.ProgressSensorSource;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import javax.annotation.Nullable;

/** Read-only progress resolution. Does not advertise switch/mode controls to other consumers. */
public final class ProgressSources {
    private ProgressSources() {}

    /** Lazy per-operation access; resolve again for each observation, never cache a relay target. */
    public interface Access {
        long value();
        long maximum();
    }

    /** Sided read-only sources, independent of switch/mode capability availability. */
    public interface Provider {
        @Nullable Access progressSource(Direction side);
    }

    @Nullable
    public static Access find(BlockEntity entity, Direction side) {
        if (entity == null || entity.isRemoved()) return null;
        if (entity instanceof ProgressSensorSource source) {
            int face = side == null ? -1 : side.ordinal();
            return new Access() {
                @Override public long value() { return source.progressValue(face); }
                @Override public long maximum() { return source.progressMaximum(face); }
            };
        }
        if (entity instanceof Provider provider) return provider.progressSource(side);
        var control = MachineControl.find(entity, side);
        if (control == null || !control.available() || !control.supportsProgress()) return null;
        return new Access() {
            @Override public long value() { return control.progress(); }
            @Override public long maximum() { return control.progressMax(); }
        };
    }
}
