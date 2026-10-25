package gregtech.client;
import gregtech.internal.material.MaterialPresentation;



//? if >=1.21.1 {
/**///?} else {
import gregtech.GregTech;
//?}

import gregtech.internal.material.GTMaterial;
import gregtech.internal.material.GTMaterialRegistry;
import gregtech.api.material.GTValues;
import gregtech.internal.material.ItemMaterialRegistry;
import gregtech.internal.material.MaterialChemistry;
import gregtech.internal.machine.FurnaceFuelValue;
import gregtech.internal.material.MaterialProperty;
import gregtech.internal.mod.ModData;
import gregtech.internal.prefix.BlockMaterialPrefix;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.block.BlockMaterialWeights;
import gregtech.content.block.stone.StoneMaterialWeights;
import gregtech.content.block.stone.StoneVariant;
import gregtech.content.data.GregTechConstants;
import gregtech.content.data.ModReferences;
import gregtech.content.recipe.ShapelessRecipeTooltipIndex;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * GregTech-style material item tooltips (mirrors GT6 {@code GT_API_Proxy_Client} material section).
 */
public final class MaterialTooltips {
    private MaterialTooltips() {}

    public static void append(ItemStack stack, GTMaterial material, MaterialPrefix prefix,
                              List<Component> tooltip, TooltipFlag flag) {
        append(stack, material, prefix.getMaterialWeight(), prefix, tooltip, flag);
    }

    public static void appendBlock(ItemStack stack, GTMaterial material, BlockMaterialPrefix prefix,
                                   List<Component> tooltip, TooltipFlag flag) {
        appendWithMaterials(stack, material, BlockMaterialWeights.contained(material, prefix), null, tooltip, flag);
    }

    /** Decorative stone blocks ({@link gregtech.content.block.stone.GTStoneBlock}). */
    public static void appendStoneBlock(ItemStack stack, GTMaterial material, StoneVariant variant, boolean slab,
                                        List<Component> tooltip, TooltipFlag flag) {
        appendWithMaterials(stack, material, StoneMaterialWeights.contained(material, variant, slab), null, tooltip, flag);
    }

    private static void appendWithMaterials(ItemStack stack, GTMaterial material,
                                            List<MaterialChemistry.WeightedMaterial> materials,
                                            @Nullable MaterialPrefix prefix,
                                            List<Component> tooltip, TooltipFlag flag) {
        GTMaterial mat = material.resolve();
        if (mat.getId() <= 0) {
            return;
        }

        if (mat.hasToolStats() && prefix != null) {
            tooltip.add(Component.literal(String.format(Locale.ROOT,
                            "Q: %d - S: %s - D: %d",
                            mat.getToolQuality(),
                            formatSpeed(mat.getToolSpeed()),
                            mat.getToolDurability()))
                    .withStyle(ChatFormatting.BLUE));
        }

        appendFuelValue(FurnaceFuelValue.getBurnValue(stack), tooltip);
        appendFlammableTooltip(mat, tooltip);

        String chemical = mat.getResolvedTooltipChemical();
        if (gregtech.GregTechConfig.chemicalFormulas() && chemical != null && !chemical.isEmpty()) {
            tooltip.add(Component.literal(chemical).withStyle(ChatFormatting.YELLOW));
        }

        if (prefix != null) {
            List<Integer> shapeless = ShapelessRecipeTooltipIndex.inputAmounts(prefix, mat);
            if (!shapeless.isEmpty()) {

//? if >=1.21.1 {
/*                tooltip.add(Component.translatable("tooltip." + "gt6ce" + ".shapeless_recipes")
*///?} else {
                tooltip.add(Component.translatable("tooltip." + GregTech.NAMESPACE + ".shapeless_recipes")
//?}

                        .append(Component.literal(shapeless.toString()).withStyle(ChatFormatting.WHITE))
                        .withStyle(ChatFormatting.AQUA));
            }
        }

        if (gregtech.GregTechConfig.materialDetails() && flag.isAdvanced()) {

//? if >=1.21.1 {
/*            tooltip.add(Component.translatable("tooltip." + "gt6ce" + ".contained_materials")
*///?} else {
            tooltip.add(Component.translatable("tooltip." + GregTech.NAMESPACE + ".contained_materials")
//?}

                    .withStyle(ChatFormatting.DARK_AQUA));
            for (MaterialChemistry.WeightedMaterial weighted : materials) {
                if (weighted.material().isValid()) {
                    tooltip.add(containedMaterialLine(weighted.material(), weighted.amount()));
                }
            }
        } else if (gregtech.GregTechConfig.materialDetails()) {

//? if >=1.21.1 {
/*            tooltip.add(Component.translatable("tooltip." + "gt6ce" + ".f3h_hint")
*///?} else {
            tooltip.add(Component.translatable("tooltip." + GregTech.NAMESPACE + ".f3h_hint")
//?}

                    .withStyle(ChatFormatting.DARK_GRAY));
        }

        if(flag.isAdvanced() && prefix!=null) {
            var form = new gregtech.internal.material.MaterialEquivalence.Form(prefix,mat);
            String tag = gregtech.internal.material.MaterialEquivalence.tagPath(form);
            if(tag!=null) tooltip.add(Component.translatable("tooltip.gt6ce.material_tag","forge:"+tag).withStyle(ChatFormatting.DARK_GRAY));
            GTMaterial target = prefix==MaterialPrefix.oreRaw ? mat.getTargetCrushingMaterial() : mat.getTargetPulverMaterial();
            if(target.resolve()!=mat) tooltip.add(Component.translatable(prefix==MaterialPrefix.oreRaw ? "tooltip.gt6ce.crushing_result" : "tooltip.gt6ce.pulver_result",MaterialPresentation.name(target)).withStyle(ChatFormatting.GRAY));
        }
        appendSourceModTooltip(tooltip, mat);
    }

