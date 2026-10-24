package gregtech.content.energy;
import gregtech.internal.energy.EnergyNodeSpec;
import gregtech.internal.energy.EnergyNodeSpec.Kind;
import gregtech.internal.material.GTMaterial;
import gregtech.content.data.GregTechTags;
import java.util.ArrayList;
import java.util.List;

import gregtech.content.material.Materials;
/** Built-in energy conversion/storage specifications, independent of Forge registration. */
public final class EnergyNodeDefinitions {
    private EnergyNodeDefinitions() {}
    public static List<EnergyNodeSpec> specifications() {
        List<EnergyNodeSpec> result = new ArrayList<>();
        // tier name → (voltage, material). GT6 registers these machines for VN[1..5] = LV..IV
        // with MT.DATA.Electric_T[1..5]; GT6's Electric_T is
        // {TinAlloy(ULV), SteelGalvanized(LV), Al(MV), StainlessSteel(HV), Cr(EV), Ti(IV), ...},
        // so LV is galvanized steel and ULV (tin alloy) has no basic machine of its own.
        record Tier(String name, long inputRate, GTMaterial material) {
        }
        Tier[] tiers = {
                new Tier("lv", 32,   Materials.SteelGalvanized),
                new Tier("mv", 128,  Materials.Aluminium),
                new Tier("hv", 512,  Materials.StainlessSteel),
                new Tier("ev", 2048, Materials.Chromium),
                new Tier("iv", 8192, Materials.Titanium),
        };

        // Electric motors: EU in → RU out at half rate (GT6 10021-10025)
        long[] motorOutputs = {16, 64, 256, 1024, 4096};
        for (int i = 0; i < tiers.length; i++) {
            result.add(EnergyNodeSpec.builder("electric_motor_" + tiers[i].name(), tiers[i].material())
                    .kind(Kind.CONVERTER).texture("motors/rotation_electric")
                    .input(GregTechTags.Energy.EU, tiers[i].inputRate()).output(GregTechTags.Energy.RU, motorOutputs[i])
                    .capacity(tiers[i].inputRate() * 2).name("Electric Motor (" + tiers[i].name().toUpperCase() + ")").build());
        }

        // Electric dynamos: RU in → EU out at 11/16 rate (GT6 10111-10115)
        long[] dynamoOutputs = {22, 88, 352, 1408, 5632};
        for (int i = 0; i < tiers.length; i++) {
            result.add(EnergyNodeSpec.builder("electric_dynamo_" + tiers[i].name(), tiers[i].material())
                    .kind(Kind.CONVERTER).texture("dynamos/electric_rotation")
                    .input(GregTechTags.Energy.RU, tiers[i].inputRate()).output(GregTechTags.Energy.EU, dynamoOutputs[i])
                    .capacity(tiers[i].inputRate() * 2).name("Electric Dynamo (" + tiers[i].name().toUpperCase() + ")").build());
        }

        // Transformers: step-down — accept the higher voltage, emit the lower (GT6 10040+).
        // GT6's first transformer is ULV-LV built from Electric_T[0] (tin alloy), so every step
        // is one tier below the machine tiers above.
        record Step(String name, long inputRate, long outputRate, GTMaterial material) {
        }
        Step[] steps = {
                new Step("ulv_lv",  32,      8,      Materials.TinAlloy),
                new Step("lv_mv",   128,     32,     Materials.SteelGalvanized),
                new Step("mv_hv",   512,     128,    Materials.Aluminium),
                new Step("hv_ev",   2048,    512,    Materials.StainlessSteel),
                new Step("ev_iv",   8192,    2048,   Materials.Chromium),
                new Step("iv_luv",  32768,   8192,   Materials.Titanium),
                new Step("luv_zpm", 131072,  32768,  Materials.Iridium),
                new Step("zpm_uv",  524288,  131072, Materials.OsmiumElemental),
                new Step("uv_xv",   2097152, 524288, Materials.Trinitanium),
        };
        for (Step step : steps) {
            result.add(EnergyNodeSpec.builder("transformer_" + step.name(), step.material())
                    .kind(Kind.CONVERTER).texture("transformers/transformer_electric")
                    .input(GregTechTags.Energy.EU, step.inputRate()).output(GregTechTags.Energy.EU, step.outputRate())
                    .capacity(step.inputRate() * 2).name("Transformer (" + step.name().replace('_', '-').toUpperCase() + ")").build());
        }

        result.addAll(FluxRotaryCatalog.specifications());

        result.addAll(OriginalSteamTurbines.specifications());

        // Solar panels (GT6 10050/10051): silicon is the ULV panel (Electric_T[0] = tin alloy),
        // germanium uses Electric_T[2] = aluminium. Both keep only the current tick's output.
        result.add(EnergyNodeSpec.builder("solar_panel_silicon", Materials.TinAlloy)
                    .kind(Kind.SOLAR).texture("solarpanels/solarpanel_electric_8eu")
                    .input(GregTechTags.Energy.EU, 0).output(GregTechTags.Energy.EU, 8)
                    .capacity(8).name("Solar Panel (Silicon)").build());
        result.add(EnergyNodeSpec.builder("solar_panel_germanium", Materials.Aluminium)
                    .kind(Kind.SOLAR).texture("solarpanels/solarpanel_electric_8eu")
                    .input(GregTechTags.Energy.EU, 0).output(GregTechTags.Energy.EU, 16)
                    .capacity(16).name("Solar Panel (Germanium)").build());

        result.addAll(BatteryBoxDefinitions.specifications());
        result.addAll(CrystalChargerDefinitions.specifications());

        result.addAll(OriginalThermalConverter.specifications());
        return gregtech.internal.definition.DefinitionCatalog.validated(result, EnergyNodeSpec::id);
    }
}
