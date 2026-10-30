package gregtech.content.registry;

import gregtech.content.block.LootCrateBlock;
import gregtech.content.block.misc.*;
import java.util.List;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/** Common decorative block factories; loader facades only retain typed handles. */
public final class SharedDecorBlocks {

    private SharedDecorBlocks() {}

    public static RegisteredBlock<Block> CRATE;
    public static RegisteredBlock<Block> CRATE_FIREPROOF;
    public static RegisteredBlock<LootCrateBlock> LOOT_CRATE;
    public static RegisteredBlock<RoadStripeRailBlock> RAILROAD;
    public static RegisteredBlock<GregLanternBlock> GREG_LANTERN;

    public static void initialize() {
        if (CRATE != null) throw new IllegalStateException("Decorative blocks registered twice");
        CRATE = RegisteredBlock.register(
                "crate",
                () -> new gregtech.content.block.CrateBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.WOOD)
                        .strength(1, 3)
                        .sound(SoundType.WOOD)
                        .ignitedByLava()));
        // The fireproof crate is the same treated crate without flammability.
        CRATE_FIREPROOF = RegisteredBlock.register(
                "crate_fireproof",
                () -> new gregtech.content.block.CrateBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.WOOD)
                        .strength(1, 3)
                        .sound(SoundType.WOOD)));
        LOOT_CRATE = RegisteredBlock.register(
                "loot_crate",
                () -> new LootCrateBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.WOOD)
                        .strength(4, 4)
                        .requiresCorrectToolForDrops()
                        .sound(SoundType.WOOD)));
        RAILROAD = RegisteredBlock.register(
                "railroad",
                () -> new RoadStripeRailBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.METAL)
                        .strength(.35F, 20F)
                        .sound(SoundType.METAL)));
        GREG_LANTERN = RegisteredBlock.register(
                "greg_lantern",
                () -> new GregLanternBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.COLOR_ORANGE)
                        .strength(1, 1)
                        .sound(SoundType.WOOD)
                        .lightLevel(state -> 15)
                        .noOcclusion()));
    }

    public static List<RegisteredBlock<? extends Block>> all() {
        return List.of(CRATE, CRATE_FIREPROOF, LOOT_CRATE, RAILROAD, GREG_LANTERN);
    }
}