    private static void append(ItemStack stack, GTMaterial material, long materialAmount,
                               @Nullable MaterialPrefix prefix, List<Component> tooltip, TooltipFlag flag) {
        appendWithMaterials(stack, material,
                MaterialChemistry.prefixMaterialWeights(material, prefix), prefix, tooltip, flag);
    }

    public static void appendSourceModTooltip(List<Component> tooltip, GTMaterial mat) {
        ModData source = mat.getSourceMod();
        if (source == null || source == ModReferences.UNKNOWN) {

//? if >=1.21.1 {
/*            tooltip.add(Component.translatable("tooltip." + "gt6ce" + ".material_from_gt")
*///?} else {
            tooltip.add(Component.translatable("tooltip." + GregTech.NAMESPACE + ".material_from_gt")
//?}

                    .withStyle(ChatFormatting.BLUE));
            return;
        }
        if (source == ModReferences.MC) {

//? if >=1.21.1 {
/*            tooltip.add(Component.translatable("tooltip." + "gt6ce" + ".vanilla_material")
*///?} else {
            tooltip.add(Component.translatable("tooltip." + GregTech.NAMESPACE + ".vanilla_material")
//?}

                    .withStyle(ChatFormatting.BLUE));
            return;
        }
        if ((source == ModReferences.GT || source == ModReferences.GAPI) && isPeriodicTableElement(mat)) {

//? if >=1.21.1 {
/*            tooltip.add(Component.translatable("tooltip." + "gt6ce" + ".material_from_periodic_table")
*///?} else {
            tooltip.add(Component.translatable("tooltip." + GregTech.NAMESPACE + ".material_from_periodic_table")
//?}

                    .withStyle(ChatFormatting.BLUE));
            return;
        }
        if (source == ModReferences.GT || source == ModReferences.GAPI) {

//? if >=1.21.1 {
/*            tooltip.add(Component.translatable("tooltip." + "gt6ce" + ".material_from_gt")
*///?} else {
            tooltip.add(Component.translatable("tooltip." + GregTech.NAMESPACE + ".material_from_gt")
//?}

                    .withStyle(ChatFormatting.BLUE));
            return;
        }
        var owners = gregtech.internal.addon.AddonMaterials.owners(mat);
        if (owners.size() > 1) {
//? if >=1.21.1 {
/*            tooltip.add(Component.translatable("tooltip.gt6ce.material_from_addons", String.join(", ", owners))
                    .withStyle(ChatFormatting.BLUE));
*///?} else {
            tooltip.add(Component.translatable("tooltip." + GregTech.NAMESPACE + ".material_from_addons",
                    String.join(", ", owners)).withStyle(ChatFormatting.BLUE));
//?}
            return;
        }
        String sourceKey = gregtech.internal.material.GTMaterialRegistry.isCoreMaterial(mat)
//? if >=1.21.1 {
/*// Version-specific branch.
                ? "tooltip." + "gt6ce" + ".material_from_mod"
                : "tooltip." + "gt6ce" + ".material_from_addon";
*///?} else {
// Version-specific branch.
                ? "tooltip." + GregTech.NAMESPACE + ".material_from_mod"
                : "tooltip." + GregTech.NAMESPACE + ".material_from_addon";
//?}
        tooltip.add(Component.translatable(sourceKey, source.name)
                .withStyle(ChatFormatting.BLUE));
    }

