package gregtech.engine.multiblock;

import gregtech.api.machine.IoTarget;
import gregtech.api.multiblock.PortChannel;
import gregtech.api.multiblock.PortRule;
import java.util.Objects;

/** An active outlet. Passive access to a casing is still governed by its layout role. */
public record StructureOutput(
        PortChannel channel, IoTarget target, PortRule rule, boolean requiresFormed, boolean requiresPort) {
    public StructureOutput {
        Objects.requireNonNull(channel);
        Objects.requireNonNull(target);
        Objects.requireNonNull(rule);
        if (channel != PortChannel.ITEM_OUTPUT
                && channel != PortChannel.FLUID_OUTPUT
                && channel != PortChannel.ENERGY_OUTPUT)
            throw new IllegalArgumentException("Not an output channel: " + channel);
        if (requiresPort && !requiresFormed)
            throw new IllegalArgumentException("A live structure port requires a formed structure");
    }

    /** Original GT6 controllers may empty already buffered products even after their shell breaks. */
    public static StructureOutput buffered(PortChannel channel, IoTarget target) {
        return new StructureOutput(channel, target, PortRule.ALL, false, false);
    }

    public static StructureOutput port(PortChannel channel, IoTarget target) {
        return new StructureOutput(channel, target, PortRule.ALL, true, true);
    }

    public StructureOutput restrict(PortRule rule) {
        return new StructureOutput(channel, target, rule, requiresFormed, requiresPort);
    }
}
