package com.gregtech.gregtech.api.machine.crucible;

import com.gregtech.gregtech.api.material.ItemMaterialRegistry;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.item.MaterialItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.ArrayList;
import java.util.List;

/** Neo stack boundary for shared material/raw-ore and vanilla composition inputs. */
public final class CrucibleItemInput {
    private CrucibleItemInput() {}
    public static List<CrucibleMaterialStack> parse(ItemStack stack) {
        List<CrucibleMaterialStack> result = new ArrayList<>();
        if (stack.isEmpty() || ItemMaterialRegistry.hasStoredContents(stack)) return result;
        if (stack.getItem() instanceof com.gregtech.gregtech.api.material.MaterialFormItem item) {
            result.add(CrucibleInputRules.materialItem(item.getMaterial(), item.getPrefix()));
            return result;
        }
        if (stack.getItem() instanceof net.minecraft.world.item.BlockItem item) {
            var block = item.getBlock();
            if (block instanceof com.gregtech.gregtech.block.MaterialBlockLike materialBlock) {
                long oreCount = switch (materialBlock.prefix().getName()) {
                    case "ore" -> 1;
                    case "blockRaw" -> 9;
                    case "crateGtRaw" -> 16;
                    case "crateGt64Raw" -> 64;
                    default -> 0;
                };
                if (oreCount > 0) result.add(CrucibleInputRules.ore(materialBlock.material().resolve(), oreCount));
                else for (var weighted : com.gregtech.gregtech.block.BlockMaterialWeights.contained(
                        materialBlock.material(), materialBlock.prefix()))
                    if (weighted.material().isValid()) CrucibleMaterialStack.of(weighted.material(), weighted.amount()).addToList(result);
                return result;
            }
            if (block instanceof com.gregtech.gregtech.block.machine.SmeltingCrucibleBlock crucibleBlock) {
                var spec = crucibleBlock.spec();
                var hull = spec.material().resolve();
                if (hull.isValid()) result.add(CrucibleMaterialStack.of(hull, spec.hullMaterialUnits()));
                return result;
            }
            java.util.List<com.gregtech.gregtech.api.material.MaterialChemistry.WeightedMaterial> weights =
                    block instanceof com.gregtech.gregtech.block.stone.GTStoneBlock stone
                            ? com.gregtech.gregtech.block.stone.StoneMaterialWeights.contained(stone.stoneMaterial(), stone.variant(), false)
                    : block instanceof com.gregtech.gregtech.block.stone.GTStoneSlabBlock slab
                            ? com.gregtech.gregtech.block.stone.StoneMaterialWeights.contained(slab.stoneMaterial(), slab.variant(), true)
                    : java.util.List.of();
            if (!weights.isEmpty()) {
                for (var weighted : weights)
                    if (weighted.material().isValid()) CrucibleMaterialStack.of(weighted.material(), weighted.amount()).addToList(result);
                return result;
            }
        }
        if (!ItemMaterialRegistry.canRecover(stack)) return result;
        var item = stack.getItem();
        var raw = item == Items.RAW_IRON || item == Items.IRON_ORE || item == Items.DEEPSLATE_IRON_ORE || item == Items.RAW_IRON_BLOCK ? Materials.Iron
                : item == Items.RAW_COPPER || item == Items.COPPER_ORE || item == Items.DEEPSLATE_COPPER_ORE || item == Items.RAW_COPPER_BLOCK ? Materials.Copper
                : item == Items.RAW_GOLD || item == Items.GOLD_ORE || item == Items.DEEPSLATE_GOLD_ORE || item == Items.RAW_GOLD_BLOCK ? Materials.Gold : null;
        if (raw != null) {
            result.add(CrucibleInputRules.ore(raw, item == Items.RAW_IRON_BLOCK || item == Items.RAW_COPPER_BLOCK || item == Items.RAW_GOLD_BLOCK ? 9 : 1));
            return result;
        }
        ItemMaterialRegistry.get(stack).ifPresent(data -> {
            if (data.prefix() == MaterialPrefix.oreVanillastone || data.prefix() == MaterialPrefix.oreDense)
                result.add(CrucibleInputRules.ore(data.material().resolve(), data.prefix() == MaterialPrefix.oreDense ? 2 : 1));
            else for (var component : data.components())
                CrucibleMaterialStack.of(component.material(), component.amount()).addToList(result);
        });
        return result;
    }

    public static boolean isValid(ItemStack stack) {
        return !parse(stack).isEmpty();
    }
}
