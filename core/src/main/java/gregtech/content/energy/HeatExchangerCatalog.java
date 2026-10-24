/* Gregorius Techneticies / GregTech-6 Team, LGPL-3.0-or-later.
 * Loader_MultiTileEntities 9103–9159 and MultiTileEntityGeneratorHotFluid. */
package gregtech.content.energy;

import gregtech.api.material.GTMaterial;
import gregtech.data.ImportedMaterialData;
import gregtech.content.multiblock.HeatExchangerRules;
import java.util.List;

/** Original single-block exchangers; the large exchanger has its own structure. */
public final class HeatExchangerCatalog {
    private HeatExchangerCatalog() {}
    public record Spec(String id, int sourceId, GTMaterial material, int rate, int efficiency, float hardness) {
        public boolean dense() { return sourceId >= 9150; }
        public HeatExchangerRules.Settings settings() {
            return new HeatExchangerRules.Settings(rate, efficiency, "gt.recipe.fuels.hot", gregtech.data.GregTechTags.Energy.HU.getId());
        }
        public gregtech.api.material.ItemComposition composition() {
            long unit = gregtech.api.material.GTValues.U;
            return new gregtech.api.material.ItemComposition(null, List.of(
                    gregtech.api.material.MaterialComponent.of(material, (dense() ? 26 : 8) * unit),
                    gregtech.api.material.MaterialComponent.of(ImportedMaterialData.Pb, (dense() ? 16 : 4) * unit),
                    gregtech.api.material.MaterialComponent.of(ImportedMaterialData.Cu, (dense() ? 12 : 2) * unit),
                    gregtech.api.material.MaterialComponent.of(material == ImportedMaterialData.Invar ? ImportedMaterialData.Cu : ImportedMaterialData.AnnealedCopper, (dense() ? 9 : 2) * unit)),
                    "GT6 Loader_MultiTileEntities " + sourceId + " CR.REV", true);
        }
    }
    public static final List<Spec> ALL = List.of(
            new Spec("heat_exchanger_invar",                9103, ImportedMaterialData.Invar,         16, 10000, 4.0F),
            new Spec("heat_exchanger_tungsten",             9107, ImportedMaterialData.W,            128, 10000, 10.0F),
            new Spec("heat_exchanger_tungsten_steel",       9108, ImportedMaterialData.TungstenSteel,128,  9000, 12.5F),
            new Spec("heat_exchanger_ta4hfc5",              9109, ImportedMaterialData.Ta4HfC5,      256, 10000, 12.5F),
            new Spec("heat_exchanger_dense_invar",          9153, ImportedMaterialData.Invar,        64, 10000, 4.0F),
            new Spec("heat_exchanger_dense_tungsten",       9157, ImportedMaterialData.W,           512, 10000, 10.0F),
            new Spec("heat_exchanger_dense_tungsten_steel", 9158, ImportedMaterialData.TungstenSteel,512,  9000, 12.5F),
            new Spec("heat_exchanger_dense_ta4hfc5",        9159, ImportedMaterialData.Ta4HfC5,     1024, 10000, 12.5F));
}
