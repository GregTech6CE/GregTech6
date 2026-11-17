package gregtech.engine.machine;

import gregtech.api.material.GTMaterial;

/** Regular steam → KU engine. Uses springSmall + plateDouble in recipes. */
public record SteamEngineSpec(
        String id,
        GTMaterial material,
        int efficiency,
        int steamCapacity,
        long outputRate,
        float hardness,
        float blastResistance) implements SteamEngineData {}
