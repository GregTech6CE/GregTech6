/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * Original 17110/17198/17199 registrations and their inherited BasicMachine defaults. */
package gregtech.content.multiblock;

import gregtech.api.energy.EnergyType;
import gregtech.api.machine.BasicMachineParameters;
import gregtech.api.machine.MachineFace;
import gregtech.api.machine.MachineFaceMasks;
import gregtech.content.data.BasicMachineOriginalParams;
import gregtech.content.machine.HostMachineFaces;
import gregtech.content.machine.OriginalMachineMaterialData;
import gregtech.engine.material.GTMaterialRegistry;
import java.util.List;

public final class OriginalAdvancedControllerData {
    private OriginalAdvancedControllerData() {}

    public static boolean handles(String name) {
        return name.equals("implosioncompressor") || name.equals("fusionreactor") || name.equals("largemassfab");
    }

    public static List<String> structureKeys(String name) {
        String family;
        int count;
        switch (name) {
            case "implosioncompressor" -> {
                family = "implosioncompressor";
                count = 2;
            }
            case "fusionreactor" -> {
                family = "fusionreactor";
                count = 7;
            }
            case "largemassfab" -> {
                family = "matterfabricator";
                count = 8;
            }
            default -> throw new IllegalArgumentException("Original advanced controller:" + name);
        }
        final String prefix = "gt.tooltip.multiblock." + family + ".";
        return java.util.stream.IntStream.rangeClosed(1, count)
                .mapToObj(i -> prefix + i)
                .toList();
    }

    public static boolean cheapOverclocking(String name) {
        return name.equals("largemassfab");
    }

    public static String chargedEnergy(String name) {
        return name.equals("fusionreactor") ? "LU" : "TU";
    }

    public static MachineFaceMasks faces(String name) {
        if (!handles(name)) throw new IllegalArgumentException(name);
        int output = name.equals("fusionreactor") ? -1 : MachineFace.BOTTOM.index();
        return HostMachineFaces.fromMask(63, 63, 63, 63, 63, 0, -1, output, -1, output);
    }

    public static BasicMachineParameters parameters(String name, String id) {
        if (!handles(name)) throw new IllegalArgumentException(name);
        var source = BasicMachineOriginalParams.find(name, 1);
        String hull = switch (name) {
            case "implosioncompressor" -> "TungstenSteel";
            case "fusionreactor" -> "SteelGalvanized";
            default -> "Pb";
        };
        return BasicMachineParameters.builder(id, GTMaterialRegistry.get(hull))
                .machineType(name)
                .energy(EnergyType.of(source.energyType()), source.energyInput())
                .tier(source.tier())
                .energyRange(source.energyInputMin(), source.energyInputMax())
                .parallel(source.parallel())
                .strength(source.hardness(), source.resistance())
                .faces(faces(name))
                .constructionMaterials(OriginalMachineMaterialData.weights(name, 1))
                .build();
    }
}
