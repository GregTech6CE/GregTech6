package gregtech.content.recipe;

import java.util.List;
import java.util.Set;

/**
 * Bounded, exact-identity washing facts from original GT6 MT at
 * 3703e40308c8c030763fd6297dea8b210d2a77b1 (W: 607771f74acb0fe647f7e9412676e72094799545).
 * Includes every source positive (20 Mercury, 12 Persulfate, 29 Firestone; 59 identities),
 * including the nine inherited refractmetal Firestone tags at MT:76. The 109 explicit
 * negatives are source-declared candidates of the 159 reviewed positive-candidate/tag cases,
 * not a blanket default for SupplementalMaterials, addons, or arbitrary matching names.
 * IDs AND canonical names must agree; callers must separately verify native provenance
 * and must not use these facts as defaults for unreviewed or replaced material identities.
 */
public final class SourceOreWashingData {
    private SourceOreWashingData() {}
    public enum Tag { MERCURY, PERSULFATE, FIRESTONE }
    public enum Decision { YES, NO, UNKNOWN }
    public record Binding(int materialId, String materialName, Set<Tag> tags, int sourceLine) {
        public Binding { tags = Set.copyOf(tags); }
    }
    private static Binding row(int id, String name, int line, Tag... tags) {
        return new Binding(id, name, Set.of(tags), line);
    }
    private static final List<Binding> BINDINGS = List.of(
        // Source positives: an omitted other washing tag here is a reviewed NO.
        row(30, "Lithium", 385, Tag.MERCURY),
        row(31, "Lithium6", 386, Tag.MERCURY),
        row(220, "Titanium", 410, Tag.FIRESTONE),
        row(230, "Vanadium", 411, Tag.FIRESTONE),
        row(240, "Chromium", 412, Tag.FIRESTONE),
        row(270, "Cobalt", 415, Tag.PERSULFATE),
        row(278, "Cobalt60", 416, Tag.PERSULFATE),
        row(280, "Nickel", 417, Tag.PERSULFATE),
        row(290, "Copper", 418, Tag.PERSULFATE),
        row(300, "Zinc", 419, Tag.MERCURY, Tag.PERSULFATE),
        row(400, "Zirconium", 429, Tag.FIRESTONE),
        row(410, "Niobium", 430, Tag.FIRESTONE),
        row(420, "Molybdenum", 431, Tag.FIRESTONE),
        row(470, "Silver", 436, Tag.MERCURY),
        row(720, "Hafnium", 461, Tag.FIRESTONE),
        row(730, "Tantalum", 462, Tag.FIRESTONE),
        row(740, "Tungsten", 463, Tag.FIRESTONE),
        row(780, "Platinum", 467, Tag.MERCURY),
        row(790, "Gold", 468, Tag.MERCURY),
        row(791, "Gold198", 469, Tag.MERCURY),
        row(8026, "TungstenTrioxide", 1077, Tag.FIRESTONE),
        row(8029, "LithiumChloride", 1121, Tag.MERCURY),
        row(8234, "VanadiumPentoxide", 1065, Tag.FIRESTONE),
        row(8419, "Zircon", 1511, Tag.FIRESTONE),
        row(8461, "NiobiumPentoxide", 1068, Tag.FIRESTONE),
        row(8462, "TantalumPentoxide", 1071, Tag.FIRESTONE),
        row(8600, "Electrum", 1691, Tag.MERCURY),
        row(8676, "AstralSilver", 1679, Tag.MERCURY),
        row(8677, "Midasium", 1680, Tag.MERCURY),
        row(8678, "Mithril", 1681, Tag.MERCURY),
        row(8744, "AncientDebris", 1832, Tag.MERCURY),
        row(8747, "Efrine", 1835, Tag.MERCURY),
        row(8769, "Orichalcum", 1678, Tag.MERCURY),
        row(8779, "Celenegil", 1682, Tag.MERCURY),
        row(9113, "Chromite", 3750, Tag.FIRESTONE),
        row(9115, "Cobaltite", 3716, Tag.PERSULFATE),
        row(9116, "Cooperite", 3718, Tag.MERCURY),
        row(9118, "Garnierite", 3702, Tag.PERSULFATE),
        row(9120, "Ilmenite", 3748, Tag.FIRESTONE),
        row(9123, "Molybdenite", 3710, Tag.FIRESTONE),
        row(9128, "Scheelite", 3725, Tag.FIRESTONE),
        row(9130, "Sphalerite", 3711, Tag.PERSULFATE),
        row(9132, "Tetrahedrite", 3719, Tag.PERSULFATE),
        row(9133, "Tungstate", 3729, Tag.FIRESTONE),
        row(9143, "VanadiumMagnetite", 3743, Tag.FIRESTONE),
        row(9145, "Pentlandite", 3713, Tag.PERSULFATE),
        row(9148, "Tantalite", 3744, Tag.FIRESTONE),
        row(9156, "Malachite", 3755, Tag.PERSULFATE),
        row(9193, "Stolzite", 3731, Tag.FIRESTONE),
        row(9194, "Ferberite", 3727, Tag.FIRESTONE),
        row(9195, "Huebnerite", 3728, Tag.FIRESTONE),
        row(9196, "Russellite", 3732, Tag.FIRESTONE),
        row(9197, "Pinalite", 3733, Tag.FIRESTONE),
        row(9210, "Bromargyrite", 3756, Tag.MERCURY),
        row(9211, "Smithsonite", 3757, Tag.MERCURY, Tag.PERSULFATE),
        row(9212, "Sperrylite", 3758, Tag.MERCURY),
        row(9217, "Wolframite", 3726, Tag.FIRESTONE),
        row(9246, "Columbite", 3745, Tag.FIRESTONE),
        row(9247, "Coltan", 3746, Tag.FIRESTONE),
        // Bounded source-negative candidates; constructor/declaration line retained.
        row(40, "Beryllium", 387),
        row(160, "Sulfur", 404),
        row(250, "Manganese", 413),
        row(310, "Gallium", 420),
        row(320, "Germanium", 421),
        row(330, "Arsenic", 422),
        row(340, "Selenium", 423),
        row(370, "Rubidium", 426),
        row(440, "Ruthenium", 433),
        row(450, "Rhodium", 434),
        row(460, "Palladium", 435),
        row(480, "Cadmium", 437),
        row(490, "Indium", 438),
        row(500, "Tin", 439),
        row(510, "Antimony", 440),
        row(550, "Caesium", 444),
        row(750, "Rhenium", 464),
        row(760, "OsmiumElemental", 465),
        row(770, "Iridium", 466),
        row(820, "Lead", 472),
        row(830, "Bismuth", 473),
        row(1260, "Trinium", 696),
        row(8008, "Alumina", 1081),
        row(8014, "AluminiumHydroxide", 1083),
        row(8016, "MagnesiumCarbonate", 1101),
        row(8101, "Soulsand", 1622),
        row(8209, "Apatite", 1298),
        row(8303, "BalasRuby", 1390),
        row(8306, "Topaz", 1425),
        row(8326, "Spinel", 1389),
        row(8333, "Redstone", 1540),
        row(8338, "Monazite", 1626),
        row(8340, "Nikolite", 1541),
        row(8341, "Glowstone", 1544),
        row(8342, "Firestone", 1537),
        row(8346, "NetherQuartz", 1554),
        row(8368, "GlowstoneCeres", 1545),
        row(8369, "GlowstoneIo", 1546),
        row(8370, "GlowstoneEnceladus", 1547),
        row(8371, "GlowstoneProteus", 1548),
        row(8372, "GlowstonePluto", 1549),
        row(8420, "Azurite", 1512),
        row(8421, "Eudialyte", 1513),
        row(8436, "RedFluorite", 1110),
        row(8438, "BlueFluorite", 1112),
        row(8439, "GreenFluorite", 1113),
        row(8440, "BlackFluorite", 1114),
        row(8441, "WhiteFluorite", 1115),
        row(8442, "YellowFluorite", 1116),
        row(8443, "OrangeFluorite", 1117),
        row(8444, "MagentaFluorite", 1118),
        row(8455, "ClayRed", 1283),
        row(8456, "Gloomstone", 1550),
        row(8457, "VoidQuartz", 1555),
        row(8500, "Stone", 1631),
        row(8505, "Basalt", 3819),
        row(8508, "GraniteBlack", 3830),
        row(8610, "Bronze", 1705),
        row(8620, "Brass", 1700),
        row(8649, "MeteoricIron", 1737),
        row(8736, "Endium", 1824),
        row(8750, "Desh", 1838),
        row(8753, "Dolamide", 1842),
        row(8761, "Infuscolium", 1660),
        row(8762, "Rubracium", 1661),
        row(8763, "Meutoite", 1662),
        row(8765, "Ceruclase", 1665),
        row(8767, "Oureclase", 1666),
        row(8770, "Carmot", 1668),
        row(8774, "Prometheum", 1676),
        row(8776, "Vulcanite", 1677),
        row(8785, "Syrmorite", 1849),
        row(8786, "Octine", 1850),
        row(9003, "BasalticMineralSand", 3705),
        row(9004, "GraniticMineralSand", 3706),
        row(9100, "RareEarth", 1625),
        row(9104, "Hematite", 1094),
        row(9105, "Bauxite", 3749),
        row(9106, "BrownLimonite", 3740),
        row(9107, "Calcite", 1108),
        row(9108, "Cassiterite", 3701),
        row(9109, "Realgar", 3708),
        row(9111, "Chalcopyrite", 3714),
        row(9114, "Cinnabar", 3709),
        row(9117, "Galena", 3717),
        row(9122, "Magnetite", 3704),
        row(9124, "Powellite", 3751),
        row(9125, "Pyrite", 1237),
        row(9126, "Pyrolusite", 1090),
        row(9131, "Stibnite", 3712),
        row(9134, "Uraninite", 3703),
        row(9136, "Wulfenite", 3752),
        row(9137, "YellowLimonite", 3741),
        row(9146, "Spodumene", 3768),
        row(9149, "Lepidolite", 3769),
        row(9160, "Barite", 3722),
        row(9161, "Gypsum", 1106),
        row(9167, "Kaolinite", 1286),
        row(9192, "Rutile", 1086),
        row(9198, "DiduraniumTrioxide", 3780),
        row(9200, "DuraniumHexachloride", 3782),
        row(9201, "DuraniumHexabromide", 3783),
        row(9202, "DuraniumHexaiodide", 3784),
        row(9203, "DuraniumHexaastatide", 3785),
        row(9204, "TritaniumDioxide", 3786),
        row(9207, "TritaniumHexabromide", 3789),
        row(9213, "Kesterite", 3720),
        row(9214, "Stannite", 3721),
        row(9216, "Arsenopyrite", 3715));

    public static List<Binding> bindings() { return BINDINGS; }
    public static Binding binding(int id, String canonicalName) {
        for (Binding binding : BINDINGS)
            if (binding.materialId() == id && binding.materialName().equals(canonicalName)) return binding;
        return null;
    }
    /** Null means unreviewed, not a known empty source tag set. */
    public static Set<Tag> expectedTags(int id, String canonicalName) {
        Binding binding = binding(id, canonicalName);
        return binding == null ? null : binding.tags();
    }
    public static Decision decision(int id, String canonicalName, Tag tag) {
        Binding binding = binding(id, canonicalName);
        if (binding == null || tag == null) return Decision.UNKNOWN;
        return binding.tags().contains(tag) ? Decision.YES : Decision.NO;
    }
}
