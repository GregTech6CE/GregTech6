/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * Adapted from Loader_MultiTileEntities.metalset and CR.DEF_REV. */
package com.gregtech.gregtech.content.machine;

import com.gregtech.gregtech.api.material.*;
import com.gregtech.gregtech.content.transport.HopperCatalog;
import com.gregtech.gregtech.data.generated.GT6Materials;
import java.util.*;

/** Source assembly data for stable block identities, independent of the native loader. */
public final class MachineConstructionMaterials {
    private MachineConstructionMaterials() {}
    private static final Map<String, ItemComposition> HOPPERS = new HashMap<>();
    static {
        for (var entry : HopperCatalog.ALL) {
            // PwP/XCX/ Xh and PCP/XCX/wXh: two flat + three curved plates,
            // and one/two craftingChest inputs whose automatic data is 4U Wood each.
            HOPPERS.put(entry.spec().id(), ReversibleCraftingData.perItem(List.of(
                    MaterialComponent.of(entry.spec().material(), GTValues.U * 5),
                    MaterialComponent.of(GT6Materials.Woods.Wood, GTValues.U * (entry.queue() ? 8 : 4))),
                    1, "GT6 Loader_MultiTileEntities metalset known CR.REV hopper inputs"));
        }
    }
    public static Optional<ItemComposition> block(String path) {
        var hopper = HOPPERS.get(path);
        if (hopper != null) return Optional.of(hopper);
        return OriginalMachineMaterialData.block(path);
    }
    public static Map<String, ItemComposition> blocks() {
        var blocks = new HashMap<>(OriginalMachineMaterialData.blocks());
        blocks.putAll(HOPPERS);
        return Map.copyOf(blocks);
    }
}
