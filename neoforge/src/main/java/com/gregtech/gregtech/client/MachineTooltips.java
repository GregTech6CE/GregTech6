package com.gregtech.gregtech.client;




import com.gregtech.gregtech.api.machine.MachineSpec;
import com.gregtech.gregtech.api.machine.BasicMachineSpec;
import com.gregtech.gregtech.api.energy.FaceConfig;
import com.gregtech.gregtech.api.material.MaterialChemistry.WeightedMaterial;
import com.gregtech.gregtech.data.MachineRecipeMaps;



import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;

/** GT6-style machine item tooltips ({@code MultiTileEntityGeneratorSolid#addToolTips} and shared tags). */
public final class MachineTooltips {
    private MachineTooltips() {}

    // ── Basic machine tooltip ────────────────────────────────────────────

    public static void appendBasicMachine(BasicMachineSpec spec, List<Component> tooltip) {
        if(com.gregtech.gregtech.content.multiblock.OriginalControllerTooltipData.basicFamily(spec.machineName())!=null) {
            if(!CommonBlockTooltips.containsKey(tooltip,"gt.lang.recipes")) OriginalControllerTooltips.basic(spec,tooltip);
            return; // Dedicated blocks own the original inherited rows; their item must not append them twice.
        }
        FaceConfig fc = spec.faceConfig();

        // Recipes
        var recipeMap = MachineRecipeMaps.byMachineName(spec.machineName());
        String recipeName = recipeMap != null ? recipeMap.mNameLocal : spec.machineName();
        tooltip.add(Component.translatable("tooltip." + "gregtech" + ".machine.recipes")
                .append(Component.literal(recipeName).withStyle(ChatFormatting.WHITE))
                .withStyle(ChatFormatting.AQUA));

        // Energy IN
        if (spec.energyIn() > 0) {
            String dirs = TooltipHelper.dirMaskToString(fc.energyInputs());
            tooltip.add(TooltipHelper.energyInLine(spec.energyIn(), spec.energyInMin(),
                    spec.energyInMax(), spec.energyType(), dirs));
        }

        // Energy OUT
        if (spec.energyOut() > 0) {
            String dirs = TooltipHelper.dirMaskToString(fc.energyOutputs());
            tooltip.add(TooltipHelper.energyOutLine(spec.energyOut(), spec.energyType())
                    .append(Component.literal(" (" + dirs + ")").withStyle(ChatFormatting.WHITE)));
        }

        // Items IN
        if (fc.itemInputs() != 0) {
            Component line = TooltipHelper.ioLine("items_in", fc.itemInputs(), fc.itemAutoInput(), ChatFormatting.GREEN);
            if (line != null) tooltip.add(line);
        }

        // Items OUT
        if (fc.itemOutputs() != 0) {
            Component line = TooltipHelper.ioLine("items_out", fc.itemOutputs(), fc.itemAutoOutput(), ChatFormatting.RED);
            if (line != null) tooltip.add(line);
        }

        // Fluids IN
        if (fc.fluidInputs() != 0) {
            Component line = TooltipHelper.ioLine("fluids_in", fc.fluidInputs(), fc.fluidAutoInput(), ChatFormatting.GREEN);
            if (line != null) tooltip.add(line);
        }

        // Fluids OUT
        if (fc.fluidOutputs() != 0) {
            Component line = TooltipHelper.ioLine("fluids_out", fc.fluidOutputs(), fc.fluidAutoOutput(), ChatFormatting.RED);
            if (line != null) tooltip.add(line);
        }

        // Tool instructions
        TooltipHelper.appendToolInstructions(tooltip);

        // Blast resistance
        TooltipHelper.appendBlastResistance(spec.blastResistance(), tooltip);

        // Harvest tool
        TooltipHelper.appendHarvestWrench(tooltip);

        // The material event adds the shared exact ItemComposition once in F3+H.

    }

    // ── Solid burning box tooltip (new format) ──────────────────────────

