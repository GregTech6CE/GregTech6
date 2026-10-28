package gregtech.content.multiblock;

import gregtech.api.multiblock.CellMatch;
import gregtech.api.multiblock.StructureGrid;
import gregtech.api.multiblock.StructureLayout;
import gregtech.api.multiblock.StructureRole;
import java.util.*;

/** Original 25-wall hollow structure; one air cell and controller omitted. */
public final class SharedImplosionStructure {
    private SharedImplosionStructure() {}

    public static final StructureGrid LAYOUT = create();

    private static StructureGrid create() {
        return MultiblockPatterns.grid(StructureLayout.pattern("implosioncompressor")
                .layer(0, "ASA", "AAA", "AAA")
                .layer(1, "AAA", "A A", "AAA")
                .layer(2, "AAA", "AAA", "AAA")
                .where('A', CellMatch.part("wall"), StructureRole.ITEM_IO)
                .where(' ', CellMatch.air(), StructureRole.AIR));
    }
}
