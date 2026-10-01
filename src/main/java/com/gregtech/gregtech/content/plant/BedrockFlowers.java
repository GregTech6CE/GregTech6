package com.gregtech.gregtech.content.plant;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.content.material.generated.WoodMaterials;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** GT6 BlockFlowersA/B metadata, kept separate from bedrock ore selection and processing recipes. */
public final class BedrockFlowers {
    public record Flower(String id, char family, int meta, String indicator,
                         boolean occursInRealLife, @Nullable GTMaterial material) {
        public boolean desert() { return family == 'B'; }
    }

    // Only five originals call OM.data; an indicator is not made of the ore it indicates.
    public static final List<Flower> ALL = List.of(
            a("flower_altered_andesite_buckwheat", 0, "gold", true, Materials.Wheat),
            a("flower_crosby_buckwheat", 1, "silver", true, Materials.Wheat),
            a("flower_alpine_catchfly", 2, "copper", true, null),
            a("flower_viola_calaminaria", 3, "zinc", true, null),
            a("flower_thlaspi_lereschianum", 4, "nickel", true, null),
            a("flower_tufted_evening_primrose", 5, "uranium", true, null),
            a("flower_narcissus_sheldonia", 6, "platinum", false, null),
            a("flower_orechid", 7, "ore", false, null),
            a("flower_hexalily", 8, "hexorium", false, null),
            b("flower_sagebrush", 0, "arsenic", true, WoodMaterials.Acacia),
            b("flower_four_wing_saltbush", 1, "antimony", true, WoodMaterials.Acacia),
            b("flower_desert_trumpet", 2, "gold", true, null),
            b("flower_copper_plant", 3, "copper", true, null),
            b("flower_prince_s_plume", 4, "redstone", true, null),
            b("flower_thompsons_locoweed", 5, "uranium", true, null),
            b("flower_pandanus_candelabrum", 6, "diamond", true, WoodMaterials.Palm),
            b("flower_tungstus", 7, "tungsten", false, null));
    private static final Map<String, Flower> BY_ID = index();

    private BedrockFlowers() {}

    @Nullable
    public static Flower byId(String id) { return BY_ID.get(id); }

    private static Flower a(String id, int meta, String indicator, boolean real, GTMaterial material) {
        return new Flower(id, 'A', meta, indicator, real, material);
    }

    private static Flower b(String id, int meta, String indicator, boolean real, GTMaterial material) {
        return new Flower(id, 'B', meta, indicator, real, material);
    }

    private static Map<String, Flower> index() {
        Map<String, Flower> result = new LinkedHashMap<>();
        for (Flower flower : ALL) {
            if (result.put(flower.id(), flower) != null)
                throw new IllegalStateException("Duplicate GT6 indicator flower " + flower.id());
        }
        return Map.copyOf(result);
    }
}
