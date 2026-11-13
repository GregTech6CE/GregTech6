package gregtech.content.registry;

import static gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE;

import gregtech.GregTech;
import gregtech.api.material.GTMaterial;
import gregtech.api.material.GTValues;
import gregtech.content.data.MaterialPrefixes;
import gregtech.content.material.MaterialItemCompositions;
import gregtech.engine.material.ItemMaterialRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

/** Forge-only block families after the shared item and block composition pass. */
public final class GTMaterialRegistration {
    private GTMaterialRegistration() {}

    /** Runs after block items exist and after the platform-neutral composition submissions. */
    public static void registerAll() {
        GregTech.LOGGER.info("[{}] Registering GT platform block item materials", GregTech.NAMESPACE);
        MaterialItemCompositions.registerCommon();
        int count = registerIconSetOres();
        count += registerOwnedOres();
        count += registerPlants();
        GregTech.LOGGER.info("[{}] Registered {} Forge-specific block item compositions", GregTech.NAMESPACE, count);
    }

    private static int registerIconSetOres() {
        int count = 0;
        for (RegisteredBlock<Block> entry : GTIconSetBlocks.all()) {
            if (!entry.isPresent()) continue;
            Block block = entry.get();
            if (block instanceof gregtech.content.block.BlackSandBlock sand) {
                ItemMaterialRegistry.register(sand.asItem(), null, sand.material(), GTValues.U * 9);
                count++;
            } else if (block instanceof gregtech.content.block.DenseOreBlock ore) {
                GTMaterial material = ore.material();
                if (material != null && material.isValid()) {
                    ItemMaterialRegistry.register(block.asItem(), MaterialPrefixes.oreDense.getDelegate(), material);
                    count++;
                }
            } else if (block instanceof gregtech.content.block.VanillaOreBlock ore) {
                GTMaterial material = ore.material();
                if (material != null && material.isValid()) {
                    ItemMaterialRegistry.register(
                            block.asItem(), MaterialPrefixes.oreVanillastone.getDelegate(), material);
                    count++;
                }
            }
        }
        return count;
    }

    private static int registerOwnedOres() {
        int count = 0;
        for (RegisteredBlock<? extends Block> entry : GTBlocks.allEntries()) {
            if (!entry.isPresent()) continue;
            if (entry.get() instanceof gregtech.content.block.OreBlock ore && !ore.isSmall()) {
                ItemMaterialRegistry.register(
                        ore.asItem(), MaterialPrefixes.oreVanillastone.getDelegate(), ore.material());
                count++;
            }
        }
        return count;
    }

    private static int registerPlants() {
        int count = 0;
        for (String colour : gregtech.content.worldgen.GTSurfaceFlora.GLOWTUS_COLOURS) {
            Block glowtus = gregtech.content.worldgen.GTSurfaceFlora.glowtus(colour);
            if (glowtus == null) throw new IllegalStateException("Missing glowtus_" + colour);
            ItemMaterialRegistry.register(
                    glowtus.asItem(), null, gregtech.content.material.Materials.Glowstone, GTValues.U4);
            count++;
        }
        for (var flower : gregtech.content.plant.BedrockFlowers.ALL) {
            if (flower.material() == null) continue;
            Block block = ForgeRegistries.BLOCKS.getValue(
                    ResourceLocation.fromNamespaceAndPath(REGISTRY_NAMESPACE, flower.id()));
            if (block == null || block == net.minecraft.world.level.block.Blocks.AIR)
                throw new IllegalStateException("Missing GT6 bedrock flower " + flower.id());
            ItemMaterialRegistry.register(block.asItem(), null, flower.material(), GTValues.U);
            count++;
        }
        return count;
    }
}
