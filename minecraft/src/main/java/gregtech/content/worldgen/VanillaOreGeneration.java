package gregtech.content.worldgen;

import gregtech.GregTechConfig;
import net.minecraft.resources.ResourceLocation;

/** Shared by vanilla features and explicit vanilla outputs of GT stone layers. */
public final class VanillaOreGeneration {
    private VanillaOreGeneration() {}
    public static boolean disabled(ResourceLocation id) {
        if (!id.getNamespace().equals("minecraft")) return false;
        return switch (id.getPath()) {
            case "nether_gold_ore" -> !GregTechConfig.Rule.GENERATE_NETHER_GOLD_ORE.enabled();
            case "ancient_debris" -> !GregTechConfig.Rule.GENERATE_ANCIENT_DEBRIS.enabled();
            default -> id.getPath().endsWith("_ore") && GregTechConfig.Rule.DISABLE_VANILLA_ORES.enabled();
        };
    }
}
