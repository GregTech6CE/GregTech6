package com.gregtech.gregtech.client;


import com.gregtech.gregtech.api.energy.FaceConfig;
import com.gregtech.gregtech.api.machine.*;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTValues;
import com.gregtech.gregtech.block.machine.EngineBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.List;

/** GT6-style engine item tooltips matching the original {@code MultiTileEntityGeneratorKinetic} format. */
public final class EngineTooltips {
    private EngineTooltips() {}

    // Default face configs (matching each engine type's defaultFaceConfig)
    private static final int ENERGY_IN_FACES =
            (1 << FaceConfig.LEFT) | (1 << FaceConfig.RIGHT) | (1 << FaceConfig.BACK);
    private static final int ENERGY_OUT_FACES = 1 << FaceConfig.FRONT;
    private static final int FLUID_IN_FACES   = 1 << FaceConfig.BACK;
    private static final int FLUID_OUT_FACES  = ENERGY_OUT_FACES;

    private static final String MOD = "gregtech";

    public static void appendEngine(EngineBlock block, List<Component> tooltip) {
        EngineType type = block.engineType();
        MachineSpec ms = block.machineSpec();

        switch (type) {
            case ELECTRIC -> appendElectric(block, ms, tooltip);
            case FLUX     -> appendFlux(block, ms, tooltip);
            case STEAM    -> appendSteam(block, ms, tooltip);
            case ROTATION -> appendRotation(block, ms, tooltip);
            case DIESEL   -> tooltip.add(Component.empty().append(Component.translatable("tooltip."+MOD+".machine.energy_out")).append(Component.literal(" "+TooltipHelper.formatLong(block.engineSpec(DieselEngineSpec.class).outputRate())+" RU/t ("+TooltipHelper.dirMaskToString(ENERGY_OUT_FACES)+")")).withStyle(ChatFormatting.RED));
        }

        // Tool hints
        appendToolHints(type, tooltip);

        // Blast resistance
        TooltipHelper.appendBlastResistance(ms.blastResistance(), tooltip);

        // Harvest tool
        TooltipHelper.appendHarvestWrench(tooltip);

        // Material name
        GTMaterial mat = resolveMaterial(block, type);
        if (mat != null && mat.isValid()) {
            tooltip.add(Component.literal(mat.getLocalName()).withStyle(ChatFormatting.YELLOW));

            // Contained materials
            tooltip.add(Component.translatable("tooltip." + MOD + ".machine.contained_materials")
                    .withStyle(ChatFormatting.BLUE));
            tooltip.add(EngineMaterialTooltips.containedMaterialLine(mat, 9L * GTValues.U));
        }

        // Footer
        tooltip.add(Component.literal("GregTech").withStyle(ChatFormatting.BLUE));
    }

    // ========================================================================
    //  ELECTRIC — EU → KU
    // ========================================================================

    private static void appendElectric(EngineBlock block, MachineSpec ms, List<Component> tooltip) {
        ElectricEngineSpec spec = block.engineSpec(ElectricEngineSpec.class);
        long in = spec.inputRate();
        long out = ms.outputRate();

        tooltip.add(Component.translatable("tooltip." + MOD + ".engine.desc.electric")
                .withStyle(ChatFormatting.BLUE));

        appendEfficiencyLine(ms.efficiency(), tooltip);

        appendPistonRates("EU", in, out, tooltip);
    }

    // ========================================================================
    //  FLUX — RF → KU
    // ========================================================================

    private static void appendFlux(EngineBlock block, MachineSpec ms, List<Component> tooltip) {
        FluxEngineSpec spec = block.engineSpec(FluxEngineSpec.class);
        long in = spec.inputRate();
        long out = ms.outputRate();

        tooltip.add(Component.translatable("tooltip." + MOD + ".engine.desc.flux")
                .withStyle(ChatFormatting.BLUE));

        appendEfficiencyLine(ms.efficiency(), tooltip);

        appendPistonRates("RF", in, out, tooltip);
    }

