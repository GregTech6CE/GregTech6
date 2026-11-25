package gregtech.content.transport.pipe.item;

import gregtech.content.registry.AddonPipeRegistration;
import gregtech.content.registry.ContentRegistrar;
import gregtech.content.registry.GTItemPipes;
import gregtech.content.transport.ItemPipeCatalog;
import gregtech.engine.machine.ItemPipeSpec;

/** Built-in GT6 definitions; registration IDs and ordering are preserved. */
public final class ItemPipeDefinitions {
    private ItemPipeDefinitions() {}

    // ==================== Item Pipes ====================

    public static void register(ContentRegistrar registrar) {
        gregtech.engine.addon.AddonMachines.collectContributors();
        for (ItemPipeCatalog.ItemPipeMat mat : ItemPipeCatalog.mats()) {
            for (ItemPipeSpec.ItemPipeSize size : ItemPipeSpec.ItemPipeSize.values()) {
                String sizeKey = size.name().toLowerCase();
                String id = "item_pipe_" + sizeKey + "_" + mat.idSuffix();
                String owner = gregtech.engine.addon.AddonPipes.owner(id);
                if (owner != null) {
                    AddonPipeRegistration.queueItem(
                            registrar, owner, id, mat.material(), size, mat.stepSize(), mat.invSize(), true);
                    continue;
                }
                var block =
                        GTItemPipes.register(registrar, id, mat.material(), size, mat.stepSize(), mat.invSize(), true);
                if (mat.idSuffix().equals("brass") && size == ItemPipeSpec.ItemPipeSize.MEDIUM) {
                    GTItemPipes.PIPE_MEDIUM_BRASS = block;
                }
            }
        }
    }
}
