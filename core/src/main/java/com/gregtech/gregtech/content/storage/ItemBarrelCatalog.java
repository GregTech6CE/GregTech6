/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later. Original IDs 6983..6999. */
package com.gregtech.gregtech.content.storage;

import java.util.List;

public final class ItemBarrelCatalog {
    private ItemBarrelCatalog() {}
    public record Spec(String id, int originalId, String material, String band, long capacity,
                       int hardness, int flammability, String name) {}
    public static final List<Spec> ALL = List.of(
            new Spec("item_barrel_wood_lead", 6998, "Wood", "Lead", 5000, 3, 100, "Wooden Item Barrel (Cheap)"),
            new Spec("item_barrel_wood_bismuth", 6992, "Wood", "Bismuth", 5000, 3, 100, "Wooden Item Barrel (Cheap)"),
            new Spec("item_barrel_wood_bronze", 6991, "Wood", "Bronze", 5000, 3, 100, "Wooden Item Barrel (Cheap)"),
            new Spec("item_barrel_wood_brass", 6990, "Wood", "Brass", 5000, 3, 100, "Wooden Item Barrel (Cheap)"),
            new Spec("item_barrel_wood_treated", 6999, "WoodTreated", "Iron", 10000, 3, 100, "Wooden Item Barrel"),
            new Spec("item_barrel_skyroot", 6983, "Skyroot", "Iron", 10000, 4, 100, "Skyroot Item Barrel"),
            new Spec("item_barrel_weedwood", 6989, "Weedwood", "Syrmorite", 5000, 4, 0, "Weedwood Item Barrel"),
            new Spec("item_barrel_livingwood", 6988, "Livingwood", "Iron", 10000, 3, 100, "Livingwood Item Barrel"),
            new Spec("item_barrel_dreamwood", 6987, "Dreamwood", "MagicIron", 10000, 5, 0, "Dreamwood Item Barrel"),
            new Spec("item_barrel_shimmerwood", 6986, "Shimmerwood", "MagicIron", 10000, 5, 0, "Shimmerwood Item Barrel"),
            new Spec("item_barrel_ironwood", 6997, "Ironwood", "Iron", 10000, 4, 0, "Ironwood Item Barrel"),
            new Spec("item_barrel_greatwood", 6985, "Greatwood", "Iron", 10000, 3, 100, "Greatwood Item Barrel"),
            new Spec("item_barrel_silverwood", 6984, "Silverwood", "MagicIron", 10000, 5, 0, "Silverwood Item Barrel"));
}
