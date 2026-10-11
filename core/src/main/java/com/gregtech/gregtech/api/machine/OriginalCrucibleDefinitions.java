/* Copyright GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later. */
package com.gregtech.gregtech.api.machine;
import java.util.List;

/** Original39 smelting vessel identities, retaining existing port registry paths. */
public final class OriginalCrucibleDefinitions {
    private OriginalCrucibleDefinitions() {}
    public static List<CrucibleSpec> all() { return OriginalSmelteryDefinitions.crucibles(); }
    public static CrucibleSpec get(String id) {
        return all().stream().filter(s -> s.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException(id));
    }
}
