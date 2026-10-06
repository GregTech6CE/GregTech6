package com.gregtech.gregtech.content.energy;
import com.gregtech.gregtech.api.energy.EnergyNodeSpec;
import com.gregtech.gregtech.api.energy.EnergyNodeSpec.Kind;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.data.GregTechTags;
import java.util.ArrayList;
import java.util.List;

import com.gregtech.gregtech.content.material.Materials;
/** Built-in energy conversion/storage specifications, independent of Forge registration. */
public final class EnergyNodeDefinitions {
    private EnergyNodeDefinitions() {}
    public static List<EnergyNodeSpec> specifications() {
        List<EnergyNodeSpec> result = new ArrayList<>();
        // tier name → (voltage, material). GT6 registers these machines for VN[1..5] = LV..IV
        // with MT.DATA.Electric_T[1..5]; GT6's Electric_T is
        // {TinAlloy(ULV), SteelGalvanized(LV), Al(MV), StainlessSteel(HV), Cr(EV), Ti(IV), ...},
        // so LV is galvanized steel and ULV (tin alloy) has no basic machine of its own.
        record Tier(String name, long v, GTMaterial mat) {}
        Tier[] tiers = {
                new Tier("lv", 32, Materials.SteelGalvanized),
                new Tier("mv", 128, Materials.Aluminium),
                new Tier("hv", 512, Materials.StainlessSteel),
                new Tier("ev", 2048, Materials.Chromium),
                new Tier("iv", 8192, Materials.Titanium),
        };

        // Electric motors: EU in → RU out at half rate (GT6 10021-10025)
        long[] motorOut = {16, 64, 256, 1024, 4096};
        for (int i = 0; i < tiers.length; i++) {
            result.add(EnergyNodeSpec.builder("electric_motor_" + tiers[i].name(), tiers[i].mat())
                    .kind(Kind.CONVERTER).texture("motors/rotation_electric")
                    .input(GregTechTags.Energy.EU, tiers[i].v()).output(GregTechTags.Energy.RU, motorOut[i])
                    .capacity(tiers[i].v() * 2).names("Electric Motor (" + tiers[i].name().toUpperCase() + ")", "电动机(" + tiers[i].name().toUpperCase() + ")").build());
        }

        // Electric dynamos: RU in → EU out at 11/16 rate (GT6 10111-10115)
        long[] dynamoOut = {22, 88, 352, 1408, 5632};
        for (int i = 0; i < tiers.length; i++) {
            result.add(EnergyNodeSpec.builder("electric_dynamo_" + tiers[i].name(), tiers[i].mat())
                    .kind(Kind.CONVERTER).texture("dynamos/electric_rotation")
                    .input(GregTechTags.Energy.RU, tiers[i].v()).output(GregTechTags.Energy.EU, dynamoOut[i])
                    .capacity(tiers[i].v() * 2).names("Electric Dynamo (" + tiers[i].name().toUpperCase() + ")", "发电机(" + tiers[i].name().toUpperCase() + ")").build());
        }

        // Transformers: step-down — accept the higher voltage, emit the lower (GT6 10040+).
        // GT6's first transformer is ULV-LV built from Electric_T[0] (tin alloy), so every step
        // is one tier below the machine tiers above.
        record Step(String name, long hi, long lo, GTMaterial mat) {}
        Step[] steps = {
                new Step("ulv_lv", 32, 8, Materials.TinAlloy),
                new Step("lv_mv", 128, 32, Materials.SteelGalvanized),
                new Step("mv_hv", 512, 128, Materials.Aluminium),
                new Step("hv_ev", 2048, 512, Materials.StainlessSteel),
                new Step("ev_iv", 8192, 2048, Materials.Chromium),
                new Step("iv_luv", 32768, 8192, Materials.Titanium),
                new Step("luv_zpm", 131072, 32768, Materials.Iridium),
                new Step("zpm_uv", 524288, 131072, Materials.OsmiumElemental),
                new Step("uv_xv", 2097152, 524288, Materials.Trinitanium),
        };
        for (Step s : steps) {
            result.add(EnergyNodeSpec.builder("transformer_" + s.name(), s.mat())
                    .kind(Kind.CONVERTER).texture("transformers/transformer_electric")
                    .input(GregTechTags.Energy.EU, s.hi()).output(GregTechTags.Energy.EU, s.lo())
                    .capacity(s.hi() * 2).names("Transformer (" + s.name().replace('_', '-').toUpperCase() + ")", "变压器(" + s.name().replace('_', '-').toUpperCase() + ")").build());
        }

        // Flux motors/dynamos: Forge Energy bridge (GT6 4 RF = 1 EU, 2 EU = 1 RU).
        // GT6 builds these from MT.DATA.Flux_T[1..5] = {Pb, Invar, Electrum, ...}.
        result.add(EnergyNodeSpec.builder("flux_motor_lv", Materials.Lead)
                    .kind(Kind.CONVERTER).texture("motors/rotation_flux")
                    .input(GregTechTags.Energy.RF, 128).output(GregTechTags.Energy.RU, 16)
                    .capacity(128 * 2).names("Flux Motor (LV)", "通量电动机(LV)").build());
        result.add(EnergyNodeSpec.builder("flux_motor_mv", Materials.Invar)
                    .kind(Kind.CONVERTER).texture("motors/rotation_flux")
                    .input(GregTechTags.Energy.RF, 512).output(GregTechTags.Energy.RU, 64)
                    .capacity(512 * 2).names("Flux Motor (MV)", "通量电动机(MV)").build());
        result.add(EnergyNodeSpec.builder("flux_dynamo_lv", Materials.Lead)
                    .kind(Kind.CONVERTER).texture("dynamos/flux_rotation")
                    .input(GregTechTags.Energy.RU, 32).output(GregTechTags.Energy.RF, 88)
                    .capacity(32 * 2).names("Flux Dynamo (LV)", "通量发电机(LV)").build());
        result.add(EnergyNodeSpec.builder("flux_dynamo_mv", Materials.Invar)
                    .kind(Kind.CONVERTER).texture("dynamos/flux_rotation")
                    .input(GregTechTags.Energy.RU, 128).output(GregTechTags.Energy.RF, 352)
                    .capacity(128 * 2).names("Flux Dynamo (MV)", "通量发电机(MV)").build());

        // Steam turbines: steam L → RU (GT6 1512+, STEAM_PER_EU = 2, waste energy)
        record Turbine(String name, GTMaterial mat, long steamLt, long ruOut, String zh) {}
        Turbine[] turbines = {
                new Turbine("bronze", Materials.Bronze, 48, 16, "青铜"),
                new Turbine("brass", Materials.Brass, 72, 24, "黄铜"),
                new Turbine("invar", Materials.Invar, 96, 32, "殷钢"),
                new Turbine("steel", Materials.Steel, 192, 64, "钢"),
                new Turbine("chromium", Materials.Chromium, 288, 96, "铬"),
        };
        for (Turbine t : turbines) {
            result.add(EnergyNodeSpec.builder("steam_turbine_" + t.name(), t.mat())
                    .kind(Kind.TURBINE).texture("turbines/rotation_steam")
                    .input(GregTechTags.Energy.STEAM, t.steamLt()).output(GregTechTags.Energy.RU, t.ruOut())
                    .capacity(t.steamLt() * 2).names("Steam Turbine (" + t.mat().getLocalName() + ")", "蒸汽轮机(" + t.zh() + ")").build());
        }

        // Solar panels (GT6 10050/10051): silicon is the ULV panel (Electric_T[0] = tin alloy),
        // germanium uses Electric_T[2] = aluminium. Both keep only the current tick's output.
        result.add(EnergyNodeSpec.builder("solar_panel_silicon", Materials.TinAlloy)
                    .kind(Kind.SOLAR).texture("solarpanels/solarpanel_electric_8eu")
                    .input(GregTechTags.Energy.EU, 0).output(GregTechTags.Energy.EU, 8)
                    .capacity(8).names("Solar Panel (Silicon)", "太阳能板(硅)").build());
        result.add(EnergyNodeSpec.builder("solar_panel_germanium", Materials.Aluminium)
                    .kind(Kind.SOLAR).texture("solarpanels/solarpanel_electric_8eu")
                    .input(GregTechTags.Energy.EU, 0).output(GregTechTags.Energy.EU, 16)
                    .capacity(16).names("Solar Panel (Germanium)", "太阳能板(锗)").build());

        result.addAll(BatteryBoxDefinitions.specifications());

        result.addAll(OriginalThermalConverter.specifications());
        return com.gregtech.gregtech.api.definition.DefinitionCatalog.validated(result, EnergyNodeSpec::id);
    }
}
