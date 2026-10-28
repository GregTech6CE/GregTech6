/* Original bucket families: Gregorius Techneticies / GregTech-6 Team, LGPL-3.0-or-later.
 * Source: MultiItemRandomTools:304-329; empty shells contain 3U wood and 1U band metal. */
package gregtech.content.tool;

import java.util.List;

/** Original wooden buckets; fluid identity belongs to the registered variant, with no stack NBT. */
public final class WoodenBucketCatalog {

    private WoodenBucketCatalog() {}

    public record Band(String id, String material, int originalBase) {}

    public record Content(String id, String tooltip, int offset, List<String> fluids) {

        public boolean empty() {
            return offset == 0;
        }

        public boolean discardOnly() {
            return offset == 5 || offset == 9;
        }
    }

    public static final List<Band> BANDS = List.of(
            new Band("copper", "Copper", 2000),
            new Band("tin", "Tin", 2100),
            new Band("zinc", "Zinc", 2200),
            new Band("lead", "Lead", 2300),
            new Band("bismuth", "Bismuth", 2400),
            new Band("brass", "Brass", 2500),
            new Band("bronze", "Bronze", 2600),
            new Band("bismuth_bronze", "BismuthBronze", 2700),
            new Band("gold", "Gold", 2800));

    public static final List<Content> CONTENTS = List.of(
            new Content("empty", "Empty", 0, List.of()),
            new Content(
                    "water",
                    "Water",
                    1,
                    List.of(
                            "Water",
                            "DistW",
                            "River_Water",
                            "MnWtr",
                            "Mineralsoda",
                            "Soda",
                            "Water_Hot",
                            "Water_Boiling",
                            "Water_Geothermal")),
            new Content("milk", "Milk (you cannot drink out of Buckets!)", 2, List.of("Milk", "MilkGrC")),
            new Content("latex", "Latex", 3, List.of("Latex")),
            new Content("creosote", "Creosote Oil", 4, List.of("Oil_Creosote")),
            new Content(
                    "sea_water",
                    "Sea Water (you cannot place this!)",
                    5,
                    List.of("Ocean", "OceanGrC", "Tropics_Water")),
            new Content("rubber_sap", "Rubber Tree Sap", 6, List.of("Resin_Rubber")),
            new Content("spruce_resin", "Spruce Tree Resin", 7, List.of("Resin_Spruce")),
            new Content("honey", "Honey", 8, List.of("Honey", "HoneyGrC", "HoneyBoP")),
            new Content("dirty_water", "Dirty Water (you cannot place this!)", 9, List.of("Dirty_Water")),
            new Content("lubricant", "Lubricant", 10, List.of("LubRoCant", "Lubricant")),
            new Content("spoiled_milk", "Milk (you cannot drink out of Buckets!)", 11, List.of("Milk_Spoiled")),
            new Content("maple_sap", "Maple Sap", 12, List.of("Sap_Maple")),
            new Content("rainbow_sap", "Rainbow Sap", 13, List.of("Sap_Rainbow")),
            new Content("soy_milk", "Soy Milk", 14, List.of("MilkSoy")),
            new Content("glue", "Glue", 15, List.of("Glue")),
            new Content("spectral_dew", "Spectral Dew", 16, List.of("SpDew")),
            new Content(
                    "seed_oil",
                    "Seed Oil",
                    17,
                    List.of("Oil_Seed", "Oil_Lin", "Oil_Hemp", "Oil_Olive", "Oil_Sunflower", "Oil_Nut")),
            new Content("poison", "Poison", 18, List.of("Poison", "Potion_Poison_2")));

    public static String itemId(Band band, Content content) {
        return "wooden_bucket_" + band.id() + (content.empty() ? "" : "_" + content.id());
    }

    public static Content empty() {
        return CONTENTS.get(0);
    }

    public static Content water() {
        return CONTENTS.get(1);
    }

    public static Content milk() {
        return CONTENTS.get(2);
    }
}
