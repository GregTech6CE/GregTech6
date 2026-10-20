package com.gregtech.gregtech.content.transport;

import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.api.machine.ItemPipeSpec;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.data.generated.GT6Materials;
import com.gregtech.gregtech.registry.*;

/** Built-in GT6 definitions; registration IDs and ordering are preserved. */
public final class ItemPipeDefinitions {
    private ItemPipeDefinitions() {}

    // ==================== Item Pipes ====================

    public static void register() {
        com.gregtech.gregtech.api.addon.AddonMachines.collectContributors();
        for (ItemPipeCatalog.ItemPipeMat mat : ItemPipeCatalog.mats()) {
            for (ItemPipeSpec.ItemPipeSize size : ItemPipeSpec.ItemPipeSize.values()) {
                String sizeKey = size.name().toLowerCase();
                String id = "item_pipe_" + sizeKey + "_" + mat.idSuffix();
                String owner = com.gregtech.gregtech.api.addon.AddonPipes.owner(id);
                if (owner != null) {
                    com.gregtech.gregtech.registry.AddonPipeRegistration.queueItem(
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
