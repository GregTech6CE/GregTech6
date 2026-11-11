package gregtech.content.material.catalog;

import gregtech.api.addon.material.MaterialDecompositionRule;
import gregtech.api.addon.material.MaterialDecompositionRule.Route;
import gregtech.api.material.GTValues;
import gregtech.content.recipe.OriginalMaterialFluidUnits;
import gregtech.engine.material.MaterialDefinition;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Source decomposition phase declarations transcribed from GT6 commit
 * {@code 3703e40308c8c030763fd6297dea8b210d2a77b1}. A null phase means the source has no binding;
 * registry resolution remains a native-runtime concern. Liquid and gas inputs are alternatives.
 */
// source-rows: INPUTS and AMBIENT rows are typed transcriptions of the pinned source declarations.
public final class DecompositionMaterials {
    private DecompositionMaterials() {}

    private record Binding(String registry, long sourceUnits, long millibuckets) {}

    private record Profile(int id, String name, Binding liquid, Binding gas, boolean keepDust) {}

    private record Thermal(int id, String name, long melting, long boiling, Binding liquid, Binding gas) {}

    private static final Profile[] INPUTS = {
        new Profile(8001, "Glass", new Binding("glass", GTValues.U, 144), null, false),
        new Profile(8002, "Flint", null, null, false),
        new Profile(8206, "Niter", null, null, false),
        new Profile(8208, "Phosphorus", null, null, false),
        new Profile(8223, "LiveRoot", null, null, false),
        new Profile(8229, "CobaltHexahydrate", null, null, false),
        new Profile(8273, "Porcelain", null, null, false),
        new Profile(8298, "EnergiumRed", null, null, false),
        new Profile(8299, "EnergiumCyan", null, null, false),
        new Profile(8302, "Ruby", null, null, false),
        new Profile(8304, "Sapphire", null, null, false),
        new Profile(8305, "GreenSapphire", null, null, false),
        new Profile(8314, "OrangeSapphire", null, null, false),
        new Profile(8315, "YellowSapphire", null, null, false),
        new Profile(8319, "EnderEye", null, null, false),
        new Profile(8328, "BlueSapphire", null, null, false),
        new Profile(8332, "Lapis", null, null, false),
        new Profile(8333, "Redstone", new Binding("molten.redstone", GTValues.U, 144), null, false),
        new Profile(8335, "HydratedCoal", null, null, false),
        new Profile(8340, "Nikolite", new Binding("molten.nikolite", GTValues.U, 144), null, false),
        new Profile(
                8341,
                "Glowstone",
                new Binding("molten.glowstone", GTValues.U, 144),
                new Binding("glowstone", GTValues.U, 250),
                true),
        new Profile(8383, "PurpleSapphire", null, null, false),
        new Profile(8458, "BluePhosphorus", null, null, false),
        new Profile(8459, "RedPhosphorus", null, null, false),
        new Profile(8460, "WhitePhosphorus", null, null, false),
        new Profile(8505, "Basalt", null, null, false),
        new Profile(8506, "Marble", null, null, false),
        new Profile(8528, "SkyStone", null, null, false),
        new Profile(8600, "Electrum", new Binding("molten.electrum", GTValues.U, 144), null, false),
        new Profile(8601, "SterlingSilver", new Binding("molten.sterlingsilver", GTValues.U, 144), null, false),
        new Profile(8602, "RoseGold", new Binding("molten.rosegold", GTValues.U, 144), null, false),
        new Profile(8605, "CdInAgAlloy", null, null, false),
        new Profile(8610, "Bronze", new Binding("molten.bronze", GTValues.U, 144), null, false),
        new Profile(8611, "BlackBronze", new Binding("molten.blackbronze", GTValues.U, 144), null, false),
        new Profile(8612, "BismuthBronze", new Binding("molten.bismuthbronze", GTValues.U, 144), null, false),
        new Profile(8614, "ArsenicCopper", new Binding("molten.arseniccopper", GTValues.U, 144), null, false),
        new Profile(8615, "ArsenicBronze", new Binding("molten.arsenicbronze", GTValues.U, 144), null, false),
        new Profile(8620, "Brass", new Binding("molten.brass", GTValues.U, 144), null, false),
        new Profile(8621, "CobaltBrass", new Binding("molten.cobaltbrass", GTValues.U, 144), null, false),
        new Profile(8622, "AluminiumAlloy", null, null, false),
        new Profile(8631, "BlackSteel", new Binding("molten.blacksteel", GTValues.U, 144), null, false),
        new Profile(8632, "BlueSteel", new Binding("molten.bluesteel", GTValues.U, 144), null, false),
        new Profile(8633, "RedSteel", new Binding("molten.redsteel", GTValues.U, 144), null, false),
        new Profile(8634, "DamascusSteel", new Binding("molten.damascussteel", GTValues.U, 144), null, false),
        new Profile(8635, "Tungstensteel", new Binding("molten.tungstensteel", GTValues.U, 144), null, false),
        new Profile(8636, "StainlessSteel", new Binding("molten.stainlesssteel", GTValues.U, 144), null, false),
        new Profile(8638, "TungstenCarbide", null, null, false),
        new Profile(8640, "AnnealedCopper", new Binding("molten.annealedcopper", GTValues.U, 144), null, false),
        new Profile(8643, "WroughtIron", new Binding("molten.wroughtiron", GTValues.U, 144), null, false),
        new Profile(8651, "SteelGalvanized", null, null, false),
        new Profile(8653, "VanadiumSteel", new Binding("molten.vanadiumsteel", GTValues.U, 144), null, false),
        new Profile(8658, "ElectrotineAlloy", new Binding("molten.electrotinealloy", GTValues.U, 144), null, false),
        new Profile(8659, "BlueAlloy", new Binding("molten.bluealloy", GTValues.U, 144), null, false),
        new Profile(8660, "RedAlloy", new Binding("molten.redalloy", GTValues.U, 144), null, false),
        new Profile(8661, "Invar", new Binding("molten.invar", GTValues.U, 144), null, false),
        new Profile(8662, "Constantan", new Binding("molten.constantan", GTValues.U, 144), null, false),
        new Profile(8663, "Nichrome", new Binding("molten.nichrome", GTValues.U, 144), null, false),
        new Profile(8664, "Kanthal", new Binding("molten.kanthal", GTValues.U, 144), null, false),
        new Profile(8665, "Magnalium", new Binding("molten.magnalium", GTValues.U, 144), null, false),
        new Profile(8666, "Ultimet", new Binding("molten.ultimet", GTValues.U, 144), null, false),
        new Profile(8667, "TinAlloy", new Binding("molten.tinalloy", GTValues.U, 144), null, false),
        new Profile(8668, "BatteryAlloy", new Binding("molten.batteryalloy", GTValues.U, 144), null, false),
        new Profile(8669, "SolderingAlloy", new Binding("molten.solderingalloy", GTValues.U, 144), null, false),
        new Profile(8672, "Ironwood", new Binding("molten.ironwood", GTValues.U, 144), null, false),
        new Profile(8673, "Steeleaf", null, null, false),
        new Profile(8674, "Knightmetal", new Binding("molten.knightmetal", GTValues.U, 144), null, false),
        new Profile(8675, "FierySteel", new Binding("molten.fierysteel", GTValues.U, 144), null, false),
        new Profile(8679, "Thaumium", new Binding("molten.thaumium", GTValues.U, 144), null, false),
        new Profile(8682, "Osmiridium", new Binding("molten.osmiridium", GTValues.U, 144), null, false),
        new Profile(8684, "Trinaquadalloy", null, null, false),
        new Profile(8686, "VanadiumGallium", null, null, false),
        new Profile(8689, "NiobiumTitanium", null, null, false),
        new Profile(8697, "FlamascusSteel", new Binding("molten.flamascussteel", GTValues.U, 144), null, false),
        new Profile(8698, "Fireleaf", null, null, false),
        new Profile(8700, "AluminiumBrass", new Binding("molten.aluminiumbrass", GTValues.U, 144), null, false),
        new Profile(8702, "Alumite", new Binding("molten.alumite", GTValues.U, 144), null, false),
        new Profile(8704, "VibraniumSteel", null, null, false),
        new Profile(8705, "VibraniumSilver", null, null, false),
        new Profile(8706, "Vibramantium", null, null, false),
        new Profile(8708, "Signalum", new Binding("molten.signalum", GTValues.U, 144), null, false),
        new Profile(8709, "Lumium", new Binding("molten.lumium", GTValues.U, 144), null, false),
        new Profile(8710, "Enderium", new Binding("molten.enderium", GTValues.U, 144), null, false),
        new Profile(8726, "VibrantAlloy", null, null, false),
        new Profile(8727, "ConductiveIron", null, null, false),
        new Profile(8729, "EnderiumBase", new Binding("molten.enderiumbase", GTValues.U, 144), null, false),
        new Profile(8730, "ElectricalSteel", null, null, false),
        new Profile(8732, "Soularium", null, null, false),
        new Profile(8733, "RedstoneAlloy", new Binding("molten.redstonealloy", GTValues.U, 144), null, false),
        new Profile(8737, "NikolineAlloy", new Binding("molten.nikolinealloy", GTValues.U, 144), null, false),
        new Profile(8745, "Netherite", new Binding("molten.netherite", GTValues.U, 144), null, false),
        new Profile(8751, "Duranium", null, null, false),
        new Profile(8752, "Tritanium", new Binding("molten_tritanium", GTValues.U, 144), null, false),
        new Profile(8790, "Trinitanium", null, null, false),
        new Profile(8793, "TitaniumIridium", null, null, false),
        new Profile(8796, "HSSG", new Binding("molten.hssg", GTValues.U, 144), null, false),
        new Profile(8797, "HSSE", new Binding("molten.hsse", GTValues.U, 144), null, false),
        new Profile(8798, "HSSS", new Binding("molten.hsss", GTValues.U, 144), null, false),
        new Profile(8802, "TantalumHafniumCarbide", null, null, false),
        new Profile(8803, "CastIron", null, null, false),
        new Profile(9000, "Pumice", null, null, false),
        new Profile(9001, "Diatomite", null, null, false),
        new Profile(9005, "GarnetSand", null, null, false),
        new Profile(9006, "QuartzSand", null, null, false),
        new Profile(9114, "Cinnabar", null, null, false),
        new Profile(9124, "Powellite", null, null, false),
        new Profile(9136, "Wulfenite", null, null, false),
        new Profile(9143, "VanadiumMagnetite", new Binding("molten.vanadiummagnetite", GTValues.U, 144), null, false),
        new Profile(9155, "Pitchblende", null, null, false),
        new Profile(9177, "Komatiite", null, null, false),
        new Profile(9189, "Limestone", null, null, false),
        new Profile(9190, "Shale", null, null, false),
        new Profile(9247, "Coltan", null, null, false),
        new Profile(9783, "Chocolate", shared("Chocolate", 9783, "liquid"), null, false),
        new Profile(9827, "AquaRegia", new Binding("aquaregia", GTValues.U, 1000), null, false),
        new Profile(9833, "MethaneIce", null, null, false),
        new Profile(9839, "HeliumNeon", null, new Binding("heliumneon", GTValues.U, 1000), false),
        new Profile(9853, "OilShale", null, null, false)
    };
    private static final Thermal[] AMBIENT = {
        new Thermal(180, "Argon", 83, 87, null, new Binding("argon", GTValues.U, 1000)),
        new Thermal(20, "Helium", 1, 4, null, shared("Helium", 20, "gas")),
        new Thermal(9826, "HydrochloricAcid", 100, 200, null, new Binding("hydrochloricacid", GTValues.U, 1000)),
        new Thermal(
                9802, "Ice", 273, 373, new Binding("ice", GTValues.U, 1000), new Binding("steam", GTValues.U, 160000)),
        new Thermal(4000, "Magic", 0, 0, new Binding("mana", GTValues.U, 250), null),
        new Thermal(800, "Mercury", 234, 629, shared("Mercury", 800, "liquid"), null),
        new Thermal(9832, "Methane", 100, 200, null, new Binding("methane", GTValues.U, 1000)),
        new Thermal(100, "Neon", 24, 27, null, new Binding("neon", GTValues.U, 1000)),
        new Thermal(9825, "NitricAcid", 231, 356, new Binding("nitricacid", GTValues.U, 1000), null),
        new Thermal(
                80,
                "Oxygen",
                54,
                90,
                new Binding("liquidoxygen", GTValues.U, 1000),
                new Binding("oxygen", GTValues.U, 1000)),
        new Thermal(
                9800,
                "Water",
                273,
                373,
                new Binding("minecraft:water", GTValues.U, 1000),
                new Binding("steam", GTValues.U, 160000))
    };

