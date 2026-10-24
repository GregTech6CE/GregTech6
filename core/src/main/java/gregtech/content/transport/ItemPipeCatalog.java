package gregtech.content.transport;

import gregtech.internal.material.GTMaterial;

import java.util.ArrayList;
import java.util.List;

/** Built-in GT6 definitions; registration IDs and ordering are preserved. */
public final class ItemPipeCatalog {
    private ItemPipeCatalog() {}

    // ==================== Item Pipes ====================

    public record ItemPipeMat(String idSuffix, GTMaterial material, long stepSize, int invSize) {}

    public static final ItemPipeMat[] ITEM_PIPE_MATS = {
            new ItemPipeMat("brass",            gregtech.content.material.generated.CompoundMaterials.Brass,           32768, 1),
            new ItemPipeMat("constantan",       gregtech.content.material.generated.CompoundMaterials.Constantan,      32768, 1),
            new ItemPipeMat("cobalt_brass",     gregtech.content.material.generated.CompoundMaterials.CobaltBrass,     32768, 1),
            new ItemPipeMat("germanium",        gregtech.content.material.generated.ElementMaterials.Germanium,        32768, 1),
            new ItemPipeMat("arsenic_copper",   gregtech.content.material.generated.CompoundMaterials.ArsenicCopper,   16384, 1),
            new ItemPipeMat("arsenic_bronze",   gregtech.content.material.generated.CompoundMaterials.ArsenicBronze,   32768, 2),
            new ItemPipeMat("electrum",         gregtech.content.material.generated.CompoundMaterials.Electrum,        16384, 2),
            new ItemPipeMat("sterling_silver",  gregtech.content.material.generated.CompoundMaterials.SterlingSilver,  16384, 2),
            new ItemPipeMat("rose_gold",        gregtech.content.material.generated.CompoundMaterials.RoseGold,        16384, 2),
            new ItemPipeMat("angmallen",        gregtech.content.material.generated.CompoundMaterials.Angmallen,       16384, 2),
            new ItemPipeMat("black_bronze",     gregtech.content.material.generated.CompoundMaterials.BlackBronze,     16384, 2),
            new ItemPipeMat("aluminium_brass",  gregtech.content.material.generated.CompoundMaterials.AluminiumBrass,  16384, 2),
            new ItemPipeMat("manyullyn",        gregtech.content.material.generated.CompoundMaterials.Manyullyn,       16384, 2),
            new ItemPipeMat("magnalium",        gregtech.content.material.generated.CompoundMaterials.Magnalium,       16384, 2),
            new ItemPipeMat("platinum",         gregtech.content.material.generated.ElementMaterials.Platinum,         8192,  4),
            new ItemPipeMat("osmium",           gregtech.content.material.generated.ElementMaterials.OsmiumElemental,  4096,  8),
            new ItemPipeMat("enderium",         gregtech.content.material.generated.CompoundMaterials.Enderium,        2048,  16),
            new ItemPipeMat("ultimet",          gregtech.content.material.generated.CompoundMaterials.Ultimet,         2048,  16),
            new ItemPipeMat("elven_elementium", gregtech.content.material.generated.CompoundMaterials.ElvenElementium, 2048,  16),
            new ItemPipeMat("osmiridium",       gregtech.content.material.generated.CompoundMaterials.Osmiridium,      1024,  32),
            new ItemPipeMat("vibranium_silver", gregtech.content.material.generated.CompoundMaterials.VibraniumSilver, 64,    512),
    };

    /** Built-in rows, then addon rows in addon-id order. */
    public static List<ItemPipeMat> mats() {
        var rows = new ArrayList<ItemPipeMat>();
        java.util.Collections.addAll(rows, ITEM_PIPE_MATS);
        for (var line : gregtech.internal.addon.AddonPipes.itemLines())
            rows.add(new ItemPipeMat(line.idSuffix(), line.material(), line.stepSize(), line.invSize()));
        return List.copyOf(rows);
    }

}
