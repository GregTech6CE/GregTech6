package gregtech.content.transport;

import gregtech.content.material.Materials;
import gregtech.internal.machine.ItemPipeSpec;
import gregtech.internal.material.GTMaterial;
import gregtech.content.data.generated.GT6Materials;
import gregtech.content.registry.*;

/** Built-in GT6 definitions; registration IDs and ordering are preserved. */
public final class ItemPipeDefinitions {
    private ItemPipeDefinitions() {}

    // ==================== Item Pipes ====================

    public static void register() {
        gregtech.internal.addon.AddonMachines.collectContributors();
        for (ItemPipeCatalog.ItemPipeMat mat : ItemPipeCatalog.mats()) {
            for (ItemPipeSpec.ItemPipeSize size : ItemPipeSpec.ItemPipeSize.values()) {
                String sizeKey = size.name().toLowerCase();
                String id = "item_pipe_" + sizeKey + "_" + mat.idSuffix();
                String owner = gregtech.internal.addon.AddonPipes.owner(id);
                if (owner != null) {
                    gregtech.content.registry.AddonPipeRegistration.queueItem(
                            owner, id, mat.material(), size, mat.stepSize(), mat.invSize(), true);
                    continue;
                }
                var block = GTItemPipes.register(id, mat.material(), size, mat.stepSize(), mat.invSize(), true);
                if (mat.idSuffix().equals("brass") && size == ItemPipeSpec.ItemPipeSize.MEDIUM) {
                    GTItemPipes.PIPE_MEDIUM_BRASS = block;
                }
            }
        }
    }

}
