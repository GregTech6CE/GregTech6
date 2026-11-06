package gregtech.content.registry;

import static gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE;

import gregtech.content.block.misc.*;
import java.util.*;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class GTRemainingDecor {

    private GTRemainingDecor() {}

    public static final List<RegisteredBlock<PanelBlock>> PANELS = new ArrayList<>();
    public static RegisteredBlock<RoadStripeRailBlock> RAILROAD;
    public static RegisteredBlock<GregLanternBlock> GREG_LANTERN;

    private static <T extends Block> RegisteredBlock<T> registerPanelBlock(
            ContentRegistrar registrar, String id, java.util.function.Supplier<T> factory) {
        return registrar.block(
                REGISTRY_NAMESPACE,
                id,
                factory,
                bare -> bare instanceof PanelBlock panel
                        ? new gregtech.content.item.PanelBlockItem(panel, new Item.Properties())
                        : new BlockItem(bare, new Item.Properties()));
    }

    public static void initialize(ContentRegistrar registrar) {
        for (var variant : gregtech.content.transport.PanelCatalog.ALL)
            if (java.util.Set.of("wood", "concrete", "cfoam", "asphalt", "colored_gray", "colored_black")
                    .contains(variant.material()))
                PANELS.add(registerPanelBlock(
                        registrar,
                        variant.id(),
                        () -> new PanelBlock(
                                variant.material(),
                                variant.tint(),
                                BlockBehaviour.Properties.of()
                                        .mapColor(
                                                switch (variant.mapColor()) {
                                                    case "WOOD" -> MapColor.WOOD;
                                                    case "STONE" -> MapColor.STONE;
                                                    case "WOOL" -> MapColor.WOOL;
                                                    case "COLOR_BLACK" -> MapColor.COLOR_BLACK;
                                                    default -> MapColor.COLOR_GRAY;
                                                })
                                        .strength(variant.hardness(), variant.hardness())
                                        .requiresCorrectToolForDrops())));
        for (var variant : gregtech.content.transport.PanelCatalog.ALL)
            if (!java.util.Set.of("wood", "concrete", "cfoam", "asphalt", "colored_gray", "colored_black")
                    .contains(variant.material()))
                registrar.blockItem(
                        REGISTRY_NAMESPACE,
                        variant.id(),
                        () -> new gregtech.content.item.PanelItem(variant, new Item.Properties()));
        RAILROAD = RegisteredBlock.reference("railroad");
        GREG_LANTERN = RegisteredBlock.reference("greg_lantern");
    }
}
