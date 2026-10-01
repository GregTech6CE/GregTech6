package com.gregtech.gregtech.loaders.a;

import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.material.MaterialProperty;
import com.gregtech.gregtech.api.prefix.BlockMaterialPrefix;
import com.gregtech.gregtech.api.prefix.BlockPrefixRegistry;
import com.gregtech.gregtech.block.MaterialBlock;
import com.gregtech.gregtech.block.OreBlock;
import com.gregtech.gregtech.block.stone.GTStoneBlock;
import com.gregtech.gregtech.block.stone.GTStoneSlabBlock;
import com.gregtech.gregtech.block.stone.StoneType;
import com.gregtech.gregtech.block.stone.StoneVariant;
import com.gregtech.gregtech.registry.GTBlocks;
import com.gregtech.gregtech.registry.GTSpecialOreBlocks;
import com.mojang.logging.LogUtils;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Set;

/** Registers GT6 material blocks, stone blocks, and stone slabs — logic inlined from three separate loader classes. */
public record Loader_Blocks() implements Runnable {
    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void run() {
        registerMaterialBlocks();
        registerStoneBlocks();
        registerStoneSlabs();
        registerIconSetBlocks();
        com.gregtech.gregtech.registry.GTMultiblocks.registerAll();
    }

    private static void registerIconSetBlocks() {
        GTSpecialOreBlocks.registerAll();
        com.gregtech.gregtech.registry.GTPlants.registerAll();
        LOGGER.info("Queued {} icon set blocks for registration", GTSpecialOreBlocks.all().size());
    }

    // === Material blocks ===
    private static void registerMaterialBlocks() {
        BlockPrefixRegistry.ensurePrefixesLoaded();
        Set<String> seenIds = new HashSet<>();
        int count = 0;

        for (var definition : com.gregtech.gregtech.api.material.MaterialBlockDefinitions.all()) {
            GTMaterial material = definition.material();
            BlockMaterialPrefix prefix = definition.prefix();
            String blockId = definition.blockId();
            if (!blockId.equals(definition.baseBlockId())) {
                LOGGER.warn("Duplicate block id '{}' for material {} (id={}); using '{}'",
                        definition.baseBlockId(), material.getName(), material.getId(), blockId);
            }
            DeferredHolder<Block, Block> block = GTBlocks.BLOCKS.register(blockId, () -> createBlock(prefix, material));
            GTBlocks.bind(prefix, material, block);
            GTBlocks.registerBlockItem(blockId, block);
            count++;
        }
        LOGGER.info("Queued {} material blocks for registration", count);
    }

    private static Block createBlock(BlockMaterialPrefix prefix, GTMaterial material) {
        boolean isOre = "ore".equals(prefix.getName()) || "oreSmall".equals(prefix.getName());
        BlockBehaviour.Properties properties = BlockBehaviour.Properties.of()
                .mapColor(com.gregtech.gregtech.block.BlockPrefixPresentation.mapColor(prefix))
                .strength(prefix.computeHardness(material), prefix.computeResistance(material))
                .sound(com.gregtech.gregtech.block.BlockPrefixPresentation.soundType(prefix));
        if (!prefix.falling() && !prefix.isCrate()) {
            properties = properties.requiresCorrectToolForDrops();
        }
        if (prefix.falling()) {
            return MaterialBlock.falling(properties, prefix, material);
        }
        if (isOre) {
            return new OreBlock(properties, prefix, material);
        }
        return new MaterialBlock(properties, prefix, material);
    }

    // === Stone blocks ===
    private static void registerStoneBlocks() {
        int count = 0;
        for (StoneType stoneType : StoneType.values()) {
            for (StoneVariant variant : StoneVariant.values()) {
                String blockId = stoneType.registryId() + "_" + variant.registrySuffix();
                StoneType boundType = stoneType;
                StoneVariant boundVariant = variant;
                DeferredHolder<Block, Block> block = GTBlocks.BLOCKS.register(blockId,
                        () -> new GTStoneBlock(boundType, boundVariant));
                GTBlocks.bindStone(blockId, block);
                GTBlocks.registerBlockItem(blockId, block);
                count++;
            }
        }
        LOGGER.info("Queued {} stone blocks for registration", count);
    }

    // === Stone slabs ===
    private static void registerStoneSlabs() {
        int count = 0;
        for (StoneType stoneType : StoneType.values()) {
            for (StoneVariant variant : StoneVariant.values()) {
                String blockId = stoneType.registryId() + "_" + variant.registrySuffix() + "_slab";
                StoneType boundType = stoneType;
                StoneVariant boundVariant = variant;
                DeferredHolder<Block, Block> block = GTBlocks.BLOCKS.register(blockId,
                        () -> new GTStoneSlabBlock(boundType, boundVariant));
                GTBlocks.bindStoneSlab(blockId, block);
                GTBlocks.registerStoneSlabItem(blockId, block);
                count++;
            }
        }
        LOGGER.info("Queued {} stone slab blocks for registration", count);
    }
}
