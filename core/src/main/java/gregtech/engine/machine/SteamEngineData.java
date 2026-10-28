package gregtech.engine.machine;

import gregtech.api.material.GTMaterial;

/** Shared interface for {@link SteamEngineSpec} and {@link StrongSteamEngineSpec}. */
public interface SteamEngineData {
    String id();

    GTMaterial material();

    int efficiency();

    int steamCapacity();

    long outputRate();

    default long outputAtPressure(int state) {
        return outputRate() * (Math.max(0, Math.min(31, state)) + 1) / 16;
    }

    /** The first sampled pressure step whose output exceeds half of rated power, including rounding. */
    default double minimumRunningFill() {
        for (int state = 0; state < 32; state++) if (outputAtPressure(state) > outputRate() / 2) return state / 32.0;
        return 1;
    }

    float hardness();

    float blastResistance();

    default int tintRgb() {
        return material().getColor();
    }

    default String materialName() {
        return material().getLocalName();
    }

    default String textureFolder() {
        return "kinetic_steam";
    }

    /** Steam consumed per operation (mB). */
    default int steamPerOp() {
        return 200;
    }
    /** Distilled water produced per operation (mB). */
    default int waterPerOp() {
        return 1;
    }
}