    private static void appendPistonRates(String unit, long input, long output, List<Component> tooltip) {
        tooltip.add(Component.translatable("tooltip.gregtech.engine.piston.rates", input, unit, output));
        appendCapacityLine(unit, input * 2, ChatFormatting.GREEN, tooltip);
        tooltip.add(Component.translatable("tooltip.gregtech.engine.piston.faces"));
        tooltip.add(Component.translatable("tooltip.gregtech.engine.piston.modes",
                (input + 15) / 16, input * 2, unit, output / 16, output * 2));
    }

    // ========================================================================
    //  STEAM — Steam → KU
    // ========================================================================

    private static void appendSteam(EngineBlock block, MachineSpec ms, List<Component> tooltip) {
        SteamEngineData spec = block.engineSpec(SteamEngineData.class);
        long out = ms.outputRate();
        int eff = spec.efficiency();

        tooltip.add(Component.translatable("tooltip." + MOD + ".engine.desc.steam")
                .withStyle(ChatFormatting.BLUE));

        appendEfficiencyLine(eff, tooltip);

        // Energy IN: steam rate range
        long steamMin = out * 10000 / eff;
        long steamMax = out * 40000 / eff;
        String dirsIn = TooltipHelper.dirMaskToString(FLUID_IN_FACES);
        tooltip.add(Component.empty()
                .append(Component.translatable("tooltip." + MOD + ".machine.energy_in").withStyle(ChatFormatting.GREEN))
                .append(Component.literal(steamMin + " - " + steamMax + " Steam/t")
                        .withStyle(ChatFormatting.WHITE))
                .append(Component.literal(" (" + dirsIn + ")").withStyle(ChatFormatting.WHITE)));
        appendCapacityLine("Steam", spec.steamCapacity(), ChatFormatting.GREEN, tooltip);

        // Energy OUT: KU range
        long kuMin = out / 2;
        long kuMax = out * 2;
        String dirsOut = TooltipHelper.dirMaskToString(ENERGY_OUT_FACES);
        tooltip.add(Component.empty()
                .append(Component.translatable("tooltip." + MOD + ".machine.energy_out").withStyle(ChatFormatting.RED))
                .append(Component.literal(kuMin + " - " + kuMax + " KU/t")
                        .withStyle(ChatFormatting.WHITE))
                .append(Component.literal(" (" + dirsOut + ")").withStyle(ChatFormatting.WHITE)));
        appendCapacityLine("KU", out * 1000, ChatFormatting.RED, tooltip);

        // Emits distilled water
        String waterDirs = TooltipHelper.dirMaskToString(FLUID_OUT_FACES);
        tooltip.add(Component.empty()
                .append(Component.translatable("tooltip." + MOD + ".engine.steam.emits_water")
                        .withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" (" + waterDirs + ")")
                        .withStyle(ChatFormatting.WHITE)));

