package com.gregtech.gregtech.content.recipe;

import com.gregtech.gregtech.block.wood.WoodSpecies;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.registry.GTWoods;
import net.minecraft.world.item.ItemStack;

/**
 * GT6's untreated-wood Bath handler and plank ore-dictionary recipes:
 * {@code Loader_Recipes_Handlers.java:660-667} and {@code Loader_Recipes_OreDict.java:257-263}.
 * The {@code plate/WoodTreated} material form resolves to the placeable
 * {@code planks_treated} block, as in GT6 {@code Loader_Woods.java:85-87}.
 */
public final class TreatedWoodRecipes extends OriginalRecipeBatch {
    public static final TreatedWoodRecipes INSTANCE = new TreatedWoodRecipes();
    private static final String[] OILS = {
            "Oil_Seed", "Oil_Lin", "Oil_Hemp", "Oil_Nut", "Oil_Olive", "Oil_Sunflower", "Oil_Creosote"};
    private static final String[] VANILLA_PLANKS = {
            "oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry"};

    private TreatedWoodRecipes() {}

    public static int register() {
        for (String wood : VANILLA_PLANKS) {
            INSTANCE.treat("planks/vanilla/" + wood, item("minecraft:" + wood + "_planks", 1),
                    mat(MaterialPrefix.plate, "WoodTreated", 1));
        }
        for (WoodSpecies species : WoodSpecies.values()) {
            INSTANCE.treat("planks/gregtech/" + species.id(), new ItemStack(GTWoods.planks(species)),
                    mat(MaterialPrefix.plate, "WoodTreated", 1));
        }
        // GT6's material Bath handler also covers these non-dust wood forms. Treating a
        // hand-crafted Wood bolt is the source of the bolt in its wooden gear recipe.
        INSTANCE.treat("plate/wood", mat(MaterialPrefix.plate, "Wood", 1),
                mat(MaterialPrefix.plate, "WoodTreated", 1));
        INSTANCE.treat("stick/vanilla", item("minecraft:stick", 1),
                mat(MaterialPrefix.stick, "WoodTreated", 1));
        INSTANCE.treat("stick/wood", mat(MaterialPrefix.stick, "Wood", 1),
                mat(MaterialPrefix.stick, "WoodTreated", 1));
        INSTANCE.treat("bolt/wood", mat(MaterialPrefix.bolt, "Wood", 1),
                mat(MaterialPrefix.bolt, "WoodTreated", 1));
        return INSTANCE.entries().size();
    }

    private void treat(String source, ItemStack input, ItemStack output) {
        for (String oil : OILS) {
            add("treated/" + source + "/" + oil, MachineRecipeMaps.Bath, 144, 0,
                    items(input.copy()), items(output.copy()), fluids(fluid(oil, 100)), null);
        }
    }

    public static int expectedRows() { return (VANILLA_PLANKS.length + WoodSpecies.values().length + 4) * OILS.length; }
}
