package com.gregtech.gregtech.api.inventory;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTValues;
import com.gregtech.gregtech.api.material.ItemMaterialRegistry;
import com.gregtech.gregtech.api.material.MaterialEquivalence;
import com.gregtech.gregtech.block.MaterialBlockLike;
import com.gregtech.gregtech.block.energy.ElectricWireBlock;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.item.MaterialItem;
import com.gregtech.gregtech.registry.GTItems;
import com.gregtech.gregtech.registry.GTWires;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

/** The deliberately narrow GT6 Mass Storage form families, not every form of one material. */
public final class MassStorageMaterialForms {
    private enum Family { DUST, INGOT, WIRE, GEM, PLATE, GEM_PLATE, CRUSHED,
        CRUSHED_PURIFIED, CRUSHED_CENTRIFUGED, RAW }

    private record Form(Family family, GTMaterial material, long units) {}

    private MassStorageMaterialForms() {}

    /** Material units represented by one candidate item, or zero when GT6 would reject it. */
    public static long compatibleUnits(ItemStack template, ItemStack candidate) {
        return compatibleUnits(template, candidate, false);
    }

    private static long compatibleUnits(ItemStack template, ItemStack candidate, boolean ignoreNbt) {
        if (template.isEmpty() || candidate.isEmpty()
                || (!ignoreNbt && (template.hasTag() || candidate.hasTag()))
                || template.isDamaged() || candidate.isDamaged()) return 0;
        Form stored = form(template);
        if (stored == null) return 0;
        Form offered = form(candidate);
        if (offered != null) return stored.family == offered.family
                && stored.material == offered.material ? offered.units : 0;
        return taggedUnits(stored, candidate);
    }

    /** The stored form's own weight. Unknown items still support exact-item bulk storage. */
    public static long templateUnits(ItemStack template) {
        Form form = form(template);
        return form == null ? 0 : form.units;
    }

    /**
     * Represent a Mass Storage's sub-template remainder as real items on harvest.
     * GT6's {@code getPartialStack} chooses forms from the stored material family;
     * this version may return several stacks so a large block-form remainder does
     * not overflow a single 1.20.1 ItemStack. Any sub-smallest-form residue is not
     * representable by an item, just as in GT6.
     */
    public static List<ItemStack> partialDrops(ItemStack template, long units) {
        Form form = form(template);
        if (form == null || units <= 0) return List.of();
        List<ItemStack> drops = new ArrayList<>();
        if (form.family == Family.WIRE) {
            for (var registered : GTWires.allWires()) {
                ElectricWireBlock wire = registered.get();
                if (wire.spec() != null && wire.spec().size() == 1
                        && wire.spec().material().resolve() == form.material) {
                    appendPartial(drops, template, new ItemStack(wire), units);
                    break;
                }
            }
            return drops;
        }
        MaterialPrefix[] candidates = switch (form.family) {
            case DUST -> new MaterialPrefix[]{MaterialPrefix.dust, MaterialPrefix.dustSmall,
                    MaterialPrefix.dustTiny, MaterialPrefix.dustDiv72};
            // OM.ingot chooses the 1/4-unit chunk before nuggets only when it
            // leaves no more residue than 1/9-unit nuggets. For example 1/4 U
            // must become one chunk, while 8/9 U must become eight nuggets.
            case INGOT -> units % GTValues.U == 0
                    ? new MaterialPrefix[]{MaterialPrefix.ingot, MaterialPrefix.chunkGt,
                            MaterialPrefix.nugget}
                    : units % GTValues.U4 <= units % GTValues.U9
                    ? new MaterialPrefix[]{MaterialPrefix.chunkGt, MaterialPrefix.nugget,
                            MaterialPrefix.ingot}
                    : new MaterialPrefix[]{MaterialPrefix.nugget, MaterialPrefix.chunkGt,
                            MaterialPrefix.ingot};
            case GEM -> new MaterialPrefix[]{MaterialPrefix.gem};
            case PLATE -> new MaterialPrefix[]{MaterialPrefix.plate};
            case GEM_PLATE -> new MaterialPrefix[]{MaterialPrefix.plateGem};
            case CRUSHED -> new MaterialPrefix[]{MaterialPrefix.crushedTiny};
            case CRUSHED_PURIFIED -> new MaterialPrefix[]{MaterialPrefix.crushedPurifiedTiny};
            case CRUSHED_CENTRIFUGED -> new MaterialPrefix[]{MaterialPrefix.crushedCentrifugedTiny};
            case RAW -> new MaterialPrefix[]{MaterialPrefix.oreRaw};
            case WIRE -> throw new IllegalStateException("wire handled above");
        };
        long remaining = units;
        for (MaterialPrefix prefix : candidates) {
            ItemStack candidate = GTItems.getStack(prefix, form.material);
            remaining -= appendPartial(drops, template, candidate, remaining);
        }
        return drops;
    }

    private static long appendPartial(List<ItemStack> drops, ItemStack template,
                                      ItemStack candidate, long remaining) {
        long perItem = compatibleUnits(template, candidate);
        if (candidate.isEmpty() || perItem <= 0 || remaining < perItem) return 0;
        long count = remaining / perItem;
        long used = count * perItem;
        int maxStack = Math.max(1, candidate.getMaxStackSize());
        while (count > 0) {
            int batch = (int) Math.min(count, maxStack);
            drops.add(candidate.copyWithCount(batch));
            count -= batch;
        }
        return used;
    }

    /** Dump reservations ignore NBT as GT6's ItemStackSet does, but keep family boundaries. */
    public static boolean reserves(ItemStack template, ItemStack candidate) {
        return !template.isEmpty() && !candidate.isEmpty()
                && (template.is(candidate.getItem())
                    && template.getDamageValue() == candidate.getDamageValue()
                    || compatibleUnits(template, candidate, true) > 0);
    }

