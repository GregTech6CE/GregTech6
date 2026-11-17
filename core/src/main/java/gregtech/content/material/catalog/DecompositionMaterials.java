package gregtech.content.material.catalog;

import gregtech.api.addon.material.MaterialDecompositionRule;
import gregtech.api.addon.material.MaterialDecompositionRule.Route;
import gregtech.api.addon.material.MaterialProcessKeys;
import gregtech.api.material.GTValues;
import gregtech.engine.material.MaterialDefinition;
import java.util.*;

/**
 * Source decomposition phase declarations transcribed from GT6 commit
 * {@code 3703e40308c8c030763fd6297dea8b210d2a77b1}. A null phase means the source has no binding;
 * registry resolution remains a native-runtime concern. Liquid and gas inputs are alternatives.
 */
// source-rows: INPUTS and AMBIENT rows are typed transcriptions of the pinned source declarations.
public final class DecompositionMaterials {
    private DecompositionMaterials() {}

    private record Binding(String registry, long sourceUnits, long millibuckets) {}

    private record Profile(int id, String name, Binding liquid, Binding gas, boolean keepDust) {
        static Profile of(int id, String name) {
            return new Profile(id, name, null, null, false);
        }

        static Profile liquid(int id, String name, Binding liquid) {
            return new Profile(id, name, liquid, null, false);
        }

        static Profile gas(int id, String name, Binding gas) {
            return new Profile(id, name, null, gas, false);
        }

        static Profile liquidGas(int id, String name, Binding liquid, Binding gas) {
            return new Profile(id, name, liquid, gas, false);
        }

        static Profile liquidGasKept(int id, String name, Binding liquid, Binding gas) {
            return new Profile(id, name, liquid, gas, true);
        }
    }

    private record Thermal(int id, String name, long melting, long boiling, Binding liquid, Binding gas) {
        static Thermal liquid(int id, String name, long melting, long boiling, Binding liquid) {
            return new Thermal(id, name, melting, boiling, liquid, null);
        }

        static Thermal gas(int id, String name, long melting, long boiling, Binding gas) {
            return new Thermal(id, name, melting, boiling, null, gas);
        }

        static Thermal liquidGas(int id, String name, long melting, long boiling, Binding liquid, Binding gas) {
            return new Thermal(id, name, melting, boiling, liquid, gas);
        }
    }

    private record RowKey(int id, String name) {}

