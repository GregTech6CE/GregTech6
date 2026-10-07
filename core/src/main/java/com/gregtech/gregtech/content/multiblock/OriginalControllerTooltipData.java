/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * Original CokeOven, DistillationTower, LogisticsCore and BedrockDrill tooltip chains. */
package com.gregtech.gregtech.content.multiblock;

import java.util.List;

/** Shared source identities and structure text; native components stay in the loader adapters. */
public final class OriginalControllerTooltipData {
    private OriginalControllerTooltipData() {}
    public enum Family { COKE_OVEN, DISTILLATION_TOWER, CRYO_DISTILLATION_TOWER, LOGISTICS_CORE, BEDROCK_DRILL }
    public record Energy(String unitKey, long minimum, long maximum, long totalPerTick) {}
    public static Energy standaloneEnergy(Family family) {
        return switch (family) {
            case LOGISTICS_CORE -> new Energy("gt.td.short.energy.electricity",
                    com.gregtech.gregtech.content.logistics.LogisticsCorePowerRules.MIN,
                    com.gregtech.gregtech.content.logistics.LogisticsCorePowerRules.MAX, 0);
            case BEDROCK_DRILL -> new Energy("gt.td.short.energy.kinetic_rotation", 1024, 4096, 32768);
            default -> throw new IllegalArgumentException("Standalone source controller: " + family);
        };
    }

    public static List<String> structureKeys(Family family) {
        return switch (family) {
            case COKE_OVEN -> List.of("gt.tooltip.multiblock.cokeoven.1", "gt.tooltip.multiblock.cokeoven.2");
            case DISTILLATION_TOWER -> List.of("gt.tooltip.multiblock.distillationtower.1", "gt.tooltip.multiblock.distillationtower.2",
                    "gt.tooltip.multiblock.distillationtower.3", "gt.tooltip.multiblock.distillationtower.4",
                    "gt.tooltip.multiblock.distillationtower.5", "gt.tooltip.multiblock.distillationtower.6");
            case CRYO_DISTILLATION_TOWER -> List.of("gt.tooltip.multiblock.cryodistillationtower.1", "gt.tooltip.multiblock.cryodistillationtower.2",
                    "gt.tooltip.multiblock.cryodistillationtower.3", "gt.tooltip.multiblock.cryodistillationtower.4",
                    "gt.tooltip.multiblock.cryodistillationtower.5", "gt.tooltip.multiblock.cryodistillationtower.6");
            case LOGISTICS_CORE -> List.of("gt.tooltip.multiblock.logisticscore.1", "gt.tooltip.multiblock.logisticscore.2",
                    "gt.tooltip.multiblock.logisticscore.3", "gt.tooltip.multiblock.logisticscore.4",
                    "gt.tooltip.multiblock.logisticscore.5", "gt.tooltip.multiblock.logisticscore.6",
                    "gt.tooltip.multiblock.logisticscore.7", "gt.tooltip.multiblock.logisticscore.8",
                    "gt.tooltip.multiblock.logisticscore.9", "gt.tooltip.multiblock.logisticscore.10");
            case BEDROCK_DRILL -> List.of("gt.tooltip.multiblock.bedrockdrill.1", "gt.tooltip.multiblock.bedrockdrill.2",
                    "gt.tooltip.multiblock.bedrockdrill.3", "gt.tooltip.multiblock.bedrockdrill.4");
        };
    }

    public static Family basicFamily(String machineName) {
        return switch (machineName) {
            case "cokeoven" -> Family.COKE_OVEN;
            case "distillationtower" -> Family.DISTILLATION_TOWER;
            case "cryodistillationtower" -> Family.CRYO_DISTILLATION_TOWER;
            default -> null;
        };
    }
}