    private static Profile input(int id, String name) {
        for (Profile p : INPUTS) if (p.id() == id && p.name().equals(name)) return p;
        return null;
    }

    private static Thermal thermal(int id, String name) {
        for (Thermal p : AMBIENT) if (p.id() == id && p.name().equals(name)) return p;
        return null;
    }

    private static Binding shared(String material, int id, String phase) {
        var binding = OriginalMaterialFluidUnits.binding(material, id, phase);
        if (binding == null)
            throw new IllegalStateException("Missing shared fluid binding for " + material + " / " + phase);
        return new Binding(binding.registryName(), binding.referenceSourceUnits(), binding.referenceMillibuckets());
    }

    /**
     * Creates the one typed decomposition value for a canonical declaration. Existing map
     * permissions are supplied by the caller so this source table never grants a machine route.
     */
    public static MaterialDecompositionRule rule(MaterialDefinition.Declaration declaration, Set<Route> routes) {
        Profile profile = input(declaration.id(), declaration.name());
        Thermal thermal = thermal(declaration.id(), declaration.name());
        if (profile == null && thermal == null && routes.isEmpty()) return null;
        List<MaterialDecompositionRule.FluidBinding> fluids = new ArrayList<>();
        if (profile != null) {
            add(fluids, MaterialDecompositionRule.Phase.LIQUID, profile.liquid());
            add(fluids, MaterialDecompositionRule.Phase.GAS, profile.gas());
        }
        if (thermal != null) {
            if (thermal.melting != declaration.material().meltingPointKelvin()
                    || thermal.boiling != declaration.material().boilingPointKelvin())
                throw new IllegalArgumentException(
                        "Decomposition thermal source disagrees with material '" + declaration.name() + "'");
            add(fluids, MaterialDecompositionRule.Phase.LIQUID, thermal.liquid());
            add(fluids, MaterialDecompositionRule.Phase.GAS, thermal.gas());
        }
        return new MaterialDecompositionRule(routes, fluids, profile != null && profile.keepDust());
    }

    private static void add(
            List<MaterialDecompositionRule.FluidBinding> target,
            MaterialDecompositionRule.Phase phase,
            Binding binding) {
        if (binding != null)
            target.add(new MaterialDecompositionRule.FluidBinding(
                    phase, binding.registry(), binding.sourceUnits(), binding.millibuckets()));
    }
}