    private static Form form(ItemStack stack) {
        if (stack.isEmpty()) return null;
        if (stack.getItem() instanceof com.gregtech.gregtech.api.material.MaterialFormItem item)
            return form(item.getPrefix().getName(), item.getMaterial().resolve(),
                    item.getPrefix().getMaterialWeight());
        if (stack.getItem() instanceof BlockItem blockItem) {
            var block = blockItem.getBlock();
            if (block instanceof MaterialBlockLike materialBlock)
                return form(materialBlock.prefix().getName(), materialBlock.material().resolve(),
                        materialBlock.prefix().getMaterialWeight());
            if (block instanceof ElectricWireBlock wire && wire.spec() != null
                    && !wire.spec().insulated() && wire.spec().size() >= 1 && wire.spec().size() <= 16)
                return new Form(Family.WIRE, wire.spec().material().resolve(), wire.spec().materialAmount());
            Form vanillaBlock = vanillaBlock(block, stack);
            if (vanillaBlock != null) return vanillaBlock;
        }
        var data = ItemMaterialRegistry.base(stack.getItem()).orElse(null);
        if (data == null || data.prefix() == null || data.components().size() != 1
                || data.amount() != data.prefix().getMaterialWeight()) return null;
        return form(data.prefix().getName(), data.material().resolve(), data.amount());
    }

    private static Form vanillaBlock(net.minecraft.world.level.block.Block block, ItemStack stack) {
        Family family;
        long units = GTValues.U * 9;
        if (block == Blocks.IRON_BLOCK || block == Blocks.GOLD_BLOCK
                || block == Blocks.COPPER_BLOCK || block == Blocks.NETHERITE_BLOCK) family = Family.INGOT;
        else if (block == Blocks.DIAMOND_BLOCK || block == Blocks.EMERALD_BLOCK
                || block == Blocks.LAPIS_BLOCK || block == Blocks.COAL_BLOCK) family = Family.GEM;
        else if (block == Blocks.REDSTONE_BLOCK) family = Family.DUST;
        else if (block == Blocks.RAW_IRON_BLOCK || block == Blocks.RAW_GOLD_BLOCK
                || block == Blocks.RAW_COPPER_BLOCK) family = Family.RAW;
        else return null;
        var data = ItemMaterialRegistry.base(stack.getItem()).orElse(null);
        if (data == null || data.components().size() != 1) return null;
        GTMaterial material = data.material().resolve();
        return material.isValid() ? new Form(family, material, units) : null;
    }

    private static Form form(String prefix, GTMaterial material, long units) {
        if (material == null || !material.isValid() || units <= 0) return null;
        // GT6 OP.wireGt01..16 are one WIRE_BASED family. In this port they are
        // normally physical ElectricWireBlock items, but retain the prefix path
        // for material bindings supplied by another registration.
        if (prefix.length() == 8 && prefix.startsWith("wireGt")
                && Character.isDigit(prefix.charAt(6)) && Character.isDigit(prefix.charAt(7))) {
            int size = (prefix.charAt(6) - '0') * 10 + prefix.charAt(7) - '0';
            return size >= 1 && size <= 16
                    ? new Form(Family.WIRE, material, size * GTValues.U2) : null;
        }
        Family family = switch (prefix) {
            case "dust", "dustSmall", "dustTiny", "dustDiv72", "blockDust" -> Family.DUST;
            case "ingot", "nugget", "billet", "chunkGt", "blockIngot" -> Family.INGOT;
            case "gem", "blockGem" -> Family.GEM;
            case "plate", "blockPlate" -> Family.PLATE;
            case "plateGem", "blockPlateGem" -> Family.GEM_PLATE;
            case "crushed", "crushedTiny" -> Family.CRUSHED;
            case "crushedPurified", "crushedPurifiedTiny" -> Family.CRUSHED_PURIFIED;
            case "crushedCentrifuged", "crushedCentrifugedTiny" -> Family.CRUSHED_CENTRIFUGED;
            case "oreRaw", "blockRaw" -> Family.RAW;
            default -> null;
        };
        // GT6 Mass Storage intentionally counts raw ore as U, unlike the port's
        // general ore-processing material weights of 2U and 18U.
        if (prefix.equals("oreRaw")) units = GTValues.U;
        else if (prefix.equals("blockRaw")) units = GTValues.U * 9;
        return family == null ? null : new Form(family, material, units);
    }

    private static long taggedUnits(Form stored, ItemStack candidate) {
        // Forge's form-specific tags admit another mod's equivalent item. Broad
        // gregtech:material/<name> and forge:storage_blocks tags are ambiguous.
        String material = MaterialEquivalence.materialName(stored.material);
        return switch (stored.family) {
            case DUST -> tagged(candidate, "dusts", material) ? GTValues.U
                    : tagged(candidate, "small_dusts", material) ? GTValues.U4
                    : tagged(candidate, "tiny_dusts", material) ? GTValues.U9 : 0;
            case INGOT -> tagged(candidate, "ingots", material) ? GTValues.U
                    : tagged(candidate, "nuggets", material) ? GTValues.U9 : 0;
            case GEM -> tagged(candidate, "gems", material) ? GTValues.U : 0;
            case PLATE -> tagged(candidate, "plates", material) ? GTValues.U : 0;
            case RAW -> tagged(candidate, "raw_materials", material) ? GTValues.U : 0;
            default -> 0;
        };
    }

    private static boolean tagged(ItemStack candidate, String form, String material) {
        return MaterialEquivalence.hasForgeTag(candidate, form + "/" + material);
    }
}
