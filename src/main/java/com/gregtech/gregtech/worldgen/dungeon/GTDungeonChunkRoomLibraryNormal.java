package com.gregtech.gregtech.worldgen.dungeon;

import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import com.gregtech.gregtech.registry.GTWoods;

/**
 * Port of GT6's {@code DungeonChunkRoomLibraryNormal}
 * ({@code gregapi/worldgen/dungeon/DungeonChunkRoomLibraryNormal.java:36-105}): GT6's plain library room.
 * It is {@link GTDungeonChunkRoomLibrary}'s reading hall plus, on every side of the cell that has no
 * neighbour, a low wall of two shelves in slab supports, with coins, pots, a cup and up to two ZPM
 * modules in front of it.
 *
 * <p>GT6's class is the normal-wood variant of the library; the Mystcraft and Thaumcraft libraries share
 * the base class but are other-mod content and are not ported, so this is the only library the port
 * registers (see {@code GTDungeonFeature.ROOMS}).</p>
 *
 * <h2>Port differences</h2>
 * <ul>
 *   <li><b>Tags.</b> GT6 asks for {@code WorldgenDungeonGT.TAG_LIBRARY_NORMAL} first and adds it after the
 *       base class succeeded ({@code :39-40}); that tag is how GT6's config switches the room off. The
 *       port has no dungeon config, so the tag only keeps the room from being built twice.</li>
 *   <li><b>Shelf supports.</b> GT6's shelf walls stand on Blue Spruce slabs - {@code BlocksGT.Planks2}, an
 *       extra GT6 wood, placed upside down ({@code ((BlockMetaType)BlocksGT.Planks2).mSlabs[1]}, the
 *       SIDE_UP variant). The port uses its registered Blue Spruce slab in the same top-half state.</li>
 *   <li><b>Shelves.</b> GT6 uses bookshelf multi-tile 7839 (a different wood than the room's own) and one
 *       facing per wall; the port's shelf helper has neither, and every shelf asks for
 *       {@code stronghold_library} - GT6's own table here ({@code ChestGenHooks.STRONGHOLD_LIBRARY}).</li>
 *   <li><b>ZPM modules.</b> GT6's {@code zpm} sits behind the {@code mZPM} config and reports failure when
 *       the multi-tile cannot be placed, which is why the room remembers whether it placed one already
 *       ({@code tDidntGenerateZPM}). The port's {@link GTDungeonData#zpm(int, int, int)} always places its
 *       ZPM block and reports success, so the latch behaves like GT6 with ZPMs enabled; the port's ZPM has
 *       no energised state either.</li>
 *   <li><b>Pots and cups.</b> GT6 hands {@code pot} and {@code cup} a Hexxit hexorium block
 *       ({@code :43-44}), which may replace the pot or the cup with that block; the port has no Hexxit, and
 *       GT6's own "block is null" fallback chooses a vanilla or GT flower for the pot and fills the cup
 *       with night vision potion. The two Hexxit argument rolls are still consumed before the pot's own
 *       flower rolls, keeping the room stream aligned. Cup paint remains absent.</li>
 *   <li><b>Cell height.</b> This subclass only decorates local 1 and 2, far below GT6's ceiling, so
 *       {@link GTDungeonChunkRoomLibrary}'s nine block tall hall needs no remapping here and GT6's
 *       coordinates are kept as they are.</li>
 *   <li><b>Rolls.</b> Every roll GT6 makes in this subclass is kept: the two {@code next1in4} coin rolls,
 *       the two {@code next(16)} ZPM rolls and the {@code next(2)} cup offset, in GT6's order.</li>
 * </ul>
 */
public class GTDungeonChunkRoomLibraryNormal extends GTDungeonChunkRoomLibrary {

    /** GT6's tag for this variant ({@code WorldgenDungeonGT.TAG_LIBRARY_NORMAL}, {@code WorldgenDungeonGT:77}). */
    public static final String TAG_LIBRARY_NORMAL = "gt.dungeon.library.normal";

