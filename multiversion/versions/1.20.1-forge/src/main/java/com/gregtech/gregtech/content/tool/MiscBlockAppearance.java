package com.gregtech.gregtech.content.tool;

import com.gregtech.gregtech.content.material.Materials;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

/** Material colors for the restored misc models, shared by world and inventory handlers. */
public final class MiscBlockAppearance {
    private MiscBlockAppearance() {}
    public static int tint(Block block) {
        if (block instanceof com.gregtech.gregtech.block.misc.SourceExtenderBlock extender) return extender.spec().material().getColor();
        if(block instanceof com.gregtech.gregtech.block.misc.LongDistanceTransformerBlock endpoint) {
            String material=switch((int)endpoint.voltage()) {
                case 2048 -> "Chromium"; case 8192 -> "Titanium"; case 32768 -> "Iridium";
                case 131072 -> "Osmium"; default -> "Trinitanium";
            };
            return com.gregtech.gregtech.api.material.GTMaterialRegistry.get(material).getColor();
        }
        var key=ForgeRegistries.BLOCKS.getKey(block);
        if(key==null) return 0xFFFFFF;
        String id=key.getPath();
        if(id.endsWith("_aluminium")) return Materials.Aluminium.getColor();
        if(id.endsWith("_titanium")) return Materials.Titanium.getColor();
        if(id.endsWith("_tungsten")) return Materials.Tungsten.getColor();
        if(id.endsWith("_ultimet")) return Materials.Ultimet.getColor();
        if(id.endsWith("_stainless") || id.equals("extender_elite") || id.equals("extender_wireless")) return Materials.StainlessSteel.getColor();
        if(id.startsWith("filter_")) return Materials.SteelGalvanized.getColor();
        if(id.equals("advanced_crafting_table")) return com.gregtech.gregtech.content.material.generated.WoodMaterials.Wood.getColor();
        return Materials.Steel.getColor();
    }
}
