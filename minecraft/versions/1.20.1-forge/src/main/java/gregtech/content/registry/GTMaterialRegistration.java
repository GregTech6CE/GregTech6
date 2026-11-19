package gregtech.content.registry;

import gregtech.GregTech;
import gregtech.api.material.GTValues;
import gregtech.content.material.MaterialItemCompositions;
import gregtech.engine.material.ItemMaterialRegistry;
import net.minecraft.world.level.block.Block;

/** Forge-only block families after the shared item and block composition pass. */
public final class GTMaterialRegistration {
    private GTMaterialRegistration() {}

    /** Runs after block items exist and after the platform-neutral composition submissions. */
    public static void registerAll() {
        GregTech.LOGGER.info("[{}] Registering GT platform block item materials", GregTech.NAMESPACE);
        MaterialItemCompositions.registerCommon();
        int count = registerIconSetOres();
        count += registerGlowtus();
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
            } else if (MaterialItemCompositions.registerOreBlockComposition(block)) count++;
        }
        return count;
    }

    private static int registerGlowtus() {
        int count = 0;
        for (String colour : gregtech.content.worldgen.GTSurfaceFlora.GLOWTUS_COLOURS) {
            Block glowtus = gregtech.content.worldgen.GTSurfaceFlora.glowtus(colour);
            if (glowtus == null) throw new IllegalStateException("Missing glowtus_" + colour);
            ItemMaterialRegistry.register(
                    glowtus.asItem(), null, gregtech.content.material.Materials.Glowstone, GTValues.U4);
            count++;
        }
        return count;
    }
}
