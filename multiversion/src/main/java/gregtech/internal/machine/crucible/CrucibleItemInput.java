package gregtech.internal.machine.crucible;

//? if >=1.21.1 {
//?} else {
import gregtech.internal.machine.CrucibleSpec;
import gregtech.internal.material.GTMaterial;
import gregtech.api.material.GTValues;
//?}
import gregtech.internal.material.ItemMaterialRegistry;
//? if >=1.21.1 {
/*import gregtech.content.material.Materials;
*///?} else {
import gregtech.internal.material.MaterialChemistry;
//?}
import gregtech.content.data.MaterialPrefix;
//? if >=1.21.1 {
//?} else {
import gregtech.content.block.BlockMaterialWeights;
import gregtech.content.block.MaterialBlockLike;
import gregtech.content.block.machine.SmeltingCrucibleBlock;
import gregtech.content.block.stone.GTStoneBlock;
import gregtech.content.block.stone.GTStoneSlabBlock;
import gregtech.content.block.stone.StoneMaterialWeights;
import gregtech.content.block.stone.StoneVariant;
//?}
import gregtech.content.item.MaterialItem;
//? if >=1.21.1 {
//?} else {
import net.minecraft.world.item.BlockItem;
//?}
import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.minecraft.world.item.Items;
*///?} else {
import net.minecraft.world.level.block.Block;

//?}
import java.util.ArrayList;
import java.util.List;
//? if >=1.21.1 {
//?} else {
import java.util.Optional;
//?}

/** Parses item stacks into crucible material payloads (GT6 {@code OM.anydata_} + ore crushing). */

/** Neo stack boundary for shared material/raw-ore and vanilla composition inputs. */
public final class CrucibleItemInput {
    private CrucibleItemInput() {}

    public static List<CrucibleMaterialStack> parse(ItemStack stack) {
        List<CrucibleMaterialStack> result = new ArrayList<>();
//? if >=1.21.1 {
        /*if (stack.isEmpty() || ItemMaterialRegistry.hasStoredContents(stack)) return result;
        if (stack.getItem() instanceof gregtech.internal.material.MaterialFormItem item) {
            result.add(CrucibleInputRules.materialItem(item.getMaterial(), item.getPrefix()));
*///?} else {
        if (stack.isEmpty() || ItemMaterialRegistry.hasStoredContents(stack)) {
//?}
            return result;
        }
//? if >=1.21.1 {
        /*if (stack.getItem() instanceof net.minecraft.world.item.BlockItem item) {
            var block = item.getBlock();
            if (block instanceof gregtech.content.block.MaterialBlockLike materialBlock) {
                long oreCount = switch (materialBlock.prefix().getName()) {
*///?} else {

        if (stack.getItem() instanceof gregtech.internal.material.MaterialFormItem materialItem) {
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
//?}
                    case "ore" -> 1;
                    case "blockRaw" -> 9;
                    case "crateGtRaw" -> 16;
                    case "crateGt64Raw" -> 64;
                    default -> 0;
                };
//? if >=1.21.1 {
                /*if (oreCount > 0) result.add(CrucibleInputRules.ore(materialBlock.material().resolve(), oreCount));
                else for (var weighted : gregtech.content.block.BlockMaterialWeights.contained(
                        materialBlock.material(), materialBlock.prefix()))
                    if (weighted.material().isValid()) CrucibleMaterialStack.of(weighted.material(), weighted.amount()).addToList(result);
*///?} else {
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
//?}
                return result;
            }

//? if >=1.21.1 {
            /*java.util.List<gregtech.internal.material.MaterialChemistry.WeightedMaterial> weights =
                    block instanceof gregtech.content.block.stone.GTStoneBlock stone
                            ? gregtech.content.block.stone.StoneMaterialWeights.contained(stone.stoneMaterial(), stone.variant(), false)
                    : block instanceof gregtech.content.block.stone.GTStoneSlabBlock slab
                            ? gregtech.content.block.stone.StoneMaterialWeights.contained(slab.stoneMaterial(), slab.variant(), true)
                    : java.util.List.of();
            if (!weights.isEmpty()) {
                for (var weighted : weights)
                    if (weighted.material().isValid()) CrucibleMaterialStack.of(weighted.material(), weighted.amount()).addToList(result);
*///?} else {
            if (block instanceof GTStoneBlock stoneBlock) {
                addStoneMaterials(result, stoneBlock.stoneMaterial(), stoneBlock.variant(), false);
                return result;
            }
            if (block instanceof GTStoneSlabBlock stoneSlab) {
                addStoneMaterials(result, stoneSlab.stoneMaterial(), stoneSlab.variant(), true);
//?}
                return result;
            }
        }

        // Vanilla raw items and their storage blocks use the same GT6 ore payload.
