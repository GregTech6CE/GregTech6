package gregtech.content.recipe;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.MaterialComponent;
import gregtech.content.data.MaterialPrefix;
import gregtech.engine.material.ItemComposition;
import gregtech.engine.material.ItemMaterialRegistry;
import gregtech.engine.stack.StackCompat;
import java.util.List;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Runtime guard shared by every fixed-output material recovery recipe. */
record RecoveryInputProof(Item item, MaterialPrefix prefix, List<MaterialComponent> components) {
    RecoveryInputProof {
        if (item == null || components == null || components.isEmpty())
            throw new IllegalArgumentException("Recovery input proof is empty");
        components = List.copyOf(components);
    }

    static RecoveryInputProof capture(ItemStack input, ItemComposition composition) {
        return composition == null ? null : capture(input, composition.prefix(), composition.components());
    }

    static RecoveryInputProof capture(ItemStack input, MaterialPrefix prefix, List<MaterialComponent> components) {
        if (input == null || input.isEmpty() || components == null || components.isEmpty()) return null;
        var proof = new RecoveryInputProof(input.getItem(), prefix, components);
        return proof.matches(input) ? proof : null;
    }

    static RecoveryInputProof capture(ItemStack input, GTMaterial material, MaterialPrefix prefix, long totalUnits) {
        if (input == null
                || input.isEmpty()
                || material == null
                || !material.isValid()
                || totalUnits <= 0
                || totalUnits % input.getCount() != 0) return null;
        return capture(input, prefix, List.of(MaterialComponent.of(material, totalUnits / input.getCount())));
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
            if (data == null) return false;
            if (!ItemMaterialRegistry.canRecover(copy)
                    || data.prefix() != prefix
                    || !data.components().equals(components)) return false;
            var handler = StackCompat.fluidHandler(copy);
            if (handler != null) {
                int tanks = handler.getTanks();
                if (tanks < 0) return false;
                for (int tank = 0; tank < tanks; tank++) if (handler.getTankCapacity(tank) != 0) return false;
            }
            return true;
        } catch (RuntimeException | LinkageError invalidBehavior) {
            return false;
        }
    }
}