    public static void appendSolidBurningBox(MachineSpec spec, List<Component> tooltip) {
        // Recipes
        appendFurnaceFuelRecipes(tooltip);

        // Efficiency
        appendEfficiency(spec, tooltip);

        // Energy OUT
        appendEnergyOutHu(spec, tooltip);

        // Energy IN: None
        tooltip.add(Component.translatable("tooltip." + "gregtech" + ".machine.energy_in")
                .append(Component.translatable("tooltip." + "gregtech" + ".machine.none")
                        .withStyle(ChatFormatting.WHITE))
                .withStyle(ChatFormatting.RED));

        // Burning box specific requirements
        appendRequiresAirFront(tooltip);
        appendRequiresAshesFront(tooltip);
        appendRequiresIgnitionFront(tooltip);
        appendNoGuiClickFront(tooltip);
        appendHazardFire(tooltip);
        appendHazardContactTop(tooltip);

        // Tool instructions
        appendShovelEmpty(tooltip);
        appendWrenchFacing(tooltip);

        // Blast resistance
        TooltipHelper.appendBlastResistance(spec.blastResistance(), tooltip);

        // Harvest tool
        TooltipHelper.appendHarvestWrench(tooltip);
    }

    // ── Shared line helpers ─────────────────────────────────────────────

    public static void appendFurnaceFuelRecipes(List<Component> tooltip) {
        tooltip.add(Component.translatable("tooltip." + "gregtech" + ".machine.recipes")
                .append(Component.translatable("tooltip." + "gregtech" + ".machine.recipes.furnace_fuels")
                        .withStyle(ChatFormatting.WHITE))
                .withStyle(ChatFormatting.AQUA));
    }

    public static void appendEfficiency(MachineSpec spec, List<Component> tooltip) {
        tooltip.add(Component.empty()
                .append(Component.translatable("tooltip." + "gregtech" + ".machine.efficiency.label")
                        .withStyle(ChatFormatting.GREEN))
                .append(Component.literal(formatEfficiencyPercent(spec.efficiency()) + "%")
                        .withStyle(ChatFormatting.WHITE)));
    }

    public static void appendEnergyOutHu(MachineSpec spec, List<Component> tooltip) {
        tooltip.add(Component.empty()
                .append(Component.translatable("tooltip." + "gregtech" + ".machine.energy_out")
                        .withStyle(ChatFormatting.RED))
                .append(Component.literal(TooltipHelper.formatLong(spec.outputRate()) + " HU/t")
                        .withStyle(ChatFormatting.WHITE)));
    }

    // ── Burning box specific lines ──────────────────────────────────────

    public static void appendRequiresAirFront(List<Component> tooltip) {
        tooltip.add(Component.translatable("tooltip." + "gregtech" + ".machine.requirement.air_front")
                .withStyle(ChatFormatting.GOLD));
    }

    public static void appendRequiresAshesFront(List<Component> tooltip) {
        tooltip.add(Component.translatable("tooltip." + "gregtech" + ".machine.requirement.ashes_front")
                .withStyle(ChatFormatting.GOLD));
    }

    public static void appendRequiresIgnitionFront(List<Component> tooltip) {
        tooltip.add(Component.translatable("tooltip." + "gregtech" + ".machine.requirement.ignite_front")
                .withStyle(ChatFormatting.GOLD));
    }

    public static void appendNoGuiClickFront(List<Component> tooltip) {
        tooltip.add(Component.translatable("tooltip." + "gregtech" + ".machine.nogui.click_front")
                .withStyle(ChatFormatting.GOLD));
    }

    public static void appendHazardFire(List<Component> tooltip) {
        tooltip.add(Component.translatable("tooltip." + "gregtech" + ".machine.hazard.fire", 4)
                .withStyle(ChatFormatting.DARK_RED));
    }

    public static void appendHazardContactTop(List<Component> tooltip) {
        tooltip.add(Component.translatable("tooltip." + "gregtech" + ".machine.hazard.contact_top")
                .withStyle(ChatFormatting.DARK_RED));
    }

    public static void appendShovelEmpty(List<Component> tooltip) {
        tooltip.add(Component.translatable("tooltip." + "gregtech" + ".machine.tool.shovel_empty")
                .withStyle(ChatFormatting.GRAY));
    }

    public static void appendWrenchFacing(List<Component> tooltip) {
        tooltip.add(Component.translatable("tooltip." + "gregtech" + ".machine.tool.wrench_facing")
                .withStyle(ChatFormatting.GRAY));
    }

    // ── Number formatting ───────────────────────────────────────────────

    /** GT6 {@code LH.percent} for efficiency values stored as 0..10000. */
    public static String formatEfficiencyPercent(int efficiency) {
        long value = Math.abs(efficiency);
        long whole = value / 100;
        long frac = value % 100;
        if (frac > 9) {
            return whole + "." + frac;
        }
        if (frac > 0) {
            return whole + ".0" + frac;
        }
        return whole + ".00";
    }
}
