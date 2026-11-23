package gregtech.engine.machine.crucible;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.MaterialComponent;
import gregtech.content.block.BlockMaterialWeights;
import gregtech.content.block.MaterialBlockLike;
import gregtech.content.block.stone.GTStoneBlock;
import gregtech.content.block.stone.GTStoneSlabBlock;
import gregtech.content.block.stone.StoneMaterialWeights;
import gregtech.content.block.stone.StoneVariant;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.material.Materials;
import gregtech.engine.material.ItemMaterialRegistry;
import gregtech.engine.material.MaterialFormItem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

/** Parses item stacks into crucible material payloads (GT6 {@code OM.anydata_} + ore crushing).
 * Admission is material data, not recovery authority: rows marked descriptive for recycling
 * ({@code recoverable=false}) still melt exactly as their tooltip composition says.
 */

/** Neo stack boundary for shared material/raw-ore and vanilla composition inputs. */
public final class CrucibleItemInput {
    private CrucibleItemInput() {}

    public static List<CrucibleMaterialStack> parse(ItemStack stack) {
        List<CrucibleMaterialStack> result = new ArrayList<>();
        if (stack.isEmpty() || ItemMaterialRegistry.hasStoredContents(stack)) return result;
        if (stack.getItem() instanceof MaterialFormItem item) {
            addMaterialForm(result, stack, item);
            return result;
        }
        if (stack.getItem() instanceof BlockItem item) {
            Block block = item.getBlock();
            if (block instanceof MaterialBlockLike materialBlock) {
                long oreCount = switch (materialBlock.prefix().getName()) {
                    case "ore" -> 1;
                    case "blockRaw" -> 9;
                    default -> 0;
                };
                if (oreCount > 0) {
                    result.add(ore(materialBlock.material().resolve(), oreCount));
                } else {
                    for (MaterialComponent weighted :
                            BlockMaterialWeights.contained(materialBlock.material(), materialBlock.prefix())) {
                        if (weighted.material().isValid()) {
                            CrucibleMaterialStack.of(weighted.material(), weighted.amount())
                                    .addToList(result);
                        }
                    }
                }
                return result;
            }
            if (block instanceof GTStoneBlock stone) {
                addStoneMaterials(result, stone.stoneMaterial(), stone.variant(), false);
                return result;
            }
            if (block instanceof GTStoneSlabBlock slab) {
                addStoneMaterials(result, slab.stoneMaterial(), slab.variant(), true);
                return result;
            }
        }

        // Vanilla raw items and their storage blocks use the same GT6 ore payload.
        if (!ItemMaterialRegistry.canMelt(stack)) return result;
        var item = stack.getItem();
        GTMaterial raw = rawOreMaterial(item);
        if (raw != null) {
            boolean storageBlock =
                    item == Items.RAW_IRON_BLOCK || item == Items.RAW_COPPER_BLOCK || item == Items.RAW_GOLD_BLOCK;
            result.add(ore(raw, storageBlock ? 9 : 1));
            return result;
        }

        var data = ItemMaterialRegistry.get(stack).orElse(null);
        if (data == null) return result;
        // Both ore block forms use GT6's ore crushing target in a crucible. Vanilla-style
        // stone ores are ordinary 2U; crystal/rock ores are 4U dense ores.
        if (data.prefix() == MaterialPrefix.oreVanillastone) {
            result.add(ore(data.material().resolve(), 1));
        } else if (data.prefix() == MaterialPrefix.oreDense) {
            result.add(ore(data.material().resolve(), 2));
        } else {
            for (var component : data.components())
                CrucibleMaterialStack.of(component.material(), component.amount())
                        .addToList(result);
        }
        return result;
    }

    private static CrucibleMaterialStack ore(GTMaterial material, long count) {
        return CrucibleInputRules.ore(material, count);
    }

    /** GT6's vanilla ore payload: the raw item, ore block and storage block of iron, copper and gold. */
    private static GTMaterial rawOreMaterial(Item item) {
        if (item == Items.RAW_IRON
                || item == Items.IRON_ORE
                || item == Items.DEEPSLATE_IRON_ORE
                || item == Items.RAW_IRON_BLOCK) return Materials.Iron;
        if (item == Items.RAW_COPPER
                || item == Items.COPPER_ORE
                || item == Items.DEEPSLATE_COPPER_ORE
                || item == Items.RAW_COPPER_BLOCK) return Materials.Copper;
        if (item == Items.RAW_GOLD
                || item == Items.GOLD_ORE
                || item == Items.DEEPSLATE_GOLD_ORE
                || item == Items.RAW_GOLD_BLOCK) return Materials.Gold;
        return null;
    }

    private static void addStoneMaterials(
            List<CrucibleMaterialStack> result, GTMaterial material, StoneVariant variant, boolean slab) {
        for (MaterialComponent weighted : StoneMaterialWeights.contained(material, variant, slab)) {
            if (weighted.material().isValid()) {
                CrucibleMaterialStack.of(weighted.material(), weighted.amount()).addToList(result);
            }
        }
    }

    /** Source MultiTileEntitySmeltery159-182 keeps raw-ore crushing separate, then admits every
     * positive component of ordinary item data. Use the actual composition (including overrides),
     * not the nominal prefix amount: Empty ammunition still has a valid shaft or casing.
     */
    private static void addMaterialForm(List<CrucibleMaterialStack> result, ItemStack stack, MaterialFormItem form) {
        if (form.getPrefix() == MaterialPrefix.oreRaw) {
            result.add(CrucibleInputRules.materialItem(form.getMaterial(), form.getPrefix()));
            return;
        }
        // Missing data must not fall back to a fabricated primary component.
        if (!ItemMaterialRegistry.canMelt(stack)) return;
        ItemMaterialRegistry.get(stack).ifPresent(data -> {
            for (var component : data.components())
                CrucibleMaterialStack.of(component.material(), component.amount())
                        .addToList(result);
        });
    }

    public static boolean isValid(ItemStack stack) {
        return !parse(stack).isEmpty();
    }
}
