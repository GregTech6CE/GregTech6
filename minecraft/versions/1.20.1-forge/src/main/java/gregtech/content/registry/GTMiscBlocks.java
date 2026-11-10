package gregtech.content.registry;

import static gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE;

import gregtech.content.block.misc.*;
import java.util.function.Supplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/** N10: decorative panels. */
public final class GTMiscBlocks {
    // N10: Decorative panels (6 types)
    public static RegisteredBlock<PanelBlock> PANEL_WOOD;
    public static RegisteredBlock<PanelBlock> PANEL_CONCRETE;
    public static RegisteredBlock<PanelBlock> PANEL_CFOAM;
    public static RegisteredBlock<PanelBlock> PANEL_ASPHALT;
    public static RegisteredBlock<PanelBlock> PANEL_COLORED_GRAY;
    public static RegisteredBlock<PanelBlock> PANEL_COLORED_BLACK;

    private GTMiscBlocks() {}

    private static RegisteredBlock<PanelBlock> reg(
            ContentRegistrar registrar, String id, Supplier<PanelBlock> blockSupplier) {
        return registrar.block(
                REGISTRY_NAMESPACE,
                id,
                blockSupplier,
                block -> new gregtech.content.item.PanelBlockItem(block, new Item.Properties()));
    }

    public static void registerAll(ContentRegistrar registrar) {
        PANEL_WOOD = reg(registrar, "panel_wood", () -> panel("wood"));
        PANEL_CONCRETE = reg(registrar, "panel_concrete", () -> panel("concrete"));
        PANEL_CFOAM = reg(registrar, "panel_cfoam", () -> panel("cfoam"));
        PANEL_ASPHALT = reg(registrar, "panel_asphalt", () -> panel("asphalt"));
        PANEL_COLORED_GRAY = reg(registrar, "panel_colored_gray", () -> panel("colored_gray"));
        PANEL_COLORED_BLACK = reg(registrar, "panel_colored_black", () -> panel("colored_black"));

        for (var spec : gregtech.content.transport.PanelCatalog.ALL)
            if (!java.util.Set.of("wood", "concrete", "cfoam", "asphalt", "colored_gray", "colored_black")
                    .contains(spec.material()))
                registrar.blockItem(
                        REGISTRY_NAMESPACE,
                        spec.id(),
                        () -> new gregtech.content.item.PanelItem(spec, new Item.Properties()));
    }

    private static PanelBlock panel(String material) {
        var s = gregtech.content.transport.PanelCatalog.get(material);
        return new PanelBlock(
                s.material(),
                s.tint(),
                props(
                        switch (s.mapColor()) {
                            case "WOOD" -> MapColor.WOOD;
                            case "STONE" -> MapColor.STONE;
                            case "WOOL" -> MapColor.WOOL;
                            case "COLOR_BLACK" -> MapColor.COLOR_BLACK;
                            default -> MapColor.COLOR_GRAY;
                        },
                        s.hardness()));
    }

    private static BlockBehaviour.Properties props(MapColor color, float hardness) {
        return BlockBehaviour.Properties.of()
                .mapColor(color)
                .strength(hardness, hardness)
                .requiresCorrectToolForDrops();
    }
}