    private static boolean isPeriodicTableElement(GTMaterial mat) {
        return GTMaterialRegistry.isPeriodicTableElement(mat);
    }

    public static void appendForeign(ItemStack stack, gregtech.internal.material.ItemComposition data,
                                     List<Component> tooltip, TooltipFlag flag) {
        var form = gregtech.internal.material.MaterialEquivalence.form(stack);
        if(form!=null) {
            append(stack,form.material(),form.prefix(),tooltip,flag);
            return;
        }
        GTMaterial mat = data.material();
        if (mat.getId() <= 0) {
            if (flag.isAdvanced()) tooltip.add(Component.translatable("tooltip.gt6ce.composition_nonmaterial").withStyle(ChatFormatting.GRAY));
            return;
        }

        String chemical = data.components().size() == 1 ? mat.getResolvedTooltipChemical() : "";
        if (gregtech.GregTechConfig.chemicalFormulas() && chemical != null && !chemical.isEmpty()) {
            tooltip.add(Component.literal(chemical).withStyle(ChatFormatting.YELLOW));
        }

        if (data.components().size() == 1) {
            appendFuelTooltip(mat, data.prefix(), data.amount(), tooltip);
            appendFlammableTooltip(mat, tooltip);
        }
        if (!data.recoverable()) tooltip.add(Component.translatable("tooltip.gt6ce.composition_descriptive").withStyle(ChatFormatting.GRAY));

        if (gregtech.GregTechConfig.materialDetails() && flag.isAdvanced()) {

//? if >=1.21.1 {
/*            tooltip.add(Component.translatable("tooltip." + "gt6ce" + ".contained_materials")
*///?} else {
            tooltip.add(Component.translatable("tooltip." + GregTech.NAMESPACE + ".contained_materials")
//?}

                    .withStyle(ChatFormatting.DARK_AQUA));
            for (var component : data.components()) for (MaterialChemistry.WeightedMaterial weighted : MaterialChemistry.materialWeights(component.material(), component.amount())) {
                if (weighted.material().isValid()) {
                    tooltip.add(containedMaterialLine(weighted.material(), weighted.amount()));
                }
            }
        } else if (gregtech.GregTechConfig.materialDetails()) {

//? if >=1.21.1 {
/*            tooltip.add(Component.translatable("tooltip." + "gt6ce" + ".f3h_hint")
*///?} else {
            tooltip.add(Component.translatable("tooltip." + GregTech.NAMESPACE + ".f3h_hint")
//?}

                    .withStyle(ChatFormatting.DARK_GRAY));
        }


//? if >=1.21.1 {
/*        tooltip.add(Component.translatable(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals("minecraft") ? "tooltip.gt6ce.vanilla_material" : "tooltip.gt6ce.foreign_material")
*///?} else {
        tooltip.add(Component.translatable(net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem()).getNamespace().equals("minecraft") ? "tooltip.gt6ce.vanilla_material" : "tooltip.gt6ce.foreign_material")
//?}

                .withStyle(ChatFormatting.BLUE));
    }

    public static Component containedMaterialLine(GTMaterial mat, long amount) {
        if (!mat.isValid()) {
            return Component.empty();
        }
        if (GTMaterialRegistry.isVanillaMatter(mat)) return Component.literal(displayUnits(amount) + " ")
                .append(MaterialPresentation.name(mat)).append(Component.translatable("tooltip.gt6ce.composition_category"));
        double weightKg = weightKg(mat, amount);
        long frac = ((long) (weightKg * 1000)) % 1000;

        return Component.empty()
                .append(Component.literal(displayUnits(amount) + " ").withStyle(ChatFormatting.WHITE))
                .append(MaterialPresentation.name(mat).copy().withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" (").withStyle(ChatFormatting.WHITE))
                .append(Component.literal("M: ").withStyle(ChatFormatting.AQUA))
                .append(Component.literal(String.valueOf(mat.getMeltingPoint())).withStyle(ChatFormatting.WHITE))
                .append(Component.literal("K ").withStyle(ChatFormatting.RED))
                .append(Component.literal(" B: ").withStyle(ChatFormatting.AQUA))
                .append(Component.literal(String.valueOf(mat.getBoilingPoint())).withStyle(ChatFormatting.WHITE))
                .append(Component.literal("K ").withStyle(ChatFormatting.RED))
                .append(Component.literal(" W: ").withStyle(ChatFormatting.AQUA))
                .append(Component.literal(formatWeight(weightKg, frac)).withStyle(ChatFormatting.WHITE))
                .append(Component.literal("kg").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(")").withStyle(ChatFormatting.WHITE));
    }

    /** {@code (g/cm³ × amount × 0.111 - /U  -> kg}, same as GT6 {@code OreDictMaterial#getWeight}. */
    public static double weightKg(GTMaterial material, long amount) {
        return weightKg(material, amount, material.getDensity());
    }

    public static double weightKg(GTMaterial material, long amount, double densityGPerCm3) {
        return gregtech.internal.material.MaterialMass.kilograms(densityGPerCm3, amount);
    }

    public static String displayUnits(long amount) {
        if (amount < 0) {
            return "?.???";
        }
        long digits = ((amount % GTValues.U) * 1000) / GTValues.U;
        return (amount / GTValues.U) + "." + pad3(digits);
    }

    public static String pad3(long digits) {
        if (digits < 1) return "000";
        if (digits < 10) return "00" + digits;
        if (digits < 100) return "0" + digits;
        return Long.toString(digits);
    }

    private static String formatWeight(double whole, long frac) {
        long w = (long) whole;
        return w + "." + pad3(frac);
    }

    /** Shared Q/S/D line for pipes, tanks, and blocks. Called by {@link TankTooltips}. */
    public static void appendToolStats(GTMaterial mat, List<Component> tooltip) {
        if (!mat.hasToolStats()) return;
        tooltip.add(Component.literal(String.format(Locale.ROOT,
                        "Q: %d - S: %s - D: %d",
                        mat.getToolQuality(),
                        formatSpeed(mat.getToolSpeed()),
                        mat.getToolDurability()))
                .withStyle(ChatFormatting.BLUE));
    }

    private static String formatSpeed(float speed) {
        if (Math.abs(speed - Math.round(speed)) < 0.001F) {
            return String.format(Locale.ROOT, "%.1f", speed);
        }
        return String.format(Locale.ROOT, "%s", speed);
    }

    public static void appendFuelTooltip(GTMaterial material, @Nullable MaterialPrefix prefix, long amount,
                                         List<Component> tooltip) {
        appendFuelValue(FurnaceFuelValue.getBurnValue(material, prefix, amount), tooltip);
    }

    private static void appendFuelValue(long burnValue, List<Component> tooltip) {
        if (burnValue <= 0) {
            return;
        }
        long heat = burnValue * GregTechConstants.EU_PER_FURNACE_TICK;
        tooltip.add(Component.empty()

//? if >=1.21.1 {
/*                .append(Component.translatable("tooltip." + "gt6ce" + ".fuel_value.label")
*///?} else {
                .append(Component.translatable("tooltip." + GregTech.NAMESPACE + ".fuel_value.label")
//?}

                        .withStyle(ChatFormatting.RED))
                .append(Component.literal(Long.toString(burnValue)).withStyle(ChatFormatting.WHITE))
                .append(Component.literal(" (" + heat + " ").withStyle(ChatFormatting.WHITE))
                .append(Component.literal("HU").withStyle(ChatFormatting.RED))
                .append(Component.literal(")").withStyle(ChatFormatting.WHITE)));
    }

    public static void appendFlammableTooltip(GTMaterial material, List<Component> tooltip) {
        if (material.resolve().has(MaterialProperty.FLAMMABLE)) {

//? if >=1.21.1 {
/*            tooltip.add(Component.translatable("tooltip." + "gt6ce" + ".flammable")
*///?} else {
            tooltip.add(Component.translatable("tooltip." + GregTech.NAMESPACE + ".flammable")
//?}

                    .withStyle(ChatFormatting.RED));
        }
    }
}