        // Can emit RF with loss
        tooltip.add(Component.translatable("tooltip." + MOD + ".engine.emit_rf", 50)
                .withStyle(ChatFormatting.YELLOW));
    }

    // ========================================================================
    //  ROTATION — RU → KU
    // ========================================================================

    private static void appendRotation(EngineBlock block, MachineSpec ms, List<Component> tooltip) {
        RotationEngineSpec spec = block.engineSpec(RotationEngineSpec.class);
        long in = spec.inputRate();
        long out = ms.outputRate();

        tooltip.add(Component.translatable("tooltip." + MOD + ".engine.desc.rotation")
                .withStyle(ChatFormatting.BLUE));

        appendEfficiencyLine(ms.efficiency(), tooltip);

        tooltip.add(energyInRangeLine("RU/t", in, ENERGY_IN_FACES));
        appendCapacityLine("RU", in * 8, ChatFormatting.GREEN, tooltip);

        tooltip.add(energyOutRangeLine("KU/t", out, ENERGY_OUT_FACES));
        appendCapacityLine("KU", out * 2, ChatFormatting.RED, tooltip);

        // Can emit RF with loss
        tooltip.add(Component.translatable("tooltip." + MOD + ".engine.emit_rf", 50)
                .withStyle(ChatFormatting.YELLOW));
    }

    // ========================================================================
    //  Line builders
    // ========================================================================

    /** "Energy IN: 128 - 1,024 EU/t (Left, Right, Back)" */
    private static Component energyInRangeLine(String unit, long rate, int faces) {
        long min = rate / 4;
        long max = rate * 2;
        return Component.empty()
                .append(Component.translatable("tooltip." + MOD + ".machine.energy_in").withStyle(ChatFormatting.GREEN))
                .append(Component.literal(
                        TooltipHelper.formatLong(min) + " - " + TooltipHelper.formatLong(max) + " " + unit)
                        .withStyle(ChatFormatting.WHITE))
                .append(Component.literal(" (" + TooltipHelper.dirMaskToString(faces) + ")")
                        .withStyle(ChatFormatting.WHITE));
    }

    /** "Energy OUT: 2 - 16 KU/t (Front)" */
    private static Component energyOutRangeLine(String unit, long rate, int faces) {
        long min = rate / 4;
        long max = rate;
        return Component.empty()
                .append(Component.translatable("tooltip." + MOD + ".machine.energy_out").withStyle(ChatFormatting.RED))
                .append(Component.literal(
                        TooltipHelper.formatLong(min) + " - " + TooltipHelper.formatLong(max) + " " + unit)
                        .withStyle(ChatFormatting.WHITE))
                .append(Component.literal(" (" + TooltipHelper.dirMaskToString(faces) + ")")
                        .withStyle(ChatFormatting.WHITE));
    }

    /** "Efficiency: 30.00%" */
    private static void appendEfficiencyLine(int efficiency, List<Component> tooltip) {
        tooltip.add(Component.empty()
                .append(Component.translatable("tooltip." + MOD + ".machine.efficiency.label").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(MachineTooltips.formatEfficiencyPercent(efficiency) + "%")
                        .withStyle(ChatFormatting.WHITE)));
    }

    /** "Capacity: 1,024 EU" */
    private static void appendCapacityLine(String unit, long capacity, ChatFormatting color, List<Component> tooltip) {
        tooltip.add(Component.empty()
                .append(Component.translatable("tooltip." + MOD + ".engine.capacity").withStyle(color))
                .append(Component.literal(TooltipHelper.formatLong(capacity) + " " + unit)
                        .withStyle(ChatFormatting.WHITE)));
    }

    // ========================================================================
    //  Tool hints
    // ========================================================================

    private static void appendToolHints(EngineType type, List<Component> tooltip) {
        ChatFormatting g = ChatFormatting.WHITE;
        if (type == EngineType.ELECTRIC || type == EngineType.FLUX) {
            tooltip.add(Component.translatable("tooltip.gregtech.engine.piston.tools").withStyle(g));
        } else if (type == EngineType.STEAM) {
            tooltip.add(Component.translatable("tooltip." + MOD + ".engine.hint.soft_hammer_toggle").withStyle(g));
        } else {
            tooltip.add(Component.translatable("tooltip." + MOD + ".engine.hint.soft_hammer").withStyle(g));
        }
        tooltip.add(Component.translatable("tooltip." + MOD + ".engine.hint.magnifying_glass").withStyle(g));
        tooltip.add(Component.translatable("tooltip." + MOD + ".engine.hint.wrench").withStyle(g));
    }

    // ========================================================================
    //  Helpers
    // ========================================================================

    private static GTMaterial resolveMaterial(EngineBlock block, EngineType type) {
        return switch (type) {
            case ELECTRIC -> block.engineSpec(ElectricEngineSpec.class).material();
            case FLUX     -> block.engineSpec(FluxEngineSpec.class).material();
            case STEAM    -> block.engineSpec(SteamEngineData.class).material();
            case ROTATION -> block.engineSpec(RotationEngineSpec.class).material();
            case DIESEL   -> block.engineSpec(com.gregtech.gregtech.api.machine.DieselEngineSpec.class).material();
        };
    }
}
