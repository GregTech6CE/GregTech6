package com.gregtech.gregtech.content.recipe;

import com.gregtech.gregtech.data.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

/** Original Vanilla and Other processing for paper, common supplies and material recovery. */
public final class SurvivalUtilityRecipes extends OriginalRecipeBatch {
    public static final SurvivalUtilityRecipes INSTANCE = new SurvivalUtilityRecipes();
    private SurvivalUtilityRecipes() {}
    public static int register() {
        if (!INSTANCE.entries().isEmpty()) throw new IllegalStateException("Survival utilities registered twice");
        INSTANCE.paper();
        INSTANCE.supplies();
        INSTANCE.recovery();
        return INSTANCE.entries().size();
    }
    private void paper() {
        MaterialPrefix[] forms = {MaterialPrefix.dust, MaterialPrefix.dustSmall, MaterialPrefix.dustTiny};
        int[] counts = {1, 4, 9};
        for (String material : new String[]{"Wood", "Bark", "Paper"}) for (int i = 0; i < forms.length; i++)
            for (String water : new String[]{"Water", "MnWtr", "DistW", "SpDew"})
                add("pulp/" + material + "/" + i + "/" + water, MachineRecipeMaps.Bath, 16, 0,
                        items(mat(forms[i], material, counts[i])), items(new ItemStack(Items.PAPER)),
                        fluids(fluid(water, water.equals("DistW") ? 100 : 125)), null);
        add("item_frame", MachineRecipeMaps.Loom, 16, 16, items(new ItemStack(Items.LEATHER), new ItemStack(Items.STICK, 8)),
                items(new ItemStack(Items.ITEM_FRAME)), null, null);
        for (DyeColor color : DyeColor.values())
            add("painting/" + color.getName(), MachineRecipeMaps.Loom, 16, 16,
                    items(item("minecraft:" + color.getName() + "_wool", 1), new ItemStack(Items.STICK, 8)), items(new ItemStack(Items.PAINTING)), null, null);
    }
    private void supplies() {
        for (String water : new String[]{"Water", "MnWtr", "DistW", "SpDew"})
            add("obsidian/" + water, MachineRecipeMaps.Mixer, 16, 16, null, items(new ItemStack(Items.OBSIDIAN)),
                    fluids(fluid(water, 50), new FluidStack(Fluids.LAVA, 1000)), null);
        add("enchanted_apple", MachineRecipeMaps.Bath, 128, 0, items(new ItemStack(Items.GOLDEN_APPLE)), items(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE)),
                fluids(fluid("GenMolten_Gold", 9216)), null);
        add("golden_carrot", MachineRecipeMaps.Bath, 16, 0, items(new ItemStack(Items.CARROT)), items(new ItemStack(Items.GOLDEN_CARROT)),
                fluids(fluid("GenMolten_Gold", 128)), null);
        add("glistering_melon", MachineRecipeMaps.Bath, 16, 0, items(new ItemStack(Items.MELON_SLICE)), items(new ItemStack(Items.GLISTERING_MELON_SLICE)),
                fluids(fluid("GenMolten_Gold", 128)), null);
        mix("glistering_melon", 16, new ItemStack(Items.GLISTERING_MELON_SLICE), new ItemStack(Items.MELON_SLICE), mat(MaterialPrefix.nugget, "Gold", 8));
        mix("ender_eye", 16, mat(MaterialPrefix.dust, "EnderEye", 1), mat(MaterialPrefix.dust, "EnderPearl", 1), mat(MaterialPrefix.dustTiny, "Blaze", 1));
        mix("ender_eye_bulk", 144, mat(MaterialPrefix.dust, "EnderEye", 9), mat(MaterialPrefix.dust, "EnderPearl", 9), mat(MaterialPrefix.dust, "Blaze", 1));
        mix("fermented_eye", 16, new ItemStack(Items.FERMENTED_SPIDER_EYE), mat(MaterialPrefix.dust, "Sugar", 1), new ItemStack(Items.SPIDER_EYE), new ItemStack(Items.BROWN_MUSHROOM));
        mix("fermented_eye_crystals", 16, new ItemStack(Items.FERMENTED_SPIDER_EYE), mat(MaterialPrefix.gemChipped, "Sugar", 4), new ItemStack(Items.SPIDER_EYE), new ItemStack(Items.BROWN_MUSHROOM));
        for (String carbon : new String[]{"Coal", "Charcoal", "CoalCoke", "LigniteCoke"})
            mix("fire_charge/" + carbon, 16, new ItemStack(Items.FIRE_CHARGE, 3), mat(MaterialPrefix.dust, carbon, 1), mat(MaterialPrefix.dustTiny, "Blaze", 1), mat(MaterialPrefix.dust, "Gunpowder", 1));
        add("silica/sand_block", MachineRecipeMaps.Electrolyzer, 576, 64, items(selector(0), new ItemStack(Items.SAND)), items(mat(MaterialPrefix.dust, "SiO2", 9)), null, null);
        add("silica/sand_dust", MachineRecipeMaps.Electrolyzer, 64, 64, items(selector(0), mat(MaterialPrefix.dust, "Sand", 1)), items(mat(MaterialPrefix.dust, "SiO2", 1)), null, null);
        add("bucket", MachineRecipeMaps.RollBender, 256, 16, items(mat(MaterialPrefix.plateCurved, "Iron", 3)), items(new ItemStack(Items.BUCKET)), null, null);
        add("chisel/stone", MachineRecipeMaps.Chisel, 16, 16, items(new ItemStack(Items.STONE)), items(new ItemStack(Items.CHISELED_STONE_BRICKS)), null, null);
        add("chisel/bricks", MachineRecipeMaps.Chisel, 16, 16, items(new ItemStack(Items.STONE_BRICKS)), items(new ItemStack(Items.CRACKED_STONE_BRICKS)), null, null);
    }
    private void recovery() {
        Item[] armor = {Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS, Items.LEATHER_BOOTS};
        int[] counts = {1, 2, 2, 1};
        for (int i = 0; i < armor.length; i++)
            add("leather_recovery/" + i, MachineRecipeMaps.Slicer, 16, 16,
                    items(new ItemStack(armor[i]), item("gregtech:slicer_shape_split", 1)), items(new ItemStack(Items.LEATHER, counts[i])), null, null);
    }
    private void mix(String id, int ticks, ItemStack output, ItemStack... inputs) {
        add("mix/" + id, MachineRecipeMaps.Mixer, ticks, 16, inputs, items(output), null, null);
    }
}
