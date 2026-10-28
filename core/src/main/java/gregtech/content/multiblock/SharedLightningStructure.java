package gregtech.content.multiblock;

import gregtech.api.multiblock.CellMatch;
import gregtech.api.multiblock.StructureLayout;
import gregtech.api.multiblock.StructureRole;

/** Five base layers plus an optional upward rod, bounded by the shared repeat limit. */
public final class SharedLightningStructure {
    private SharedLightningStructure() {}

    public static final SharedLargeMachineLayouts.Layout LAYOUT =
            MultiblockPatterns.layout(StructureLayout.pattern("lightningrod")
                    .layer(0, "WWW", "WSW", "WWW")
                    .layer(1, "CCC", "CCC", "CCC")
                    .layer(2, "WWW", "WWW", "WWW")
                    .layer(3, "CCC", "CCC", "CCC")
                    .layer(4, "WWW", "WWW", "WWW")
                    .repeat("rod", 5, 0, -1, "...", ".R.", "...")
                    .ignore('.')
                    .where('W', CellMatch.part("tungsten_wall"), StructureRole.CASING)
                    .where('C', CellMatch.part("large_niobium_titanium_coil"), StructureRole.CASING)
                    .where('R', CellMatch.part("lightning_rod"), StructureRole.CASING)
                    .build());
}
