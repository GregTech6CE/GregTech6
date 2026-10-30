package gregtech.content.multiblock;

import gregtech.api.energy.EnergyType;
import gregtech.api.machine.BasicMachineParameters;
import gregtech.api.machine.MachineFace;
import gregtech.content.machine.HostMachineFaces;
import gregtech.content.material.Materials;

/** Original dedicated-controller parameters omitted from the general basic-machine catalog. */
public final class OriginalMultiblockMachineParameters {
    private OriginalMultiblockMachineParameters() {}

    public static BasicMachineParameters distillationTower() {
        return BasicMachineParameters.builder("distillation_tower_main", Materials.StainlessSteel)
                .machineType("distillationtower")
                .energy(EnergyType.HU, 512)
                .tier(1)
                .energyRange(1, 1024)
                .parallel(1)
                .strength(6, 6)
                .faces(HostMachineFaces.fromMask(
                        63, 63, 63, 63, 63, 0, -1, MachineFace.BACK.index(), -1, MachineFace.BACK.index()))
                .constructionMaterials(
                        gregtech.content.machine.OriginalMachineMaterialData.weights("distillationtower", 1))
                .build();
    }

    public static BasicMachineParameters cryoDistillationTower() {
        return BasicMachineParameters.builder("cryo_distillation_main", Materials.StainlessSteel)
                .machineType("cryodistillationtower")
                .energy(EnergyType.CU, 512)
                .tier(1)
                .energyRange(1, 1024)
                .parallel(1)
                .strength(6, 6)
                .faces(HostMachineFaces.fromMask(
                        63, 63, 63, 63, 63, 0, -1, MachineFace.BACK.index(), -1, MachineFace.BACK.index()))
                .constructionMaterials(
                        gregtech.content.machine.OriginalMachineMaterialData.weights("cryodistillationtower", 1))
                .build();
    }

    public static BasicMachineParameters cokeOven() {
        return BasicMachineParameters.builder("coke_oven_main", Materials.Ceramic)
                .machineType("cokeoven")
                .energy(EnergyType.TU, 1)
                .energyRange(1, 16)
                .parallel(16)
                .strength(5, 5)
                .faces(HostMachineFaces.fromMask(2, 1, 0, 61, 63, 0, -1, -1, -1, MachineFace.BOTTOM.index()))
                .constructionMaterials(gregtech.content.machine.OriginalMachineMaterialData.weights("cokeoven", 1))
                .build();
    }

    public static BasicMachineParameters fusionReactor() {
        return OriginalAdvancedControllerData.parameters("fusionreactor", "fusion_reactor_main");
    }

    public static BasicMachineParameters implosionCompressor() {
        return OriginalAdvancedControllerData.parameters("implosioncompressor", "implosion_compressor_main");
    }
}
