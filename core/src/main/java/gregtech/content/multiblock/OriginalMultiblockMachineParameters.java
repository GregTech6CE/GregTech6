package gregtech.content.multiblock;

import gregtech.api.machine.BasicMachineParameters;
import gregtech.content.machine.BasicMachineCatalog;

/** Original dedicated-controller parameters omitted from the general basic-machine catalog. */
public final class OriginalMultiblockMachineParameters {
    private OriginalMultiblockMachineParameters() {}

    public static BasicMachineParameters distillationTower() {
        return BasicMachineCatalog.controller("distillationtower");
    }

    public static BasicMachineParameters cryoDistillationTower() {
        return BasicMachineCatalog.controller("cryodistillationtower");
    }

    public static BasicMachineParameters cokeOven() {
        return BasicMachineCatalog.controller("cokeoven");
    }

    public static BasicMachineParameters fusionReactor() {
        return BasicMachineCatalog.controller("fusionreactor");
    }

    public static BasicMachineParameters implosionCompressor() {
        return BasicMachineCatalog.controller("implosioncompressor");
    }
}
