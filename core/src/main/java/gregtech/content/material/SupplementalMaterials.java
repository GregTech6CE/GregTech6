package gregtech.content.material;

import gregtech.api.material.*;
import gregtech.engine.material.*;
import gregtech.content.material.generated.CompoundMaterials;
import gregtech.content.material.generated.OreMaterials;
import gregtech.content.data.ModReferences;

import java.util.Set;

/** Additional built-in materials kept outside generated original-material catalogs. */
public final class SupplementalMaterials {
    private static GTMaterial porcelain;
    private static GTMaterial sculk;
    private static final Set<String> SCULK_FORMS = Set.of("DUSTS", "PLANTS", "PROJECTILES", "STICKS");
    private SupplementalMaterials() {}
    public static void declare() {
        modernStone(9900, "Tuff", "Tuff", 0x696D6E, MaterialTextureSet.STONE);
        var clayBrick = CompoundMaterials.ClayBrick;
        modernStone(9901, "MudBrick", "Mud Brick", 0x93704F, MaterialTextureSet.BRICK)
                .setStats(clayBrick.getMeltingPoint(), clayBrick.getBoilingPoint(), clayBrick.getDensity());
        modernStone(9902, "RedNetherBrick", "Red Nether Brick", 0x6A1317, MaterialTextureSet.BRICK);
        modernStone(9903, "Purpur", "Purpur", 0xAA7AAA, MaterialTextureSet.BRICK);
        GTMaterialRegistry.registerDefinition(MaterialDefinition.builder(9223, "Blackstone").color(0x1E1414)
                .texture(MaterialTextureSet.FINE).dust().build());
        // Modern Minecraft material: use the same physical defaults as Blaze, not its identity.
        var blaze = CompoundMaterials.Blaze;
        sculk = GTMaterialRegistry.registerDefinition(MaterialDefinition.builder(8472, "Sculk").color(0x111B21)
                .texture(blaze.getTextureSet()).meltingPointKelvin(blaze.getMeltingPoint())
                .boilingPointKelvin(blaze.getBoilingPoint()).density(blaze.getDensity())
                .properties(blaze.getProperties().toArray(MaterialProperty[]::new)).build())
                .mod(ModReferences.MC);
        var ceramic = CompoundMaterials.Ceramic;
        var silica = OreMaterials.SiliconDioxide;
        var feldspar = OreMaterials.PotassiumFeldspar;
        porcelain = GTMaterialRegistry.registerDefinition(MaterialDefinition.builder(8273, "Porcelain").color(0xEBEBF5)
                .texture(MaterialTextureSet.FINE).meltingPointKelvin(1800).boilingPointKelvin(3600)
                .density((ceramic.getDensity() * 2 + silica.getDensity() + feldspar.getDensity()) / 4)
                .dust().properties(MaterialProperty.GENERATE_PLATE).build());
        porcelain.setComposition(4, MaterialComponent.of(ceramic, 2 * GTValues.U),
                MaterialComponent.of(silica, GTValues.U), MaterialComponent.of(feldspar, GTValues.U));
    }
    /** Called after the original tool-stat inheritance pass has finalized Blaze. */
    public static void link() {
        var blaze = CompoundMaterials.Blaze;
        sculk.setToolStats(blaze.getToolTypes(), blaze.getToolSpeed(),
                blaze.getToolDurability(), blaze.getToolQuality());
    }

    /** Port-only forms are explicit overrides, not rows claimed to come from original GT6. */
    public static Set<String> forms(GTMaterial material) {
        return material != null && material == sculk ? SCULK_FORMS : Set.of();
    }

    public static GTMaterial sculk() { GTMaterialRegistry.init(); return sculk; }
    private static GTMaterial modernStone(int id, String name, String displayName, int color, MaterialTextureSet texture) {
        return GTMaterialRegistry.registerDefinition(MaterialDefinition.builder(id, name).displayName(displayName)
                .color(color).texture(texture).meltingPointKelvin(1000).boilingPointKelvin(3000)
                .dust().properties(MaterialProperty.STONE, MaterialProperty.GENERATE_PLATE,
                        MaterialProperty.GENERATE_STICKS, MaterialProperty.GENERATE_PLANT,
                        MaterialProperty.GENERATE_PROJECTILE).build());
    }
    public static GTMaterial porcelain() { GTMaterialRegistry.init(); return porcelain; }
}
