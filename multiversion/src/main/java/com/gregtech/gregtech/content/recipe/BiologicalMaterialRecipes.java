package com.gregtech.gregtech.content.recipe;

import com.gregtech.gregtech.data.*;
import net.minecraft.world.item.*;

/** GT6 OreDict slimeball, Other itemResin, and Vanilla slime centrifuging/magma cream. */
public final class BiologicalMaterialRecipes extends OriginalRecipeBatch {
    public static final BiologicalMaterialRecipes INSTANCE = new BiologicalMaterialRecipes();
    private BiologicalMaterialRecipes() {}
    public static int register() {
        if (!INSTANCE.entries().isEmpty()) throw new IllegalStateException("Biomaterials registered twice");
        INSTANCE.slime();
        INSTANCE.resin();
        return INSTANCE.entries().size();
    }
    private void slime() {
        add("slimeball/squeeze", MachineRecipeMaps.Squeezer, 64, 16, items(new ItemStack(Items.SLIME_BALL)), null, null, fluids(fluid("Slime_Green", 250)));
        add("slimeball/juice", MachineRecipeMaps.Juicer, 64, 16, items(new ItemStack(Items.SLIME_BALL)), null, null, fluids(fluid("Slime_Green", 125)));
        for (String slime : new String[]{"Slime_Green", "Slime_Blue", "Slime_Pink"}) {
            add(slime + "/latex_glue", MachineRecipeMaps.Centrifuge, 64, 16, null, null,
                    fluids(fluid(slime, 250)), fluids(fluid("Latex", 72), fluid("Glue", 250)));
            add(slime + "/magma", MachineRecipeMaps.Mixer, 16, 16, items(mat(MaterialPrefix.dustTiny, "Blaze", 1)), items(new ItemStack(Items.MAGMA_CREAM)),
                    fluids(fluid(slime, 250)), null);
            add(slime + "/magma_bulk", MachineRecipeMaps.Mixer, 144, 16, items(mat(MaterialPrefix.dust, "Blaze", 1)), items(new ItemStack(Items.MAGMA_CREAM, 9)),
                    fluids(fluid(slime, 2250)), null);
        }
        add("magma/recover", MachineRecipeMaps.Centrifuge, 16, 16, items(new ItemStack(Items.MAGMA_CREAM)), items(new ItemStack(Items.BLAZE_POWDER)),
                null, fluids(fluid("Slime_Green", 125)));
        add("slimy_bone/recover", MachineRecipeMaps.Centrifuge, 16, 16, items(mat(MaterialPrefix.dust, "SlimyBone", 1)), items(mat(MaterialPrefix.dust, "Bone", 1)),
                null, fluids(fluid("Slime_Green", 250)));
    }
    private void resin() {
        add("resin/squeeze", MachineRecipeMaps.Squeezer, 64, 16, items(item("gregtech:rubber_resin", 1)), null, null, fluids(fluid("Latex", 144)));
        add("resin/juice", MachineRecipeMaps.Juicer, 64, 16, items(item("gregtech:rubber_resin", 1)), null, null, fluids(fluid("Latex", 72)));
        add("resin/separate", MachineRecipeMaps.Centrifuge, 64, 16, items(item("gregtech:rubber_resin", 1)), null, null,
                fluids(fluid("Latex", 144), fluid("Glue", 250)));
        for (String water : new String[]{"Water", "MnWtr", "DistW", "SpDew"})
            add("resin/glue/" + water, MachineRecipeMaps.Mixer, 16, 16, items(item("gregtech:rubber_resin", 1)), null,
                    fluids(fluid(water, 250)), fluids(fluid("Glue", 250)));
        add("resin/piston", MachineRecipeMaps.Laminator, 16, 16, items(item("gregtech:rubber_resin", 1), new ItemStack(Items.PISTON)),
                items(new ItemStack(Items.STICKY_PISTON)), null, null);
    }
}
