package gregtech.content.worldgen;

import gregtech.GregTechConfig;
import net.minecraft.resources.ResourceLocation;

/** Shared by vanilla features, vanilla ore veins and explicit vanilla outputs of GT stone layers. */
public final class VanillaOreGeneration {
    private VanillaOreGeneration() {}

    /** Vanilla's large copper and iron veins are written by {@code OreVeinifier}, not by an ore feature. */
    public static boolean veinsDisabled() {
        return GregTechConfig.Rule.DISABLE_VANILLA_ORES.enabled();
    }

    public static boolean disabled(ResourceLocation id) {
        if (!id.getNamespace().equals("minecraft")) return false;
        return switch (id.getPath()) {
            case "granite", "diorite", "andesite" -> GregTechConfig.Rule.DISABLE_VANILLA_DECORATIVE_STONE.enabled();
            case "nether_gold_ore" -> !GregTechConfig.Rule.GENERATE_NETHER_GOLD_ORE.enabled();
            case "ancient_debris" -> !GregTechConfig.Rule.GENERATE_ANCIENT_DEBRIS.enabled();
            default -> id.getPath().endsWith("_ore") && GregTechConfig.Rule.DISABLE_VANILLA_ORES.enabled();
        };
    }
}
