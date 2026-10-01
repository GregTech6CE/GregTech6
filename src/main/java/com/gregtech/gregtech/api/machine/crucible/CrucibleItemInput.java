package com.gregtech.gregtech.api.machine.crucible;

import com.gregtech.gregtech.api.machine.CrucibleSpec;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTValues;
import com.gregtech.gregtech.api.material.ItemMaterialRegistry;
import com.gregtech.gregtech.api.material.MaterialChemistry;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.block.BlockMaterialWeights;
import com.gregtech.gregtech.block.MaterialBlockLike;
import com.gregtech.gregtech.block.machine.SmeltingCrucibleBlock;
import com.gregtech.gregtech.block.stone.GTStoneBlock;
import com.gregtech.gregtech.block.stone.GTStoneSlabBlock;
import com.gregtech.gregtech.block.stone.StoneMaterialWeights;
import com.gregtech.gregtech.block.stone.StoneVariant;
import com.gregtech.gregtech.item.MaterialItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Parses item stacks into crucible material payloads (GT6 {@code OM.anydata_} + ore crushing). */
public final class CrucibleItemInput {
    private CrucibleItemInput() {}

    public static List<CrucibleMaterialStack> parse(ItemStack stack) {
        List<CrucibleMaterialStack> result = new ArrayList<>();
        if (stack.isEmpty()) {
            return result;
        }

        if (stack.getItem() instanceof MaterialItem materialItem) {
            GTMaterial material = materialItem.getMaterial().resolve();
            MaterialPrefix prefix = materialItem.getPrefix();
            result.add(CrucibleInputRules.materialItem(material, prefix));
            return result;
        }

        if (stack.getItem() instanceof BlockItem blockItem) {
            Block block = blockItem.getBlock();
            if (block instanceof MaterialBlockLike materialBlock) {
                var prefix = materialBlock.prefix();
                long oreCount = switch (prefix.getName()) {
                    case "ore" -> 1;
                    case "blockRaw" -> 9;
                    case "crateGtRaw" -> 16;
                    case "crateGt64Raw" -> 64;
                    default -> 0;
                };
                if (oreCount > 0) {
                    result.add(ore(materialBlock.material().resolve(), oreCount));
                    return result;
                }
                for (MaterialChemistry.WeightedMaterial weighted
                        : BlockMaterialWeights.contained(materialBlock.material(), materialBlock.prefix())) {
                    if (weighted.material().isValid()) {
                        CrucibleMaterialStack.of(weighted.material(), weighted.amount()).addToList(result);
                    }
                }
                return result;
            }
            if (block instanceof SmeltingCrucibleBlock crucibleBlock) {
                CrucibleSpec spec = crucibleBlock.spec();
                GTMaterial hull = spec.material().resolve();
                if (hull.isValid()) {
                    result.add(CrucibleMaterialStack.of(hull, spec.hullMaterialUnits()));
                }
                return result;
            }
            if (block instanceof GTStoneBlock stoneBlock) {
                addStoneMaterials(result, stoneBlock.stoneMaterial(), stoneBlock.variant(), false);
                return result;
            }
            if (block instanceof GTStoneSlabBlock stoneSlab) {
                addStoneMaterials(result, stoneSlab.stoneMaterial(), stoneSlab.variant(), true);
                return result;
            }
        }

        // Vanilla raw items and their storage blocks use the same GT6 ore payload.
        var item = stack.getItem();
        GTMaterial vanillaOre = null;
        long count = 1;
        if (item == net.minecraft.world.item.Items.RAW_IRON || item == net.minecraft.world.item.Items.IRON_ORE
                || item == net.minecraft.world.item.Items.DEEPSLATE_IRON_ORE || item == net.minecraft.world.item.Items.RAW_IRON_BLOCK)
            vanillaOre = com.gregtech.gregtech.content.material.Materials.Iron;
        else if (item == net.minecraft.world.item.Items.RAW_COPPER || item == net.minecraft.world.item.Items.COPPER_ORE
                || item == net.minecraft.world.item.Items.DEEPSLATE_COPPER_ORE || item == net.minecraft.world.item.Items.RAW_COPPER_BLOCK)
            vanillaOre = com.gregtech.gregtech.content.material.Materials.Copper;
        else if (item == net.minecraft.world.item.Items.RAW_GOLD || item == net.minecraft.world.item.Items.GOLD_ORE
                || item == net.minecraft.world.item.Items.DEEPSLATE_GOLD_ORE || item == net.minecraft.world.item.Items.RAW_GOLD_BLOCK)
            vanillaOre = com.gregtech.gregtech.content.material.Materials.Gold;
        if (item == net.minecraft.world.item.Items.RAW_IRON_BLOCK || item == net.minecraft.world.item.Items.RAW_COPPER_BLOCK
                || item == net.minecraft.world.item.Items.RAW_GOLD_BLOCK) count = 9;
        if (vanillaOre != null && ItemMaterialRegistry.canRecover(stack)) {
            result.add(ore(vanillaOre, count)); return result;
        }

        Optional<com.gregtech.gregtech.api.material.ItemComposition> data = ItemMaterialRegistry.get(stack);
        // Both ore block forms use GT6's ore crushing target in a crucible. Vanilla-style
        // stone ores are ordinary 2U; crystal/rock ores are 4U dense ores.
        if (ItemMaterialRegistry.canRecover(stack) && data.isPresent()
                && data.get().prefix() == MaterialPrefix.oreVanillastone) {
            result.add(ore(data.get().material().resolve(), 1));
            return result;
        }
        if (ItemMaterialRegistry.canRecover(stack) && data.isPresent()
                && data.get().prefix() == MaterialPrefix.oreDense) {
            result.add(ore(data.get().material().resolve(), 2));
            return result;
        }
        if (ItemMaterialRegistry.canRecover(stack)) data.ifPresent(entry -> {
            for (var component : entry.components())
                CrucibleMaterialStack.of(component.material(), component.amount()).addToList(result);
        });
        return result;
    }

    private static CrucibleMaterialStack ore(GTMaterial material, long count) {
        return CrucibleInputRules.ore(material, count);
    }

    private static void addStoneMaterials(List<CrucibleMaterialStack> result, GTMaterial material,
                                          StoneVariant variant, boolean slab) {
        for (MaterialChemistry.WeightedMaterial weighted
                : StoneMaterialWeights.contained(material, variant, slab)) {
            if (weighted.material().isValid()) {
                CrucibleMaterialStack.of(weighted.material(), weighted.amount()).addToList(result);
            }
        }
    }

    public static boolean isValid(ItemStack stack) {
        return !parse(stack).isEmpty();
    }
}
