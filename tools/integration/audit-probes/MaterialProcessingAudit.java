import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.mod.ModData;
import com.gregtech.gregtech.content.recipe.generated.GTMaterialFields;
import com.gregtech.gregtech.data.generated.MaterialProcessingTargets;
import com.gregtech.gregtech.data.generated.MaterialWorkability;

class MaterialProcessingAudit {
    public static void main(String[] args) {
        ModData.bindPresence(id -> id.equals("minecraft") || id.equals("gt6ce"));
        GTMaterialRegistry.init();
        GTMaterialRegistry.postInit();
        for (String field : new String[]{"MeatRaw", "MeatCooked", "FishRaw", "Cheese", "Chocolate",
                "NaCl", "Sn", "Pb", "Au", "Paper", "Wood", "SiO2"}) {
            GTMaterial material = GTMaterialRegistry.get(GTMaterialFields.materialOf(field));
            if (material == null || !material.isValid()) throw new AssertionError(field);
            System.out.println("FLAGS|" + field + "|" + material.getName()
                    + "|FOOD=" + MaterialWorkability.isFood(material)
                    + "|MORTAR=" + MaterialWorkability.isMortarGrindable(material)
                    + "|FURNACE=" + MaterialWorkability.isFurnace(material)
                    + "|BRITTLE=" + MaterialWorkability.isBrittle(material)
                    + "|CRYSTALLISABLE=" + MaterialWorkability.isCrystallisable(material));
        }
        for (String field : new String[]{"WoodTreated", "WoodPolished", "Brick", "TungstenSintered"}) {
            String name = GTMaterialFields.materialOf(field);
            for (String kind : new String[]{"Forging", "Compressing"}) {
                String[] target = MaterialProcessingTargets.of(name, kind);
                System.out.println("TARGET|" + field + "|" + name + "|" + kind + "|"
                        + (target == null ? "DEFAULT_SELF_1U" : String.join(",", target)));
            }
        }
    }
}
