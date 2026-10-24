package gregtech.content.recipe;
import gregtech.api.material.GTMaterial;
import gregtech.api.material.GTMaterialRegistry;
import gregtech.api.addon.AddonMaterials;
/** Bounded baseline ORES-property proof. Not a universal recipe or external-form prohibition.
 * 53 reviewed negatives from54 target-link candidates; Sand and all other identities UNKNOWN.
 * Evidence: docs/integration/nonore-input-source-profile.json; paired declaration references and source hashes.
 * Factory/group/postmutation tracing, never table absence, establishes these bounded decisions.
 */
public final class ReviewedOreInputProfile {
    public enum Evidence { YES, NO, UNKNOWN }
    private ReviewedOreInputProfile() {}
    public static Evidence decision(GTMaterial material) {
        if(material==null)return Evidence.UNKNOWN;
        material=material.resolve();
        if(!material.isValid()||!GTMaterialRegistry.isCoreMaterial(material)||AddonMaterials.owns(material))return Evidence.UNKNOWN;
        String name=material.getName();
        // A supported ore-form request changes this material's baseline. Defer, do not infer ORES YES.
        // Otherwise allowing only the requested crushed input could strand its old purified outputs.
        if(AddonMaterials.acceptedForms().stream().anyMatch(f->f.materialName().equals(name)
                &&java.util.Set.of("oreRaw","crushed","crushedTiny","crushedPurified","crushedPurifiedTiny",
                    "crushedCentrifuged","crushedCentrifugedTiny").contains(f.prefixName())))return Evidence.UNKNOWN;
        return switch(material.getId()) {
        case 8366 -> name.equals("ArcaneCompound") ? Evidence.NO : Evidence.UNKNOWN;
        case 8198 -> name.equals("Bakelite") ? Evidence.NO : Evidence.UNKNOWN;
        case 8418 -> name.equals("Bamboo") ? Evidence.NO : Evidence.UNKNOWN;
        case 8275 -> name.equals("Bark") ? Evidence.NO : Evidence.UNKNOWN;
        case 8106 -> name.equals("BlackEndSand") ? Evidence.NO : Evidence.UNKNOWN;
        case 8620 -> name.equals("Brass") ? Evidence.NO : Evidence.UNKNOWN;
        case 8610 -> name.equals("Bronze") ? Evidence.NO : Evidence.UNKNOWN;
        case 8501 -> name.equals("Concrete") ? Evidence.NO : Evidence.UNKNOWN;
        case 8290 -> name.equals("Dreamwood") ? Evidence.NO : Evidence.UNKNOWN;
        case 8751 -> name.equals("Duranium") ? Evidence.NO : Evidence.UNKNOWN;
        case 9191 -> name.equals("Eclogite") ? Evidence.NO : Evidence.UNKNOWN;
        case 8504 -> name.equals("Endstone") ? Evidence.NO : Evidence.UNKNOWN;
        case 8035 -> name.equals("FerricOxyhydroxide") ? Evidence.NO : Evidence.UNKNOWN;
        case 8698 -> name.equals("Fireleaf") ? Evidence.NO : Evidence.UNKNOWN;
        case 9712 -> name.equals("FishRaw") ? Evidence.NO : Evidence.UNKNOWN;
        case 8002 -> name.equals("Flint") ? Evidence.NO : Evidence.UNKNOWN;
        case 90 -> name.equals("Fluorine") ? Evidence.NO : Evidence.UNKNOWN;
        case 8606 -> name.equals("GildedIron") ? Evidence.NO : Evidence.UNKNOWN;
        case 9175 -> name.equals("Graphene") ? Evidence.NO : Evidence.UNKNOWN;
        case 8296 -> name.equals("Greatwood") ? Evidence.NO : Evidence.UNKNOWN;
        case 8199 -> name.equals("HardPlastic") ? Evidence.NO : Evidence.UNKNOWN;
        case 8522 -> name.equals("Holystone") ? Evidence.NO : Evidence.UNKNOWN;
        case 9802 -> name.equals("Ice") ? Evidence.NO : Evidence.UNKNOWN;
        case 8644 -> name.equals("IronCompressed") ? Evidence.NO : Evidence.UNKNOWN;
        case 8645 -> name.equals("IronMagnetic") ? Evidence.NO : Evidence.UNKNOWN;
        case 8241 -> name.equals("Leather") ? Evidence.NO : Evidence.UNKNOWN;
        case 9189 -> name.equals("Limestone") ? Evidence.NO : Evidence.UNKNOWN;
        case 8289 -> name.equals("Livingwood") ? Evidence.NO : Evidence.UNKNOWN;
        case 8506 -> name.equals("Marble") ? Evidence.NO : Evidence.UNKNOWN;
        case 9700 -> name.equals("MeatRaw") ? Evidence.NO : Evidence.UNKNOWN;
        case 8650 -> name.equals("MeteoricSteel") ? Evidence.NO : Evidence.UNKNOWN;
        case 8647 -> name.equals("NeodymiumMagnetic") ? Evidence.NO : Evidence.UNKNOWN;
        case 8502 -> name.equals("Netherrack") ? Evidence.NO : Evidence.UNKNOWN;
        case 8214 -> name.equals("Obsidian") ? Evidence.NO : Evidence.UNKNOWN;
        case 8218 -> name.equals("Plastic") ? Evidence.NO : Evidence.UNKNOWN;
        case 8197 -> name.equals("PVC") ? Evidence.NO : Evidence.UNKNOWN;
        case 8509 -> name.equals("Redrock") ? Evidence.NO : Evidence.UNKNOWN;
        case 8104 -> name.equals("RedSand") ? Evidence.NO : Evidence.UNKNOWN;
        case 8217 -> name.equals("Rubber") ? Evidence.NO : Evidence.UNKNOWN;
        case 8414 -> name.equals("Shimmerwood") ? Evidence.NO : Evidence.UNKNOWN;
        case 8297 -> name.equals("Silverwood") ? Evidence.NO : Evidence.UNKNOWN;
        case 8291 -> name.equals("Skyroot") ? Evidence.NO : Evidence.UNKNOWN;
        case 9801 -> name.equals("Snow") ? Evidence.NO : Evidence.UNKNOWN;
        case 8673 -> name.equals("Steeleaf") ? Evidence.NO : Evidence.UNKNOWN;
        case 8651 -> name.equals("SteelGalvanized") ? Evidence.NO : Evidence.UNKNOWN;
        case 8646 -> name.equals("SteelMagnetic") ? Evidence.NO : Evidence.UNKNOWN;
        case 8196 -> name.equals("Teflon") ? Evidence.NO : Evidence.UNKNOWN;
        case 8752 -> name.equals("Tritanium") ? Evidence.NO : Evidence.UNKNOWN;
        case 8027 -> name.equals("TungsticAcid") ? Evidence.NO : Evidence.UNKNOWN;
        case 8286 -> name.equals("Weedwood") ? Evidence.NO : Evidence.UNKNOWN;
        case 8105 -> name.equals("WhiteEndSand") ? Evidence.NO : Evidence.UNKNOWN;
        case 8221 -> name.equals("Wood") ? Evidence.NO : Evidence.UNKNOWN;
        case 8224 -> name.equals("WoodRubber") ? Evidence.NO : Evidence.UNKNOWN;
        default -> Evidence.UNKNOWN;
        };
    }
}