//? if >=1.21.1 {
        /*if (!ItemMaterialRegistry.canRecover(stack)) return result;
*///?} else {
//?}
        var item = stack.getItem();
//? if >=1.21.1 {
        /*var raw = item == Items.RAW_IRON || item == Items.IRON_ORE || item == Items.DEEPSLATE_IRON_ORE || item == Items.RAW_IRON_BLOCK ? Materials.Iron
                : item == Items.RAW_COPPER || item == Items.COPPER_ORE || item == Items.DEEPSLATE_COPPER_ORE || item == Items.RAW_COPPER_BLOCK ? Materials.Copper
                : item == Items.RAW_GOLD || item == Items.GOLD_ORE || item == Items.DEEPSLATE_GOLD_ORE || item == Items.RAW_GOLD_BLOCK ? Materials.Gold : null;
        if (raw != null) {
            result.add(CrucibleInputRules.ore(raw, item == Items.RAW_IRON_BLOCK || item == Items.RAW_COPPER_BLOCK || item == Items.RAW_GOLD_BLOCK ? 9 : 1));
*///?} else {
        GTMaterial vanillaOre = null;
        long count = 1;
        if (item == net.minecraft.world.item.Items.RAW_IRON || item == net.minecraft.world.item.Items.IRON_ORE
                || item == net.minecraft.world.item.Items.DEEPSLATE_IRON_ORE || item == net.minecraft.world.item.Items.RAW_IRON_BLOCK)
            vanillaOre = gregtech.content.material.Materials.Iron;
        else if (item == net.minecraft.world.item.Items.RAW_COPPER || item == net.minecraft.world.item.Items.COPPER_ORE
                || item == net.minecraft.world.item.Items.DEEPSLATE_COPPER_ORE || item == net.minecraft.world.item.Items.RAW_COPPER_BLOCK)
            vanillaOre = gregtech.content.material.Materials.Copper;
        else if (item == net.minecraft.world.item.Items.RAW_GOLD || item == net.minecraft.world.item.Items.GOLD_ORE
                || item == net.minecraft.world.item.Items.DEEPSLATE_GOLD_ORE || item == net.minecraft.world.item.Items.RAW_GOLD_BLOCK)
            vanillaOre = gregtech.content.material.Materials.Gold;
        if (item == net.minecraft.world.item.Items.RAW_IRON_BLOCK || item == net.minecraft.world.item.Items.RAW_COPPER_BLOCK
                || item == net.minecraft.world.item.Items.RAW_GOLD_BLOCK) count = 9;
        if (vanillaOre != null && ItemMaterialRegistry.canRecover(stack)) {
            result.add(ore(vanillaOre, count)); return result;
        }

        Optional<gregtech.internal.material.ItemComposition> data = ItemMaterialRegistry.get(stack);
        // Both ore block forms use GT6's ore crushing target in a crucible. Vanilla-style
        // stone ores are ordinary 2U; crystal/rock ores are 4U dense ores.
        if (ItemMaterialRegistry.canRecover(stack) && data.isPresent()
                && data.get().prefix() == MaterialPrefix.oreVanillastone) {
            result.add(ore(data.get().material().resolve(), 1));
//?}
            return result;
        }
//? if >=1.21.1 {
        /*ItemMaterialRegistry.get(stack).ifPresent(data -> {
            if (data.prefix() == MaterialPrefix.oreVanillastone || data.prefix() == MaterialPrefix.oreDense)
                result.add(CrucibleInputRules.ore(data.material().resolve(), data.prefix() == MaterialPrefix.oreDense ? 2 : 1));
            else for (var component : data.components())
*///?} else {
        if (ItemMaterialRegistry.canRecover(stack) && data.isPresent()
                && data.get().prefix() == MaterialPrefix.oreDense) {
            result.add(ore(data.get().material().resolve(), 2));
            return result;
        }
        if (ItemMaterialRegistry.canRecover(stack)) data.ifPresent(entry -> {
            for (var component : entry.components())
//?}
                CrucibleMaterialStack.of(component.material(), component.amount()).addToList(result);
        });
        return result;
//? if >=1.21.1 {
//?} else {
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
//?}
    }

    public static boolean isValid(ItemStack stack) {
        return !parse(stack).isEmpty();
    }
}
