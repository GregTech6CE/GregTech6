package gregtech.content.multiblock;

import gregtech.content.machine.BasicMachineCatalog;
import gregtech.engine.machine.BasicMachineParameters;

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
