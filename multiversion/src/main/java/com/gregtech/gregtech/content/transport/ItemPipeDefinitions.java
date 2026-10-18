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
        for (ItemPipeCatalog.ItemPipeMat mat : ItemPipeCatalog.ITEM_PIPE_MATS) {
            for (ItemPipeSpec.ItemPipeSize size : ItemPipeSpec.ItemPipeSize.values()) {
                String sizeKey = size.name().toLowerCase();
                String id = "item_pipe_" + sizeKey + "_" + mat.idSuffix();
                var block = GTItemPipes.register(id, mat.material(), size, mat.stepSize(), mat.invSize(), true);
                if (mat.idSuffix().equals("brass") && size == ItemPipeSpec.ItemPipeSize.MEDIUM) {
                    GTItemPipes.PIPE_MEDIUM_BRASS = block;
                }
            }
        }
    }

}
