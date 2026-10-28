package gregtech.engine.sensor;

import gregtech.api.material.WeightedMaterial;
import gregtech.content.block.BlockMaterialWeights;
import gregtech.content.block.MaterialBlockLike;
import gregtech.content.block.stone.GTStoneBlock;
import gregtech.content.block.stone.GTStoneSlabBlock;
import gregtech.content.block.stone.StoneMaterialWeights;
import gregtech.engine.material.ItemMaterialRegistry;
import gregtech.engine.material.MaterialMass;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

/** Physical item composition, never the amount an ore-processing or crucible recipe recovers. */
public final class PhysicalItemMass {
    private PhysicalItemMass() {}

    /** Includes the stack count; an explicitly registered empty composition remains weightless. */
    public static double kilograms(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        var composition = ItemMaterialRegistry.get(stack);
        if (composition.isPresent()) {
            double kilograms = 0;
            for (var component : composition.get().components())
                kilograms += MaterialMass.kilograms(component.material(), component.amount());
            return kilograms * stack.getCount();
        }
        // Some native block registrations have no item-composition entry. Preserve their
        // physical components without applying ore crushing multipliers or recovery vetoes.
        java.util.List<WeightedMaterial> components = java.util.List.of();
        if (stack.getItem() instanceof BlockItem item) {
            var block = item.getBlock();
            if (block instanceof MaterialBlockLike material)
                components = BlockMaterialWeights.contained(material.material(), material.prefix());
            else if (block instanceof GTStoneBlock stone)
                components = StoneMaterialWeights.contained(stone.stoneMaterial(), stone.variant(), false);
            else if (block instanceof GTStoneSlabBlock slab)
                components = StoneMaterialWeights.contained(slab.stoneMaterial(), slab.variant(), true);
        }
        double kilograms = 0;
        for (var component : components)
            if (component.material().isValid())
                kilograms += MaterialMass.kilograms(component.material(), component.amount());
        return kilograms * stack.getCount();
    }
}
