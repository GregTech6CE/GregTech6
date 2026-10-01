package com.gregtech.gregtech.content.transport;

import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.api.machine.ItemPipeSpec;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.data.generated.GT6Materials;

/** Built-in GT6 definitions; registration IDs and ordering are preserved. */
public final class ItemPipeCatalog {
    private ItemPipeCatalog() {}

    // ==================== Item Pipes ====================

    public record ItemPipeMat(String idSuffix, GTMaterial material, long stepSize, int invSize) {}

    public static final ItemPipeMat[] ITEM_PIPE_MATS = {
            new ItemPipeMat("brass",                com.gregtech.gregtech.content.material.generated.CompoundMaterials.Brass,           32768, 1),
            new ItemPipeMat("constantan",           com.gregtech.gregtech.content.material.generated.CompoundMaterials.Constantan,      32768, 1),
            new ItemPipeMat("cobalt_brass",         com.gregtech.gregtech.content.material.generated.CompoundMaterials.CobaltBrass,     32768, 1),
            new ItemPipeMat("germanium",            com.gregtech.gregtech.content.material.generated.ElementMaterials.Germanium,               32768, 1),
            new ItemPipeMat("arsenic_copper",       com.gregtech.gregtech.content.material.generated.CompoundMaterials.ArsenicCopper,   16384, 1),
            new ItemPipeMat("arsenic_bronze",       com.gregtech.gregtech.content.material.generated.CompoundMaterials.ArsenicBronze,   32768, 2),
            new ItemPipeMat("electrum",             com.gregtech.gregtech.content.material.generated.CompoundMaterials.Electrum,         16384, 2),
            new ItemPipeMat("sterling_silver",      com.gregtech.gregtech.content.material.generated.CompoundMaterials.SterlingSilver,   16384, 2),
            new ItemPipeMat("rose_gold",            com.gregtech.gregtech.content.material.generated.CompoundMaterials.RoseGold,         16384, 2),
            new ItemPipeMat("angmallen",            com.gregtech.gregtech.content.material.generated.CompoundMaterials.Angmallen,        16384, 2),
            new ItemPipeMat("black_bronze",         com.gregtech.gregtech.content.material.generated.CompoundMaterials.BlackBronze,      16384, 2),
            new ItemPipeMat("aluminium_brass",      com.gregtech.gregtech.content.material.generated.CompoundMaterials.AluminiumBrass,   16384, 2),
            new ItemPipeMat("manyullyn",            com.gregtech.gregtech.content.material.generated.CompoundMaterials.Manyullyn,        16384, 2),
            new ItemPipeMat("magnalium",            com.gregtech.gregtech.content.material.generated.CompoundMaterials.Magnalium,        16384, 2),
            new ItemPipeMat("platinum",             com.gregtech.gregtech.content.material.generated.ElementMaterials.Platinum,               8192,  4),
            new ItemPipeMat("osmium",               com.gregtech.gregtech.content.material.generated.ElementMaterials.OsmiumElemental,               4096,  8),
            new ItemPipeMat("enderium",             com.gregtech.gregtech.content.material.generated.CompoundMaterials.Enderium,         2048, 16),
            new ItemPipeMat("ultimet",              com.gregtech.gregtech.content.material.generated.CompoundMaterials.Ultimet,          2048, 16),
            new ItemPipeMat("elven_elementium",     com.gregtech.gregtech.content.material.generated.CompoundMaterials.ElvenElementium,  2048, 16),
            new ItemPipeMat("osmiridium",           com.gregtech.gregtech.content.material.generated.CompoundMaterials.Osmiridium,       1024, 32),
            new ItemPipeMat("vibranium_silver",     com.gregtech.gregtech.content.material.generated.CompoundMaterials.VibraniumSilver,   64, 512),
    };

}
