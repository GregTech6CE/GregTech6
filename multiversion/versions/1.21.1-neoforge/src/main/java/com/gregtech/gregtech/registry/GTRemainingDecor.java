package com.gregtech.gregtech.registry;

import com.gregtech.gregtech.block.misc.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredHolder;
import java.util.*;

public final class GTRemainingDecor {

    private GTRemainingDecor() {
    }
    public static final List<DeferredHolder<Block, PanelBlock>> PANELS = new ArrayList<>();
    public static DeferredHolder<Block, RoadStripeRailBlock> RAILROAD;
    public static DeferredHolder<Block, GregLanternBlock> GREG_LANTERN;

    private static <T extends Block> DeferredHolder<Block, T> registerPanelBlock(String id, java.util.function.Supplier<T> factory) {
        var panelBlockHandle = GTBlocks.BLOCKS.register(id, factory);
        GTBlocks.BLOCK_ITEMS.register(id, () -> panelBlockHandle.get() instanceof PanelBlock panel ? new com.gregtech.gregtech.item.PanelBlockItem(panel,
                new net.minecraft.world.item.Item.Properties()) : new net.minecraft.world.item.BlockItem(panelBlockHandle.get(),
                new net.minecraft.world.item.Item.Properties()));
        return panelBlockHandle;
    }

    public static void initialize() {
        SharedDecorBlocks.initialize();
        for (var variant : com.gregtech.gregtech.content.transport.PanelCatalog.ALL)
            if (java.util.Set.of("wood", "concrete", "cfoam", "asphalt", "colored_gray", "colored_black").contains(variant.material()))
                PANELS.add(registerPanelBlock(variant.id(), () -> new PanelBlock(variant.material(), variant.tint(),
                        BlockBehaviour.Properties.of().mapColor(switch (variant.mapColor()) {
                            case "WOOD" -> MapColor.WOOD;
                            case "STONE" -> MapColor.STONE;
                            case "WOOL" -> MapColor.WOOL;
                            case "COLOR_BLACK" -> MapColor.COLOR_BLACK;
                            default -> MapColor.COLOR_GRAY;
                        }).strength(variant.hardness(), variant.hardness()).requiresCorrectToolForDrops())));
        for (var variant : com.gregtech.gregtech.content.transport.PanelCatalog.ALL)
            if (!java.util.Set.of("wood", "concrete", "cfoam", "asphalt", "colored_gray", "colored_black").contains(variant.material()))
                GTBlocks.BLOCK_ITEMS.register(variant.id(),
                        () -> new com.gregtech.gregtech.item.PanelItem(variant, new net.minecraft.world.item.Item.Properties()));
        RAILROAD = DeferredHolder.create(net.minecraft.core.registries.Registries.BLOCK,
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6ce", "railroad"));
        GREG_LANTERN = DeferredHolder.create(net.minecraft.core.registries.Registries.BLOCK,
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6ce", "greg_lantern"));
    }
}
