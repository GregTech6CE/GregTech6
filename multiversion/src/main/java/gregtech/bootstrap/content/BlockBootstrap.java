package gregtech.bootstrap.content;

import gregtech.content.material.Materials;
import gregtech.internal.material.GTMaterial;
import gregtech.internal.material.GTMaterialRegistry;
import gregtech.internal.material.MaterialProperty;
import gregtech.internal.prefix.BlockMaterialPrefix;
import gregtech.internal.prefix.BlockPrefixRegistry;
import gregtech.content.block.MaterialBlock;
import gregtech.content.block.OreBlock;
import gregtech.content.block.stone.GTStoneBlock;
import gregtech.content.block.stone.GTStoneSlabBlock;
import gregtech.content.block.stone.StoneType;
import gregtech.content.block.stone.StoneVariant;
//? if >=1.21.1 {
//?} else {
import gregtech.bootstrap.InitializationStep;
//?}
import gregtech.content.registry.GTBlocks;
//? if >=1.21.1 {
/*import gregtech.content.registry.GTSpecialOreBlocks;
*///?} else {
import gregtech.content.registry.GTIconSetBlocks;
//?}
import com.mojang.logging.LogUtils;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.registries.DeferredHolder;
*///?} else {
import net.minecraftforge.registries.RegistryObject;
//?}
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Set;

/** Registers GT6 material blocks, stone blocks, and stone slabs — logic inlined from three separate loader classes. */
//? if >=1.21.1 {
/*public record BlockBootstrap() implements Runnable {
*///?} else {
public record BlockBootstrap() implements InitializationStep {
//?}
    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void run() {
        gregtech.content.registry.GTLegacyEnergyDevices.registerAll();
        gregtech.content.registry.GTItemBarrels.registerAll();
        registerMaterialBlocks();
        registerStoneBlocks();
        registerStoneSlabs();
        registerIconSetBlocks();
//? if >=1.21.1 {
        /*gregtech.content.registry.GTMultiblocks.registerAll();
*///?} else {
//?}
    }

    private static void registerIconSetBlocks() {
//? if >=1.21.1 {
        /*GTSpecialOreBlocks.registerAll();
        gregtech.content.registry.GTPlants.registerAll();
        LOGGER.info("Queued {} icon set blocks for registration", GTSpecialOreBlocks.all().size());
*///?} else {
        GTIconSetBlocks.registerAll();
        LOGGER.info("Queued {} icon set blocks for registration", GTIconSetBlocks.all().size());
//?}
    }

    // === Material blocks ===
    private static void registerMaterialBlocks() {
        BlockPrefixRegistry.ensurePrefixesLoaded();
        Set<String> seenIds = new HashSet<>();
        int count = 0;

        for (var definition : gregtech.internal.material.MaterialBlockDefinitions.all()) {
            GTMaterial material = definition.material();
            BlockMaterialPrefix prefix = definition.prefix();
            String blockId = definition.blockId();
            if (!blockId.equals(definition.baseBlockId())) {
                LOGGER.warn("Duplicate block id '{}' for material {} (id={}); using '{}'",
                        definition.baseBlockId(), material.getName(), material.getId(), blockId);
            }
//? if >=1.21.1 {
            /*DeferredHolder<Block, Block> block = GTBlocks.BLOCKS.register(blockId, () -> createBlock(prefix, material));
*///?} else {
            RegistryObject<Block> block = GTBlocks.BLOCKS.register(blockId, () -> createBlock(prefix, material));
//?}
            GTBlocks.bind(prefix, material, block);
            GTBlocks.registerBlockItem(blockId, block);
            count++;
        }
        LOGGER.info("Queued {} material blocks for registration", count);
    }

    private static Block createBlock(BlockMaterialPrefix prefix, GTMaterial material) {
        boolean isOre = "ore".equals(prefix.getName()) || "oreSmall".equals(prefix.getName());
        BlockBehaviour.Properties properties = BlockBehaviour.Properties.of()
                .mapColor(gregtech.content.block.BlockPrefixPresentation.mapColor(prefix))
                .strength(prefix.computeHardness(material), prefix.computeResistance(material))
                .sound(gregtech.content.block.BlockPrefixPresentation.soundType(prefix));
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
//? if >=1.21.1 {
                /*DeferredHolder<Block, Block> block = GTBlocks.BLOCKS.register(blockId,
*///?} else {
                RegistryObject<Block> block = GTBlocks.BLOCKS.register(blockId,
//?}
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
//? if >=1.21.1 {
                /*DeferredHolder<Block, Block> block = GTBlocks.BLOCKS.register(blockId,
*///?} else {
                RegistryObject<Block> block = GTBlocks.BLOCKS.register(blockId,
//?}
                        () -> new GTStoneSlabBlock(boundType, boundVariant));
                GTBlocks.bindStoneSlab(blockId, block);
                GTBlocks.registerStoneSlabItem(blockId, block);
                count++;
            }
        }
        LOGGER.info("Queued {} stone slab blocks for registration", count);
    }
}