    // Canonical route authority; GTMaterial properties are compatibility mirrors created after freezing.
    private static final String CENTRIFUGE_MATERIALS =
            "AluminiumAlloy AluminiumBrass Alumite AnnealedCopper AquaRegia ArsenicBronze ArsenicCopper Basalt "
                    + "BatteryAlloy BismuthBronze BlackBronze BlackSteel BlueAlloy BluePhosphorus BlueSapphire "
                    + "BlueSteel Brass Bronze CastIron CdInAgAlloy Chocolate Cinnabar CobaltBrass CobaltHexahydrate "
                    + "Coltan ConductiveIron Constantan DamascusSteel Diatomite Duranium ElectricalSteel "
                    + "ElectrotineAlloy Electrum EnderEye Enderium EnderiumBase EnergiumCyan EnergiumRed FierySteel "
                    + "Fireleaf FlamascusSteel Flint GarnetSand Glass Glowstone GreenSapphire HSSE HSSG HSSS "
                    + "HeliumNeon HydratedCoal Invar Kanthal Knightmetal Komatiite Lapis Limestone LiveRoot Lumium "
                    + "Magnalium Marble MethaneIce Netherite Nichrome NikolineAlloy Nikolite NiobiumTitanium Niter "
                    + "OilShale OrangeSapphire Osmiridium Phosphorus Pitchblende Porcelain Pumice PurpleSapphire "
                    + "QuartzSand RedAlloy RedPhosphorus RedSteel Redstone RedstoneAlloy RoseGold Ruby Sapphire "
                    + "Shale Signalum SolderingAlloy Soularium StainlessSteel SteelGalvanized Steeleaf SterlingSilver "
                    + "TantalumHafniumCarbide Thaumium TinAlloy TitaniumIridium Trinaquadalloy Trinitanium Tritanium "
                    + "TungstenCarbide Tungstensteel Ultimet VanadiumGallium VanadiumMagnetite VanadiumSteel "
                    + "Vibramantium VibraniumSilver VibraniumSteel WhitePhosphorus WroughtIron YellowSapphire";
    private static final String ELECTROLYSER_MATERIALS =
            "Almandine Alunite Amazonite Amethyst Andradite Apatite Aquamarine Asbestos Azurite BalasRuby Barite "
                    + "Bastnasite Bentonite Biotite Bixbite BlackAventurine BlackEye BlackVitriol BlueAventurine "
                    + "BlueJasper BlueTopaz Bone Bromargyrite BrownAventurine CarbonDioxide CarbonMonoxide "
                    + "CarbonTrioxide Carborundum Cassiterite CatsEye Celestine Ceramic Charcoal Chromite "
                    + "ChromiumDioxide ClayBrown ClayRed Coal CoalCoke Columbite Datolite DiduraniumTrioxide Dioptase "
                    + "DragonEye DuraniumHexaastatide DuraniumHexabromide DuraniumHexachloride DuraniumHexafluoride "
                    + "DuraniumHexaiodide Emerald EnderPearl FerrousChloride Garnierite Glauconite Glycerol Glyceryl "
                    + "Goshenite Granite GraniteBlack GraniteRed GreenAventurine GreenJasper Grossular HawksEye "
                    + "Heliodor HexafluorosilicicAcid HydrogenBorate IodineSalt Kaolinite Kyanite Lazurite Lepidolite "
                    + "Lignite LigniteCoke LithiumHydroxide Malachite Maxixe Methane Mica Monazite Morganite "
                    + "NitroCarbon NitrogenDioxide NitrogenMonoxide Obsidian OceanJasper OnyxBlack OnyxRed Opal "
                    + "Palygorskite Peridot PetroleumCoke Phosphate Phosphorite Pollucite PotassiumFeldspar "
                    + "PotassiumFluoride PotassiumHydroxide PotassiumNitrate Powellite Pyrope RainforestJasper "
                    + "RedAventurine RedJasper SaltedWater Saltwater Smithsonite Sodalite SodiumFluoride "
                    + "SodiumHydroxide SodiumNitrate Sperrylite Spessartine Spinel Spodumene Talc Tantalite Tanzanite "
                    + "TigerEye TigerIron Topaz TritaniumDioxide TritaniumHexaastatide TritaniumHexabromide "
                    + "TritaniumHexachloride TritaniumHexafluoride TritaniumHexaiodide Trona Uvarovite "
                    + "VanadiumPentoxide Vermiculite Wollastonite Wulfenite YellowAventurine YellowJasper "
                    + "YttriumBariumCuprate Zeolite Zircon";
    private static final Map<String, Set<Route>> ROUTES = routes();
    // Add accepted source rows here only when their current lack of a consumer is deliberate.
    private static final Set<RowKey> INTENTIONALLY_DISABLED = Set.of();

