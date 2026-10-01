package com.gregtech.gregtech.content.recipe;

import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.data.RegisteredFluids;
import com.gregtech.gregtech.registry.GTFluids;
import com.gregtech.gregtech.worldgen.GTSurfaceFlora;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** GT6 {@code BlockGlowtus.run}: mortar/shredder and {@code RM.biomass(4)} for every colour. */
public final class GlowtusProcessingRecipes extends OriginalRecipeBatch {
    public static final GlowtusProcessingRecipes INSTANCE = new GlowtusProcessingRecipes();

    private GlowtusProcessingRecipes() {}

    public static int register() {
        if (!INSTANCE.entries().isEmpty()) throw new IllegalStateException("Glowtus recipes registered twice");
        for (String colour : GTSurfaceFlora.GLOWTUS_COLOURS) INSTANCE.registerColour(colour);
        return INSTANCE.entries().size();
    }

    private void registerColour(String colour) {
        ItemStack glowtus = new ItemStack(GTSurfaceFlora.glowtus(colour));
        if (glowtus.isEmpty()) throw new IllegalStateException("Missing glowtus_" + colour);
        // GT6 RM.mortarize sends the same 16/t × 16 tick row to both machines.
        ItemStack smallDust = mat(MaterialPrefix.dustSmall, "Glowstone", 1);
        add(colour + "/mortar", MachineRecipeMaps.Mortar, 16, 16,
                items(glowtus.copy()), items(smallDust.copy()), null, null);
        add(colour + "/shredder", MachineRecipeMaps.Shredder, 16, 16,
                items(glowtus.copy()), items(smallDust.copy()), null, null);

        // The 4 passed to RM.biomass scales its per-plant fluid amounts and duration.
        // It consumes one Glowtus in each recipe; the original stack size is a divisor.
        Set<String> seenFluids = new HashSet<>();
        biomass(colour, glowtus, "Rotten_Drink", 270, 810, 32, seenFluids);
        biomass(colour, glowtus, "Soup_Mushroom", 270, 810, 32, seenFluids);
        List<String> fluidFields = new ArrayList<>(RegisteredFluids.all().keySet());
        fluidFields.sort(Comparator.naturalOrder());
        for (String field : fluidFields) {
            RegisteredFluids.FluidEntry entry = RegisteredFluids.get(field);
            if (entry.hasFlag(RegisteredFluids.FluidFlags.WATER))
                biomass(colour, glowtus, field, 270, 270, 64, seenFluids);
            else if (entry.hasFlag(RegisteredFluids.FluidFlags.MILK))
                biomass(colour, glowtus, field, 270, 540, 48, seenFluids);
            else if (entry.hasFlag(RegisteredFluids.FluidFlags.JUICE)
                    || entry.hasFlag(RegisteredFluids.FluidFlags.HONEY))
                biomass(colour, glowtus, field, 270, 810, 48, seenFluids);
        }
        biomass(colour, glowtus, "Honeydew", 270, 810, 32, seenFluids);
        biomass(colour, glowtus, "RoyalJelly", 270, 3140, 32, seenFluids);
    }

    private void biomass(String colour, ItemStack glowtus, String fluid, int inputMb,
                         int outputMb, int ticks, Set<String> seenFluids) {
        FluidStack input = GTFluids.stack(fluid, inputMb);
        FluidStack output = GTFluids.stack("BiomassIC2", outputMb);
        if (input == null || output == null) throw new IllegalStateException("Missing Glowtus fluid " + fluid);
        String fluidId = net.minecraftforge.registries.ForgeRegistries.FLUIDS.getKey(input.getFluid()).toString();
        if (!seenFluids.add(fluidId)) return; // field aliases can identify the same actual fluid.
        add(colour + "/biomass/" + fluid, MachineRecipeMaps.Fermenter, ticks, 16,
                items(glowtus.copy()), null, fluids(input), fluids(output));
    }
}
