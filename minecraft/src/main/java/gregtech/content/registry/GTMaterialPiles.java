package gregtech.content.registry;

import gregtech.content.block.misc.PileBlock;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class GTMaterialPiles {

    private GTMaterialPiles() {}

    public static RegisteredBlock<PileBlock> INGOT_PILE, PLATE_PILE, PLATE_GEM_PILE;

    public static java.util.List<RegisteredBlock<PileBlock>> all() {
        return java.util.List.of(INGOT_PILE, PLATE_PILE, PLATE_GEM_PILE);
    }

    public static void initialize() {
        INGOT_PILE = registerPile("ingot_pile", PileBlock.Kind.INGOT, MapColor.METAL);
        PLATE_PILE = registerPile("plate_pile", PileBlock.Kind.PLATE, MapColor.METAL);
        PLATE_GEM_PILE = registerPile("plate_gem_pile", PileBlock.Kind.GEM_PLATE, MapColor.DIAMOND);
    }

    private static RegisteredBlock<PileBlock> registerPile(String id, PileBlock.Kind kind, MapColor color) {
        var registered = RegisteredBlock.registerBlock(
                id,
                () -> new PileBlock(
                        kind,
                        BlockBehaviour.Properties.of()
                                .mapColor(color)
                                .strength(.25f, 0)
                                .sound(SoundType.METAL)
                                .noOcclusion()
                                .isSuffocating((s, l, p) -> false)));
        GTBlocks.BLOCK_ITEMS.register(
                id,
                () -> new net.minecraft.world.item.BlockItem(
                        registered.get(), new net.minecraft.world.item.Item.Properties()));
        return registered;
    }
}
