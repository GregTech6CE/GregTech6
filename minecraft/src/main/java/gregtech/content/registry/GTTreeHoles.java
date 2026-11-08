package gregtech.content.registry;

import static gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE;

import gregtech.content.block.wood.TreeHoleBlock;
import gregtech.content.block.wood.WoodSpecies;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * GT6's three tree holes ({@code Loader_MultiTileEntities}, multi-tile ids 32762/32761/32760):
 * the rubber resin hole, the tapped maple and the tapped rainbowood.
 *
 * <p>Registry ids follow GT6's names. The resin hole is placed by the rubber tree shape itself (GT6
 * {@code BlockTreeSaplingAB} case 0), the two sap holes are drilled into a maple/rainbowood log with a
 * hand drill (GT6 {@code BlockTreeLogA} / {@code BlockTreeLogB}).
 */
public final class GTTreeHoles {
    private static final List<RegisteredBlock<? extends Block>> ALL = new ArrayList<>();
    private static final Map<WoodSpecies, RegisteredBlock<TreeHoleBlock>> HOLES = new LinkedHashMap<>();

    private GTTreeHoles() {}

    public static List<RegisteredBlock<? extends Block>> all() {
        return Collections.unmodifiableList(ALL);
    }

    public static List<Block> blocks() {
        List<Block> out = new ArrayList<>();
        for (RegisteredBlock<? extends Block> entry : ALL) if (entry.isPresent()) out.add(entry.get());
        return out;
    }

    /** The hole block of a species, or {@code null} for species GT6 has no hole for. */
    public static TreeHoleBlock hole(WoodSpecies species) {
        RegisteredBlock<TreeHoleBlock> entry = HOLES.get(species);
        return entry == null || !entry.isPresent() ? null : entry.get();
    }

    public static void registerAll(ContentRegistrar registrar) {
        hole(registrar, "resin_hole_rubber", WoodSpecies.RUBBER, "item:gt6ce:rubber_resin");
        hole(registrar, "tapped_maple", WoodSpecies.MAPLE, "fluid:gt6ce:maplesap");
        hole(registrar, "tapped_rainbowood", WoodSpecies.RAINBOWOOD, "fluid:gt6ce:rainbowsap");
    }

    private static void hole(ContentRegistrar registrar, String id, WoodSpecies species, String yield) {
        RegisteredBlock<TreeHoleBlock> block = registrar.block(
                REGISTRY_NAMESPACE,
                id,
                () -> new TreeHoleBlock(
                        species,
                        yield,
                        BlockBehaviour.Properties.of()
                                .mapColor(MapColor.WOOD)
                                .strength(2.0f, 3.0f)
                                .sound(SoundType.WOOD)
                                .noOcclusion()),
                bare -> new BlockItem(bare, new Item.Properties()));
        ALL.add(block);
        HOLES.put(species, block);
    }
}
