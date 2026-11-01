package gregtech.engine.machine.crucible;

import gregtech.api.material.GTMaterial;
import gregtech.content.data.generated.MaterialProcessingTargets;
import gregtech.content.recipe.generated.GTMaterialFields;
import gregtech.content.registry.GTBlocks;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.material.MaterialUnification;
import gregtech.engine.prefix.BlockMaterialPrefix;
import net.minecraft.world.item.ItemStack;

/** Both casting tools use the material's solidification target and the preferred registered block. */
public final class CastingMaterials {
    private CastingMaterials() {}

    public static GTMaterial solidifying(GTMaterial molten) {
        String[] target = MaterialProcessingTargets.of(molten.getName(), "Solidifying");
        if (target == null || target[0].isEmpty()) return molten;
        String name = GTMaterialFields.materialOf(target[0]);
        return name == null ? molten : GTMaterialRegistry.get(name);
    }

    public static ItemStack block(BlockMaterialPrefix prefix, GTMaterial material) {
        var vanilla = MaterialUnification.vanillaFormStack(prefix.getName(), material);
        return vanilla.isEmpty() ? GTBlocks.getStack(prefix, material) : vanilla;
    }
}
