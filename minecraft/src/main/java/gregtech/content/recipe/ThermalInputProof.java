package gregtech.content.recipe;

import gregtech.api.material.GTMaterial;
import gregtech.content.data.MaterialPrefix;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.material.ItemMaterialRegistry;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidUtil;
*///?} else {
import net.minecraftforge.fluids.FluidUtil;
//?}

/** Per-item composition captured before recipe normalization; normal ingredient aliases remain valid. */
record ThermalInputProof(GTMaterial material, MaterialPrefix prefix, long units, net.minecraft.world.item.Item sourceBlock) {
    static ThermalInputProof capture(ItemStack input, GTMaterial material, MaterialPrefix prefix, long totalUnits) {
        if (input == null || input.isEmpty() || totalUnits <= 0 || totalUnits % input.getCount() != 0) return null;
        var proof = new ThermalInputProof(material, prefix, totalUnits / input.getCount(), null);
        return proof.matches(input) ? proof : null;
    }

    /** Forge does not register ordinary block compositions; this one source-proven holder is not an addon fallback. */
    static ThermalInputProof captureCinnabarBlock(ItemStack input, GTMaterial material) {
        if (input == null || input.isEmpty() || material == null
                || material.getId() != 9114 || !material.getName().equals("Cinnabar")) return null;
        var canonical = ReviewedCinnabarFacts.INSTANCE.blockDust(material, 1);
        if (canonical.isEmpty() || canonical.getItem() != input.getItem()) return null;
        var proof = new ThermalInputProof(material, null, 9 * gregtech.api.material.GTValues.U, input.getItem());
        return proof.matches(input) ? proof : null;
    }

    private boolean sourceBlockComposition(ItemStack stack) {
        if (sourceBlock == null || stack.getItem() != sourceBlock || prefix != null
                || !(stack.getItem() instanceof gregtech.content.block.MaterialBlockItem item)
                || !(item.getBlock() instanceof gregtech.content.block.MaterialBlockLike block)
                || block.prefix() != gregtech.engine.prefix.BlockMaterialPrefix.blockDust
                || block.material().resolve() != material) return false;
        var canonical = ReviewedCinnabarFacts.INSTANCE.blockDust(material, 1);
        if (canonical.isEmpty() || canonical.getItem() != sourceBlock) return false;
        var weights = gregtech.content.block.BlockMaterialWeights.contained(block.material(), block.prefix());
        return weights.size() == 1 && weights.get(0).material().resolve() == material
                && weights.get(0).amount() == units && units == 9 * gregtech.api.material.GTValues.U;
    }

    boolean matches(ItemStack offered) {
        try {
            if (offered == null || offered.isEmpty() || units <= 0 || material == null
                    || !material.isValid() || material.resolve() != material
                    || GTMaterialRegistry.get(material.getId()) != material
                    || GTMaterialRegistry.get(material.getName()) != material) return false;
            // Capability and remainder implementations may mutate their argument. Never hand them inventory stacks.
            ItemStack copy = offered.copy();
            if (copy.isDamaged() || copy.isDamageableItem() || copy.getMaxDamage() != 0
                    || ItemMaterialRegistry.hasStoredContents(copy)
                    || copy.getItem() instanceof gregtech.engine.recipe.RecipeCatalystLike
                    || copy.getItem() instanceof gregtech.engine.inventory.ContainerShapeLike
                    || copy.hasCraftingRemainingItem() || !copy.getCraftingRemainingItem().isEmpty()) return false;
            var data = ItemMaterialRegistry.get(copy).orElse(null);
            if (data == null) {
                if (!sourceBlockComposition(copy)) return false;
            } else if (!ItemMaterialRegistry.canRecover(copy) || data.prefix() != prefix
                    || data.components().size() != 1 || data.material().resolve() != material
                    || data.amount() != units) return false;
//? if >=1.21.1 {
            /*var handler = FluidUtil.getFluidHandler(copy).orElse(null);
*///?} else {
            var handler = FluidUtil.getFluidHandler(copy).resolve().orElse(null);
//?}
            if (handler != null) {
                int tanks = handler.getTanks();
                if (tanks < 0) return false;
                for (int tank = 0; tank < tanks; tank++)
                    if (handler.getTankCapacity(tank) != 0) return false;
            }
            return true;
        } catch (RuntimeException | LinkageError invalidBehavior) {
            return false;
        }
    }
}
