package com.gregtech.gregtech.content.recipe;

import com.gregtech.gregtech.data.*;
import net.minecraft.world.item.*;
import net.minecraftforge.fluids.FluidStack;
import java.util.Locale;

/** GT6 Loader_Recipes_Vanilla: dye baths, chlorine bleaching, paper and loom products. */
public final class TextileFinishingRecipes extends OriginalRecipeBatch {
    public static final TextileFinishingRecipes INSTANCE = new TextileFinishingRecipes();
    private TextileFinishingRecipes() {}
    public static int register() {
        if (!INSTANCE.entries().isEmpty()) throw new IllegalStateException("Textiles registered twice");
        INSTANCE.colors();
        INSTANCE.loom();
        return INSTANCE.entries().size();
    }
    private void colors() {
        for (String color : RegisteredFluids.DYE_OREDICTS_POST) {
            String name = color.replace("LightBlue", "light_blue").replace("LightGray", "light_gray").toLowerCase(Locale.ROOT);
            for (String dye : new String[]{"Water", "Flower", "Chemical"}) {
                String field = "Dye_" + dye + "_" + color;
                if (!color.equals("White")) {
                    bath(field + "/wool", "white_wool", name + "_wool", fluid(field, 9));
                    bath(field + "/carpet", "white_carpet", name + "_carpet", fluid(field, 5));
                }
                bath(field + "/terracotta", "terracotta", name + "_terracotta", fluid(field, 9));
                bath(field + "/glass", "glass", name + "_stained_glass", fluid(field, 9));
                bath(field + "/pane", "glass_pane", name + "_stained_glass_pane", fluid(field, 4));
            }
            if (!color.equals("White")) {
                bleach(name + "_wool", "white_wool", 50);
                bleach(name + "_carpet", "white_carpet", 25);
            }
            bleach(name + "_terracotta", "terracotta", 50);
            bleach(name + "_stained_glass", "glass", 50);
            bleach(name + "_stained_glass_pane", "glass_pane", 20);
        }
        for (String water : new String[]{"Water", "MnWtr", "DistW", "SpDew"})
            bath("paper/" + water, "sugar_cane", "paper", fluid(water, water.equals("DistW") ? 100 : 125));
    }
    private void bleach(String input, String output, int amount) {
        bath("bleach/" + input, input, output, fluid("GenGas_Chlorine", amount));
    }
    private void bath(String id, String input, String output, FluidStack liquid) {
        add(id, MachineRecipeMaps.Bath, 16, 0, items(item("minecraft:" + input, 1)),
                items(item("minecraft:" + output, 1)), fluids(liquid), null);
    }
    private void loom() {
        loom("wool", 16, 16, new ItemStack(Items.WHITE_WOOL), selector(0), new ItemStack(Items.STRING, 4));
        loom("cobweb", 64, 16, new ItemStack(Items.COBWEB), selector(1), new ItemStack(Items.STRING, 4));
        loom("paper", 16, 16, new ItemStack(Items.PAPER), selector(0), new ItemStack(Items.SUGAR_CANE));
        Item[] leather = {Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS, Items.LEATHER_BOOTS};
        Item[] chain = {Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE, Items.CHAINMAIL_LEGGINGS, Items.CHAINMAIL_BOOTS};
        int[] counts = {5, 8, 7, 4};
        for (int i = 0; i < 4; i++) {
            loom("leather_armor/" + i, 128, 16, new ItemStack(leather[i]), selector(i + 4), new ItemStack(Items.LEATHER, counts[i]));
            loom("chain_armor/" + i, 128, 96, new ItemStack(chain[i]), selector(i + 4), mat(MaterialPrefix.ring, "Steel", counts[i]));
        }
        loom("horse_iron", 128, 64, new ItemStack(Items.IRON_HORSE_ARMOR), new ItemStack(Items.LEATHER, 6), mat(MaterialPrefix.plate, "Iron", 8));
        loom("horse_gold", 128, 64, new ItemStack(Items.GOLDEN_HORSE_ARMOR), new ItemStack(Items.LEATHER, 6), mat(MaterialPrefix.plate, "Gold", 8));
        loom("horse_diamond", 128, 64, new ItemStack(Items.DIAMOND_HORSE_ARMOR), new ItemStack(Items.LEATHER, 6), mat(MaterialPrefix.plateGem, "Diamond", 8));
        loom("saddle", 128, 64, new ItemStack(Items.SADDLE), new ItemStack(Items.LEATHER, 6), mat(MaterialPrefix.ring, "Steel", 2), mat(MaterialPrefix.stick, "Steel", 3));
    }
    private void loom(String id, int ticks, int power, ItemStack output, ItemStack... inputs) {
        add("loom/" + id, MachineRecipeMaps.Loom, ticks, power, inputs, items(output), null, null);
    }
}
