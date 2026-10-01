package com.gregtech.gregtech.data;

import com.gregtech.gregtech.api.prefix.PrefixRegistry;

/**
 * One-shot bootstrap for GT6-style data tables (CS, TD, OP, RM, ...).
 * Call before material item registration.
 */
public final class GregTechData {
    private static boolean initialized = false;

    private GregTechData() {}

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        GregTechConstants.bootstrap();
        GregTechTags.bootstrap();
        AspectReferences.bootstrap();
        OreDictionaryNames.bootstrap();
        PrefixRegistry.ensurePrefixesLoaded();
        MaterialPrefixes.bootstrap();
        MachineRecipeMaps.bootstrap();
        FuelRecipeMaps.bootstrap();
        RegisteredFluids.bootstrap();
        ItemReferences.bootstrap();
        TranslationKeys.bootstrap();
        BlockIcons.bootstrap();
        ModReferences.UNKNOWN.getClass(); // ensure MD static holder
        MaterialGroups.Glowstone.getClass(); // ensure ANY static holder
    }
}
