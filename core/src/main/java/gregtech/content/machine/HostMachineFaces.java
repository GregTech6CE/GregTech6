package gregtech.content.machine;

import gregtech.api.machine.MachineFace;
import gregtech.api.machine.MachineFaceMasks;
import java.util.EnumSet;
import java.util.Set;

/** Converts the original GT6 bitmasks and auto-face indices into API face values. */
public final class HostMachineFaces {
    private HostMachineFaces() {}

    public static MachineFaceMasks fromMask(
            int itemInputs,
            int itemOutputs,
            int fluidInputs,
            int fluidOutputs,
            int energyInputs,
            int energyOutputs,
            int itemAutoInput,
            int itemAutoOutput,
            int fluidAutoInput,
            int fluidAutoOutput) {
        return new MachineFaceMasks(
                faces(itemInputs), faces(itemOutputs),
                faces(fluidInputs), faces(fluidOutputs),
                faces(energyInputs), faces(energyOutputs),
                auto(itemAutoInput), auto(itemAutoOutput),
                auto(fluidAutoInput), auto(fluidAutoOutput));
    }

    private static Set<MachineFace> faces(int mask) {
        var faces = EnumSet.noneOf(MachineFace.class);
        for (MachineFace face : MachineFace.values()) {
            if ((mask & (1 << face.index())) != 0) faces.add(face);
        }
        return faces;
    }

    private static MachineFace auto(int index) {
        if (index < 0) return null;
        if (index >= MachineFace.values().length)
            throw new IllegalArgumentException("Invalid automatic face index: " + index);
        return MachineFace.values()[index];
    }
}
