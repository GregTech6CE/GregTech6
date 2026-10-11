/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * MultiTileEntityDistillationTower and MultiTileEntityCryoDistillationTower.doOutputFluids. */
package com.gregtech.gregtech.content.multiblock;

import java.util.Locale;

/** Height of the original rear outlet, relative to the front-bottom controller. */
public final class OriginalDistillationOutputRules {
    private OriginalDistillationOutputRules() {}
    public static int fluidHeight(boolean cryogenic, String originalRegistryName) {
        String name=originalRegistryName==null?"":originalRegistryName.toLowerCase(Locale.ROOT);
        if(cryogenic) return switch(name) {
            case "helium" -> 7;
            case "neon" -> 6;
            case "nitrogen" -> 5;
            case "oxygen" -> 4;
            case "argon" -> 3;
            case "carbondioxide","sulfurdioxide" -> 2;
            default -> 1;
        };
        return switch(name) {
            case "propane","methane" -> 7;
            case "butane" -> 6;
            case "petrol","gasoline","bioethanol" -> 5;
            case "kerosene","kerosine","glycerol" -> 4;
            case "diesel","biodiesel" -> 3;
            case "fuel","fueloil","biofuel" -> 2;
            default -> 1;
        };
    }
}