    private static final Profile[] INPUTS = {
        Profile.liquid(8001, "Glass", new Binding("glass", GTValues.U, 144)),
        Profile.of(8002, "Flint"),
        Profile.of(8206, "Niter"),
        Profile.of(8208, "Phosphorus"),
        Profile.of(8223, "LiveRoot"),
        Profile.of(8229, "CobaltHexahydrate"),
        Profile.of(8273, "Porcelain"),
        Profile.of(8298, "EnergiumRed"),
        Profile.of(8299, "EnergiumCyan"),
        Profile.of(8302, "Ruby"),
        Profile.of(8304, "Sapphire"),
        Profile.of(8305, "GreenSapphire"),
        Profile.of(8314, "OrangeSapphire"),
        Profile.of(8315, "YellowSapphire"),
        Profile.of(8319, "EnderEye"),
        Profile.of(8328, "BlueSapphire"),
        Profile.of(8332, "Lapis"),
        Profile.liquid(8333, "Redstone", new Binding("molten.redstone", GTValues.U, 144)),
        Profile.of(8335, "HydratedCoal"),
        Profile.liquid(8340, "Nikolite", new Binding("molten.nikolite", GTValues.U, 144)),
        Profile.liquidGasKept(
                8341,
                "Glowstone",
                new Binding("molten.glowstone", GTValues.U, 144),
                new Binding("glowstone", GTValues.U, 250)),
        Profile.of(8383, "PurpleSapphire"),
        Profile.of(8458, "BluePhosphorus"),
        Profile.of(8459, "RedPhosphorus"),
        Profile.of(8460, "WhitePhosphorus"),
        Profile.of(8505, "Basalt"),
        Profile.of(8506, "Marble"),
        Profile.of(8528, "SkyStone"),
        Profile.liquid(8600, "Electrum", new Binding("molten.electrum", GTValues.U, 144)),
        Profile.liquid(8601, "SterlingSilver", new Binding("molten.sterlingsilver", GTValues.U, 144)),
        Profile.liquid(8602, "RoseGold", new Binding("molten.rosegold", GTValues.U, 144)),
        Profile.of(8605, "CdInAgAlloy"),
        Profile.liquid(8610, "Bronze", new Binding("molten.bronze", GTValues.U, 144)),
        Profile.liquid(8611, "BlackBronze", new Binding("molten.blackbronze", GTValues.U, 144)),
        Profile.liquid(8612, "BismuthBronze", new Binding("molten.bismuthbronze", GTValues.U, 144)),
        Profile.liquid(8614, "ArsenicCopper", new Binding("molten.arseniccopper", GTValues.U, 144)),
        Profile.liquid(8615, "ArsenicBronze", new Binding("molten.arsenicbronze", GTValues.U, 144)),
        Profile.liquid(8620, "Brass", new Binding("molten.brass", GTValues.U, 144)),
        Profile.liquid(8621, "CobaltBrass", new Binding("molten.cobaltbrass", GTValues.U, 144)),
        Profile.of(8622, "AluminiumAlloy"),
        Profile.liquid(8631, "BlackSteel", new Binding("molten.blacksteel", GTValues.U, 144)),
        Profile.liquid(8632, "BlueSteel", new Binding("molten.bluesteel", GTValues.U, 144)),
        Profile.liquid(8633, "RedSteel", new Binding("molten.redsteel", GTValues.U, 144)),
        Profile.liquid(8634, "DamascusSteel", new Binding("molten.damascussteel", GTValues.U, 144)),
        Profile.liquid(8635, "Tungstensteel", new Binding("molten.tungstensteel", GTValues.U, 144)),
        Profile.liquid(8636, "StainlessSteel", new Binding("molten.stainlesssteel", GTValues.U, 144)),
        Profile.of(8638, "TungstenCarbide"),
        Profile.liquid(8640, "AnnealedCopper", new Binding("molten.annealedcopper", GTValues.U, 144)),
        Profile.liquid(8643, "WroughtIron", new Binding("molten.wroughtiron", GTValues.U, 144)),
        Profile.of(8651, "SteelGalvanized"),
        Profile.liquid(8653, "VanadiumSteel", new Binding("molten.vanadiumsteel", GTValues.U, 144)),
        Profile.liquid(8658, "ElectrotineAlloy", new Binding("molten.electrotinealloy", GTValues.U, 144)),
        Profile.liquid(8659, "BlueAlloy", new Binding("molten.bluealloy", GTValues.U, 144)),
        Profile.liquid(8660, "RedAlloy", new Binding("molten.redalloy", GTValues.U, 144)),
        Profile.liquid(8661, "Invar", new Binding("molten.invar", GTValues.U, 144)),
        Profile.liquid(8662, "Constantan", new Binding("molten.constantan", GTValues.U, 144)),
        Profile.liquid(8663, "Nichrome", new Binding("molten.nichrome", GTValues.U, 144)),
        Profile.liquid(8664, "Kanthal", new Binding("molten.kanthal", GTValues.U, 144)),
        Profile.liquid(8665, "Magnalium", new Binding("molten.magnalium", GTValues.U, 144)),
        Profile.liquid(8666, "Ultimet", new Binding("molten.ultimet", GTValues.U, 144)),
        Profile.liquid(8667, "TinAlloy", new Binding("molten.tinalloy", GTValues.U, 144)),
        Profile.liquid(8668, "BatteryAlloy", new Binding("molten.batteryalloy", GTValues.U, 144)),
        Profile.liquid(8669, "SolderingAlloy", new Binding("molten.solderingalloy", GTValues.U, 144)),
        Profile.liquid(8672, "Ironwood", new Binding("molten.ironwood", GTValues.U, 144)),
        Profile.of(8673, "Steeleaf"),
        Profile.liquid(8674, "Knightmetal", new Binding("molten.knightmetal", GTValues.U, 144)),
        Profile.liquid(8675, "FierySteel", new Binding("molten.fierysteel", GTValues.U, 144)),
        Profile.liquid(8679, "Thaumium", new Binding("molten.thaumium", GTValues.U, 144)),
        Profile.liquid(8682, "Osmiridium", new Binding("molten.osmiridium", GTValues.U, 144)),
        Profile.of(8684, "Trinaquadalloy"),
        Profile.of(8686, "VanadiumGallium"),
        Profile.of(8689, "NiobiumTitanium"),
        Profile.liquid(8697, "FlamascusSteel", new Binding("molten.flamascussteel", GTValues.U, 144)),
        Profile.of(8698, "Fireleaf"),
        Profile.liquid(8700, "AluminiumBrass", new Binding("molten.aluminiumbrass", GTValues.U, 144)),
        Profile.liquid(8702, "Alumite", new Binding("molten.alumite", GTValues.U, 144)),
        Profile.of(8704, "VibraniumSteel"),
        Profile.of(8705, "VibraniumSilver"),
        Profile.of(8706, "Vibramantium"),
        Profile.liquid(8708, "Signalum", new Binding("molten.signalum", GTValues.U, 144)),
        Profile.liquid(8709, "Lumium", new Binding("molten.lumium", GTValues.U, 144)),
        Profile.liquid(8710, "Enderium", new Binding("molten.enderium", GTValues.U, 144)),
        Profile.of(8726, "VibrantAlloy"),
        Profile.of(8727, "ConductiveIron"),
        Profile.liquid(8729, "EnderiumBase", new Binding("molten.enderiumbase", GTValues.U, 144)),
        Profile.of(8730, "ElectricalSteel"),
        Profile.of(8732, "Soularium"),
        Profile.liquid(8733, "RedstoneAlloy", new Binding("molten.redstonealloy", GTValues.U, 144)),
        Profile.liquid(8737, "NikolineAlloy", new Binding("molten.nikolinealloy", GTValues.U, 144)),
        Profile.liquid(8745, "Netherite", new Binding("molten.netherite", GTValues.U, 144)),
        Profile.of(8751, "Duranium"),
        Profile.liquid(8752, "Tritanium", new Binding("molten_tritanium", GTValues.U, 144)),
        Profile.of(8790, "Trinitanium"),
        Profile.of(8793, "TitaniumIridium"),
        Profile.liquid(8796, "HSSG", new Binding("molten.hssg", GTValues.U, 144)),
        Profile.liquid(8797, "HSSE", new Binding("molten.hsse", GTValues.U, 144)),
        Profile.liquid(8798, "HSSS", new Binding("molten.hsss", GTValues.U, 144)),
        Profile.of(8802, "TantalumHafniumCarbide"),
        Profile.of(8803, "CastIron"),
        Profile.of(9000, "Pumice"),
        Profile.of(9001, "Diatomite"),
        Profile.of(9005, "GarnetSand"),
        Profile.of(9006, "QuartzSand"),
        Profile.of(9114, "Cinnabar"),
        Profile.of(9124, "Powellite"),
        Profile.of(9136, "Wulfenite"),
        Profile.liquid(9143, "VanadiumMagnetite", new Binding("molten.vanadiummagnetite", GTValues.U, 144)),
        Profile.of(9155, "Pitchblende"),
        Profile.of(9177, "Komatiite"),
        Profile.of(9189, "Limestone"),
        Profile.of(9190, "Shale"),
        Profile.of(9247, "Coltan"),
        Profile.liquid(9783, "Chocolate", shared("Chocolate", 9783, "liquid")),
        Profile.liquid(9827, "AquaRegia", new Binding("aquaregia", GTValues.U, 1000)),
        Profile.of(9833, "MethaneIce"),
        Profile.gas(9839, "HeliumNeon", new Binding("heliumneon", GTValues.U, 1000)),
        Profile.of(9853, "OilShale")
    };
    private static final Thermal[] AMBIENT = {
        Thermal.gas(180, "Argon", 83, 87, new Binding("argon", GTValues.U, 1000)),
        Thermal.gas(20, "Helium", 1, 4, shared("Helium", 20, "gas")),
        Thermal.gas(9826, "HydrochloricAcid", 100, 200, new Binding("hydrochloricacid", GTValues.U, 1000)),
        Thermal.liquidGas(
                9802, "Ice", 273, 373, new Binding("ice", GTValues.U, 1000), new Binding("steam", GTValues.U, 160000)),
        Thermal.liquid(4000, "Magic", 0, 0, new Binding("mana", GTValues.U, 250)),
        Thermal.liquid(800, "Mercury", 234, 629, shared("Mercury", 800, "liquid")),
        Thermal.gas(9832, "Methane", 100, 200, new Binding("methane", GTValues.U, 1000)),
        Thermal.gas(100, "Neon", 24, 27, new Binding("neon", GTValues.U, 1000)),
        Thermal.liquid(9825, "NitricAcid", 231, 356, new Binding("nitricacid", GTValues.U, 1000)),
        Thermal.liquidGas(
                80,
                "Oxygen",
                54,
                90,
                new Binding("liquidoxygen", GTValues.U, 1000),
                new Binding("oxygen", GTValues.U, 1000)),
        Thermal.liquidGas(
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
        var binding = MaterialFluidUnitFacts.binding(material, id, phase);
        if (binding == null)
            throw new IllegalStateException("Missing shared fluid binding for " + material + " / " + phase);
        return new Binding(binding.registryName(), binding.referenceSourceUnits(), binding.referenceMillibuckets());
    }

    private static Map<String, Set<Route>> routes() {
        var result = new LinkedHashMap<String, EnumSet<Route>>();
        addRoutes(result, CENTRIFUGE_MATERIALS, Route.CENTRIFUGE);
        addRoutes(result, ELECTROLYSER_MATERIALS, Route.ELECTROLYSER);
        var immutable = new LinkedHashMap<String, Set<Route>>();
        result.forEach((name, routes) -> immutable.put(name, Set.copyOf(routes)));
        return Map.copyOf(immutable);
    }

    private static void addRoutes(Map<String, EnumSet<Route>> target, String names, Route route) {
        for (String name : names.split("\\s+")) {
            if (name.isEmpty()) continue;
            if (target.computeIfAbsent(name, ignored -> EnumSet.noneOf(Route.class))
                    .contains(route))
                throw new IllegalStateException("Duplicate decomposition route " + route + " for " + name);
            target.get(name).add(route);
        }
    }

    /** Creates the one typed decomposition value for a canonical declaration. */
    public static MaterialDecompositionRule rule(MaterialDefinition.Declaration declaration) {
        Set<Route> routes = ROUTES.getOrDefault(declaration.name(), Set.of());
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

    /**
     * Fails when a source row or route has no canonical owner, or when the owner did not retain
     * the row in its typed decomposition rule.
     */
    public static void validateConsumed(List<MaterialDefinition.Declaration> declarations) {
        var byRow = new LinkedHashMap<RowKey, MaterialDefinition.Declaration>();
        var byName = new LinkedHashMap<String, MaterialDefinition.Declaration>();
        for (var declaration : declarations) {
            if (declaration.id() <= 0) continue;
            var key = new RowKey(declaration.id(), declaration.name());
            if (byRow.putIfAbsent(key, declaration) != null)
                throw new IllegalStateException("Duplicate canonical material row: " + key);
            byName.put(declaration.name(), declaration);
        }
        requireUniqueRows("input", INPUTS);
        requireUniqueRows("ambient", AMBIENT);
        for (Profile profile : INPUTS) validateRow(new RowKey(profile.id(), profile.name()), byRow);
        for (Thermal thermal : AMBIENT) validateRow(new RowKey(thermal.id(), thermal.name()), byRow);
        ROUTES.forEach((name, expected) -> {
            var declaration = byName.get(name);
            if (declaration == null)
                throw new IllegalStateException("Decomposition routes have no canonical material: " + name);
            var declared = declaration
                    .info()
                    .processes()
                    .findSingle(MaterialProcessKeys.DECOMPOSITION)
                    .orElseThrow(() ->
                            new IllegalStateException("Decomposition routes were not consumed for material: " + name))
                    .value();
            if (!declared.routes().equals(expected))
                throw new IllegalStateException("Decomposition routes disagree for material: " + name);
        });
    }

    private static void requireUniqueRows(String kind, Profile[] rows) {
        var keys = new HashSet<RowKey>();
        for (Profile row : rows)
            if (!keys.add(new RowKey(row.id(), row.name())))
                throw new IllegalStateException("Duplicate decomposition " + kind + " row: " + row);
    }

    private static void requireUniqueRows(String kind, Thermal[] rows) {
        var keys = new HashSet<RowKey>();
        for (Thermal row : rows)
            if (!keys.add(new RowKey(row.id(), row.name())))
                throw new IllegalStateException("Duplicate decomposition " + kind + " row: " + row);
    }

    private static void validateRow(RowKey key, Map<RowKey, MaterialDefinition.Declaration> declarations) {
        if (INTENTIONALLY_DISABLED.contains(key)) return;
        var declaration = declarations.get(key);
        if (declaration == null)
            throw new IllegalStateException("Decomposition data row has no canonical material: " + key);
        declaration
                .info()
                .processes()
                .findSingle(MaterialProcessKeys.DECOMPOSITION)
                .orElseThrow(() -> new IllegalStateException("Decomposition data row was not consumed: " + key));
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
