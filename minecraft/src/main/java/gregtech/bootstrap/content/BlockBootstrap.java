package gregtech.bootstrap.content;

import static gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE;

import com.mojang.logging.LogUtils;
import gregtech.api.material.GTMaterial;
import gregtech.content.block.MaterialBlock;
import gregtech.content.block.OreBlock;
import gregtech.content.block.stone.GTStoneBlock;
import gregtech.content.block.stone.GTStoneSlabBlock;
import gregtech.content.block.stone.StoneType;
import gregtech.content.block.stone.StoneVariant;
import gregtech.content.registry.ContentRegistrar;
import gregtech.content.registry.GTBlocks;
import gregtech.engine.prefix.BlockMaterialPrefix;
import gregtech.engine.prefix.BlockPrefixRegistry;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.slf4j.Logger;

/** Queues GT6 material blocks, stone blocks, and stone slabs — logic inlined from three separate loader classes. */
public final class BlockBootstrap {
    private static final Logger LOGGER = LogUtils.getLogger();

    private BlockBootstrap() {}

    public static void register(ContentRegistrar registrar) {
        gregtech.content.registry.GTLegacyEnergyDevices.registerAll(registrar);
        gregtech.content.registry.GTItemBarrels.registerAll(registrar);
        registerMaterialBlocks(registrar);
        registerStoneBlocks(registrar);
        registerStoneSlabs(registrar);
    }

    // === Material blocks ===
    private static void registerMaterialBlocks(ContentRegistrar registrar) {
        BlockPrefixRegistry.ensurePrefixesLoaded();
        Set<String> seenIds = new HashSet<>();
        int count = 0;

        for (var definition : gregtech.engine.material.MaterialBlockDefinitions.all()) {
            GTMaterial material = definition.material();
            BlockMaterialPrefix prefix = definition.prefix();
            String blockId = definition.blockId();
            if (!blockId.equals(definition.baseBlockId())) {
                LOGGER.warn(
                        "Duplicate block id '{}' for material {} (id={}); using '{}'",
                        definition.baseBlockId(),
                        material.getName(),
                        material.getId(),
                        blockId);
            }
            var block = registrar.block(
                    REGISTRY_NAMESPACE, blockId, () -> createBlock(prefix, material), GTBlocks::materialBlockItem);
            GTBlocks.bind(prefix, material, block);
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
        if (!prefix.falling()) {
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
    private static void registerStoneBlocks(ContentRegistrar registrar) {
        int count = 0;
        for (StoneType stoneType : StoneType.values()) {
            for (StoneVariant variant : StoneVariant.values()) {
                String blockId = stoneType.registryId() + "_" + variant.registrySuffix();
                StoneType boundType = stoneType;
                StoneVariant boundVariant = variant;
                var block = registrar.block(
                        REGISTRY_NAMESPACE,
                        blockId,
                        () -> new GTStoneBlock(boundType, boundVariant),
                        GTBlocks::materialBlockItem);
                GTBlocks.bindStone(blockId, block);
                count++;
            }
        }
        LOGGER.info("Queued {} stone blocks for registration", count);
    }

    // === Stone slabs ===
    private static void registerStoneSlabs(ContentRegistrar registrar) {
        int count = 0;
        for (StoneType stoneType : StoneType.values()) {
            for (StoneVariant variant : StoneVariant.values()) {
                String blockId = stoneType.registryId() + "_" + variant.registrySuffix() + "_slab";
                StoneType boundType = stoneType;
                StoneVariant boundVariant = variant;
                var block = registrar.block(
                        REGISTRY_NAMESPACE,
                        blockId,
                        () -> new GTStoneSlabBlock(boundType, boundVariant),
                        GTBlocks::stoneSlabItem);
                GTBlocks.bindStoneSlab(blockId, block);
                count++;
            }
        }
        LOGGER.info("Queued {} stone slab blocks for registration", count);
    }
}
