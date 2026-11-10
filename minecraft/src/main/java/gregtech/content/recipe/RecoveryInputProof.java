package gregtech.content.recipe;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.GTValues;
import gregtech.api.material.MaterialComponent;
import gregtech.content.data.MaterialPrefix;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.material.ItemComposition;
import gregtech.engine.material.ItemMaterialRegistry;
import gregtech.engine.stack.StackCompat;
import java.util.List;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Runtime guard shared by every fixed-output material recovery recipe. */
record RecoveryInputProof(
        Item item, MaterialPrefix prefix, List<MaterialComponent> components, Item sourceBlock) {
    RecoveryInputProof {
        if (item == null || components == null || components.isEmpty())
            throw new IllegalArgumentException("Recovery input proof is empty");
        components = List.copyOf(components);
    }

    static RecoveryInputProof capture(ItemStack input, ItemComposition composition) {
        return composition == null
                ? null
                : capture(input, composition.prefix(), composition.components());
    }

    static RecoveryInputProof capture(ItemStack input, MaterialPrefix prefix, List<MaterialComponent> components) {
        if (input == null || input.isEmpty() || components == null || components.isEmpty()) return null;
        var proof = new RecoveryInputProof(input.getItem(), prefix, components, null);
        return proof.matches(input) ? proof : null;
    }

    static RecoveryInputProof capture(ItemStack input, GTMaterial material, MaterialPrefix prefix, long totalUnits) {
        if (input == null || input.isEmpty() || material == null || !material.isValid() || totalUnits <= 0
                || totalUnits % input.getCount() != 0) return null;
        return capture(input, prefix, List.of(MaterialComponent.of(material, totalUnits / input.getCount())));
    }

    /** Forge's one source-proven ordinary block composition has no item snapshot entry. */
    static RecoveryInputProof captureCinnabarBlock(ItemStack input, GTMaterial material) {
        if (input == null
                || input.isEmpty()
                || material == null
                || material.getId() != 9114
                || !"Cinnabar".equals(material.getName())) return null;
        var canonical = ReviewedCinnabarFacts.INSTANCE.blockDust(material, 1);
        if (canonical.isEmpty() || canonical.getItem() != input.getItem()) return null;
        var proof = new RecoveryInputProof(
                input.getItem(),
                null,
                List.of(MaterialComponent.of(material, 9 * GTValues.U)),
                input.getItem());
        return proof.matches(input) ? proof : null;
    }

    boolean matches(ItemStack offered) {
        try {
            if (offered == null || offered.isEmpty() || offered.getItem() != item) return false;
            ItemStack copy = offered.copyWithCount(1);
            if (copy.isDamaged()
                    || copy.isDamageableItem()
                    || copy.getMaxDamage() != 0
                    || ItemMaterialRegistry.hasStoredContents(copy)
                    || copy.getItem() instanceof gregtech.engine.recipe.RecipeCatalystLike
                    || copy.getItem() instanceof gregtech.engine.inventory.ContainerShapeLike
                    || copy.hasCraftingRemainingItem()
                    || !copy.getCraftingRemainingItem().isEmpty()) return false;
            var data = ItemMaterialRegistry.get(copy).orElse(null);
            if (data == null) {
                if (!sourceBlockComposition(copy)) return false;
            } else if (!ItemMaterialRegistry.canRecover(copy)
                    || data.prefix() != prefix
                    || !data.components().equals(components)) return false;
            var handler = StackCompat.fluidHandler(copy);
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

    private boolean sourceBlockComposition(ItemStack stack) {
        if (sourceBlock == null
                || stack.getItem() != sourceBlock
                || prefix != null
                || !(stack.getItem() instanceof gregtech.content.block.MaterialBlockItem item)
                || !(item.getBlock() instanceof gregtech.content.block.MaterialBlockLike block)
                || block.prefix() != gregtech.engine.prefix.BlockMaterialPrefix.blockDust) return false;
        var weights = gregtech.content.block.BlockMaterialWeights.contained(block.material(), block.prefix());
        return weights.equals(components)
                && GTMaterialRegistry.get(block.material().getId()) == block.material().resolve()
                && GTMaterialRegistry.get(block.material().getName()) == block.material().resolve();
    }
}