    /** GT6's shelf support: an upside down slab of its extra Blue Spruce wood (see the class javadoc). */
    private static BlockState shelfSupport() {
        return GTWoods.SLAB_BLUE_SPRUCE.get().defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP);
    }

    /**
     * GT6's {@code :57} to {@code :101} (twice per wall): what stands in front of a shelf wall - a pot, or,
     * once per cell and with a 1 in 16 chance, a ZPM module instead (GT6's {@code tDidntGenerateZPM}
     * latch, see the class javadoc). Returns whether no ZPM has been generated yet, which is guard and
     * result at once in GT6.
     */
    private static boolean potOrZpm(GTDungeonData data, int x, int y, int z, boolean noZpmYet) {
        if (noZpmYet && data.next(16) == 0 && data.zpm(x, y, z)) return false;
        // Java evaluates GT6's two Hexxit-block arguments even without Hexxit installed.
        data.next1in2();
        data.next1in2();
        data.pot(x, y, z);
        return noZpmYet;
    }

    @Override
    public boolean generate(GTDungeonData data) {
        // GT6's :39.
        if (data.hasTag(TAG_LIBRARY_NORMAL) || !super.generate(data)) return false;
        data.tags.add(TAG_LIBRARY_NORMAL);

        // GT6's :46.
        boolean didNotGenerateZPM = true;

        if (!data.connected(1, 0)) {
            // GT6's :48-61.
            data.set(14, 1, 5, shelfSupport());
            data.shelf(14, 1, 6, SHELF_LOOT);
            data.set(14, 1, 7, shelfSupport());
            data.set(14, 1, 8, shelfSupport());
            data.shelf(14, 1, 9, SHELF_LOOT);
            data.set(14, 1, 10, shelfSupport());

            if (data.next1in4()) data.coins(14, 2, 5);
            didNotGenerateZPM = potOrZpm(data, 14, 2, 6, didNotGenerateZPM);
            data.cup(14, 2, 7 + data.next(2), "Potion_NightVision_1L");
            didNotGenerateZPM = potOrZpm(data, 14, 2, 9, didNotGenerateZPM);
            if (data.next1in4()) data.coins(14, 2, 10);
        }
        if (!data.connected(-1, 0)) {
            // GT6's :62-75.
            data.set(1, 1, 5, shelfSupport());
            data.shelf(1, 1, 6, SHELF_LOOT);
            data.set(1, 1, 7, shelfSupport());
            data.set(1, 1, 8, shelfSupport());
            data.shelf(1, 1, 9, SHELF_LOOT);
            data.set(1, 1, 10, shelfSupport());

            if (data.next1in4()) data.coins(1, 2, 5);
            didNotGenerateZPM = potOrZpm(data, 1, 2, 6, didNotGenerateZPM);
            data.cup(1, 2, 7 + data.next(2), "Potion_NightVision_1L");
            didNotGenerateZPM = potOrZpm(data, 1, 2, 9, didNotGenerateZPM);
            if (data.next1in4()) data.coins(1, 2, 10);
        }
        if (!data.connected(0, 1)) {
            // GT6's :76-89.
            data.set(5, 1, 14, shelfSupport());
            data.shelf(6, 1, 14, SHELF_LOOT);
            data.set(7, 1, 14, shelfSupport());
            data.set(8, 1, 14, shelfSupport());
            data.shelf(9, 1, 14, SHELF_LOOT);
            data.set(10, 1, 14, shelfSupport());

            if (data.next1in4()) data.coins(5, 2, 14);
            didNotGenerateZPM = potOrZpm(data, 6, 2, 14, didNotGenerateZPM);
            data.cup(7 + data.next(2), 2, 14, "Potion_NightVision_1L");
            didNotGenerateZPM = potOrZpm(data, 9, 2, 14, didNotGenerateZPM);
            if (data.next1in4()) data.coins(10, 2, 14);
        }
        if (!data.connected(0, -1)) {
            // GT6's :90-103.
            data.set(5, 1, 1, shelfSupport());
            data.shelf(6, 1, 1, SHELF_LOOT);
            data.set(7, 1, 1, shelfSupport());
            data.set(8, 1, 1, shelfSupport());
            data.shelf(9, 1, 1, SHELF_LOOT);
            data.set(10, 1, 1, shelfSupport());

            if (data.next1in4()) data.coins(5, 2, 1);
            didNotGenerateZPM = potOrZpm(data, 6, 2, 1, didNotGenerateZPM);
            data.cup(7 + data.next(2), 2, 1, "Potion_NightVision_1L");
            didNotGenerateZPM = potOrZpm(data, 9, 2, 1, didNotGenerateZPM);
            if (data.next1in4()) data.coins(10, 2, 1);
        }

        return true;
    }
}
