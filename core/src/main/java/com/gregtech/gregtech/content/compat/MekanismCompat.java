package com.gregtech.gregtech.content.compat;

import java.util.List;

/**
 * The 1.7.10 Mekanism class dyes eight metadata items and deletes the 2x2 salt block.
 * Mekanism 10 no longer registers those dye targets. The salt block recipe id is unchanged.
 */
public final class MekanismCompat {
    private static final String SOURCE = "Compat_Recipes_Mekanism.java";

    private MekanismCompat() {}

    public static CompatSpecs.Module module() {
        return new CompatSpecs.Module("mekanism", true, true, "Compat_Recipes_Mekanism",
                List.of(), List.of(), List.of(),
                List.of(new CompatSpecs.Removal("mekanism:storage_blocks/salt", SOURCE + ":41")),
                List.of(
                        SOURCE + ":44 Balloon is not registered in Mekanism 10.4 or 10.7",
                        SOURCE + ":45 PlasticFence is not registered in Mekanism 10.4 or 10.7",
                        SOURCE + ":46 GlowPanel is not registered in Mekanism 10.4 or 10.7",
                        SOURCE + ":47 RoadPlasticBlock is not registered in Mekanism 10.4 or 10.7",
                        SOURCE + ":48 PlasticBlock is not registered in Mekanism 10.4 or 10.7",
                        SOURCE + ":49 SlickPlasticBlock is not registered in Mekanism 10.4 or 10.7",
                        SOURCE + ":50 GlowPlasticBlock is not registered in Mekanism 10.4 or 10.7",
                        SOURCE + ":51 ReinforcedPlasticBlock is not registered in Mekanism 10.4 or 10.7",
                        "PrefixBlock.java:224 and MultiTileEntityBlock.java:139 MekanismAPI box blacklist stays an API bridge",
                        "LoaderUnificationTargets.java:1057-1078 Mekanism forms are not made the canonical GT item",
                        "MT.java:2434-2438 material ownership stays on the GT material",
                        "GT_API_Proxy_Client.java:258 Osmium tooltip rename is not this batch",
                        "LoaderItemList.java:644 Mekanism ore as a replaceable stone stays deferred"));
    }
}
