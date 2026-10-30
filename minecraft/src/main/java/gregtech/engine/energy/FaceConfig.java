package gregtech.engine.energy;

import gregtech.api.machine.MachineFace;
import gregtech.api.machine.MachineFaceMasks;
import java.util.Set;
import net.minecraft.core.Direction;

/** Platform direction adapter for the shared original face masks. */
public record FaceConfig(
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
    public static final int BOTTOM = 0, TOP = 1, LEFT = 4, RIGHT = 2, FRONT = 3, BACK = 5, AUTO_NONE = -1;

    public static FaceConfig from(MachineFaceMasks m) {
        return new FaceConfig(
                mask(m.itemInputs()),
                mask(m.itemOutputs()),
                mask(m.fluidInputs()),
                mask(m.fluidOutputs()),
                mask(m.energyInputs()),
                mask(m.energyOutputs()),
                auto(m.itemAutoInput()),
                auto(m.itemAutoOutput()),
                auto(m.fluidAutoInput()),
                auto(m.fluidAutoOutput()));
    }

    public static final FaceConfig ALL_SIDES = from(MachineFaceMasks.ALL_SIDES);
    public static final FaceConfig TOP_IN_BOTTOM_OUT = from(MachineFaceMasks.TOP_IN_BOTTOM_OUT);
    public static final FaceConfig ALL_IN_ALL_OUT = from(MachineFaceMasks.ALL_IN_ALL_OUT);
    public static final FaceConfig TOP_IN_BOTTOM_FLUID = from(MachineFaceMasks.TOP_IN_BOTTOM_FLUID);
    public static final FaceConfig NO_ENERGY = from(MachineFaceMasks.NO_ENERGY);

    public static boolean has(int mask, Direction d) {
        return d != null && has(mask, d.get3DDataValue());
    }

    public static boolean has(Set<MachineFace> faces, int rel) {
        for (MachineFace face : faces) if (face.index() == rel) return true;
        return false;
    }

    public static boolean has(int mask, int d) {
        return (mask & (1 << d)) != 0;
    }

    public static boolean autoValid(int d) {
        return d >= 0 && d < MachineFace.values().length;
    }

    public static Builder builder() {
        return new Builder();
    }

    private static int mask(Set<MachineFace> faces) {
        int mask = 0;
        for (MachineFace face : faces) mask |= 1 << face.index();
        return mask;
    }

    private static int auto(MachineFace face) {
        return face == null ? AUTO_NONE : face.index();
    }

    private static MachineFace face(int index) {
        if (!autoValid(index)) throw new IllegalArgumentException("Machine face must be 0..5: " + index);
        return MachineFace.values()[index];
    }

    private static MachineFace[] faces(int... indexes) {
        var faces = new MachineFace[indexes.length];
        for (int i = 0; i < indexes.length; i++) faces[i] = face(indexes[i]);
        return faces;
    }

    public static class Builder {
        private final MachineFaceMasks.Builder delegate = MachineFaceMasks.builder();

        private static int[] sides(Direction[] ds) {
            return java.util.Arrays.stream(ds)
                    .filter(java.util.Objects::nonNull)
                    .mapToInt(Direction::get3DDataValue)
                    .toArray();
        }

        public Builder itemIn(int... ds) {
            delegate.itemIn(faces(ds));
            return this;
        }

        public Builder itemIn(Direction... ds) {
            return itemIn(sides(ds));
        }

        public Builder itemOut(int... ds) {
            delegate.itemOut(faces(ds));
            return this;
        }

        public Builder itemOut(Direction... ds) {
            return itemOut(sides(ds));
        }

        public Builder fluidIn(int... ds) {
            delegate.fluidIn(faces(ds));
            return this;
        }

        public Builder fluidIn(Direction... ds) {
            return fluidIn(sides(ds));
        }

        public Builder fluidOut(int... ds) {
            delegate.fluidOut(faces(ds));
            return this;
        }

        public Builder fluidOut(Direction... ds) {
            return fluidOut(sides(ds));
        }

        public Builder energyIn(int... ds) {
            delegate.energyIn(faces(ds));
            return this;
        }

        public Builder energyIn(Direction... ds) {
            return energyIn(sides(ds));
        }

        public Builder energyOut(int... ds) {
            delegate.energyOut(faces(ds));
            return this;
        }

        public Builder energyOut(Direction... ds) {
            return energyOut(sides(ds));
        }

        public Builder itemAutoIn(int d) {
            delegate.itemAutoIn(d < 0 ? null : face(d));
            return this;
        }

        public Builder itemAutoIn(Direction d) {
            return itemAutoIn(d == null ? AUTO_NONE : d.get3DDataValue());
        }

        public Builder itemAutoOut(int d) {
            delegate.itemAutoOut(d < 0 ? null : face(d));
            return this;
        }

        public Builder itemAutoOut(Direction d) {
            return itemAutoOut(d == null ? AUTO_NONE : d.get3DDataValue());
        }

        public Builder fluidAutoIn(int d) {
            delegate.fluidAutoIn(d < 0 ? null : face(d));
            return this;
        }

        public Builder fluidAutoIn(Direction d) {
            return fluidAutoIn(d == null ? AUTO_NONE : d.get3DDataValue());
        }

        public Builder fluidAutoOut(int d) {
            delegate.fluidAutoOut(d < 0 ? null : face(d));
            return this;
        }

        public Builder fluidAutoOut(Direction d) {
            return fluidAutoOut(d == null ? AUTO_NONE : d.get3DDataValue());
        }

        public FaceConfig build() {
            return from(delegate.build());
        }
    }
}
