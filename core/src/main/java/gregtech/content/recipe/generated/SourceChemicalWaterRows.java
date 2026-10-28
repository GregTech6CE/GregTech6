package gregtech.content.recipe.generated;

import java.util.Set;

/** Exact inputs of Loader_Recipes_Chem's FL.waters(1000) loop, lines 37-96.
 * Source: GregTech6/gregtech6@3703e40308c8c030763fd6297dea8b210d2a77b1.
 * Keep this allowlist separate from the legacy generated table: other w: tokens do not
 * imply interchangeable water, and neither output fluids nor unrelated rows are expanded.
 */
public final class SourceChemicalWaterRows {
    private SourceChemicalWaterRows() {}

    private static final Set<String> FAMILIES = Set.of(
            "Mixer|omd:NaCl:250|w:750",
            "Mixer|omd:NaCl:1000|w:3000",
            "Mixer|omd:KCl:250|w:750",
            "Mixer|omd:KCl:1000|w:3000",
            "Mixer|omd:MgCl2:1000|w:2000",
            "Mixer|omd:Na2SO4:7000|w:30000",
            "Mixer|omd:Na2CO3:1000|w:1000",
            "Mixer|omd:CaSO4:1000|w:1000",
            "Mixer|omd:Si:1000|w:6000",
            "Mixer|omd:Mn:1000|w:6000",
            "Mixer|omd:Na:1000|w:3000",
            "Mixer|omd:K:1000|w:3000",
            "Mixer|omd:Li:1000|w:3000",
            "Mixer|omd:Ca:1000|m:CO2:3000,w:3000",
            "Mixer|omd:Mg:1000|m:CO2:3000,w:3000",
            "Mixer|omd:FeCl3:8000|w:6000,m:SO2:3000",
            "Mixer|omd:FeCl3:4000,omd:NaOH:9000|w:18000",
            "Mixer|omd:FeCl3:4000,omd:KOH:9000|w:18000",
            "Mixer||w:3000,m:Cl:2000",
            "Mixer||w:3000,m:SO3:4000",
            "Mixer||w:3000,m:NO2:9000",
            "Mixer||w:3000,m:H2S2O7:11000",
            "Electrolyzer|i:dustSmall:NaCl:1|w:750",
            "Electrolyzer|i:dust:NaCl:1|w:3000",
            "Electrolyzer|i:dustSmall:KCl:1|w:750",
            "Electrolyzer|i:dust:KCl:1|w:3000",
            "Electrolyzer|i:dustSmall:LiCl:1|w:750",
            "Electrolyzer|i:dust:LiCl:1|w:3000",
            "Electrolyzer|tag:1|m:BlueVitriol:6000,w:3000",
            "Electrolyzer|tag:1|m:RedVitriol:6000,w:3000",
            "Electrolyzer|tag:1|m:PinkVitriol:6000,w:3000",
            "Electrolyzer|tag:1|m:CyanVitriol:6000,w:3000",
            "Electrolyzer|tag:1|m:WhiteVitriol:6000,w:3000",
            "Electrolyzer|tag:1|m:GrayVitriol:6000,w:3000",
            "Electrolyzer|tag:1|m:GreenVitriol:6000,w:3000",
            "Electrolyzer|tag:1|m:MartianVitriol:6000,w:3000",
            "Electrolyzer|tag:1|m:VitriolOfClay:8500,w:4500",
            "Electrolyzer|tag:1|m:ChloroauricAcid:6000,w:4500",
            "Electrolyzer|tag:1|m:ChloroplatinicAcid:9000,w:6000",
            "Electrolyzer|tag:1|m:StannicChloride:5000,w:6000",
            "Lightning|tag:1|w:3000,f:Oxygen:1000",
            "Bath|omd:Mn:1000|w:1500",
            "Bath|omd:Si:1000|w:1500",
            "Bath|i:dust:KAlO2:4|w:1500",
            "Bath|i:dust:NaAlO2:4|w:1500");

    public static boolean matches(String map, String[] itemsIn, String[] fluidsIn) {
        return FAMILIES.contains(map + "|" + String.join(",", itemsIn) + "|" + String.join(",", fluidsIn));
    }

    public static Set<String> families() { return FAMILIES; }
}
