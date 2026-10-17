/* GregTech-6 Team / Gregorius Techneticies, LGPL-3.0-or-later.
 * MultiItemFood:847-900; Loader_Recipes_Food food listeners. */
package com.gregtech.gregtech.content.food;
public final class IceCreamRecipes {
    private IceCreamRecipes() {}
    public record DustFlavor(String base, String material, String result) {}
    public record Blend(String first, String second, String third, String result) {}
    public static final java.util.List<DustFlavor> DUSTS=java.util.List.of(
        new DustFlavor("ice_cream","Chocolate","stracciatella_ice_cream"),
        new DustFlavor("ice_cream","Vanilla","vanilla_ice_cream"),
        new DustFlavor("ice_cream","Coffee","mocha_ice_cream"),
        new DustFlavor("ice_cream","Mint","mint_ice_cream"),
        new DustFlavor("ice_cream","Pistachio","pistachio_ice_cream"),
        new DustFlavor("ice_cream","MeatCooked","bear_ice_cream"),
        new DustFlavor("mint_ice_cream","Chocolate","mint_chocolate_chip_ice_cream"));
    public static final java.util.List<Blend> BLENDS=java.util.List.of(
        new Blend("strawberry_ice_cream","vanilla_ice_cream","chocolate_ice_cream","neapolitan_ice_cream"),
        new Blend("cherry_ice_cream","pistachio_ice_cream","vanilla_ice_cream","spumoni_vanilla_ice_cream"),
        new Blend("cherry_ice_cream","pistachio_ice_cream","chocolate_ice_cream","spumoni_chocolate_ice_cream"),
        new Blend("cherry_ice_cream","blueberry_ice_cream","lemon_ice_cream","superman_ice_cream"));
}
