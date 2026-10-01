package com.gregtech.gregtech.content.recipe;

import com.gregtech.gregtech.content.plant.BedrockFlowers;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.data.RegisteredFluids;
import com.gregtech.gregtech.registry.GTFluids;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** GT6 BlockFlowersA/B.run: mortar, shredder, dye pressing and RM.biomass(8). */
public final class BedrockFlowerProcessingRecipes extends OriginalRecipeBatch {
    public static final BedrockFlowerProcessingRecipes INSTANCE = new BedrockFlowerProcessingRecipes();

    private BedrockFlowerProcessingRecipes() {}

    public static int register() {
        if (!INSTANCE.entries().isEmpty()) throw new IllegalStateException("Bedrock flower recipes registered twice");
        for (BedrockFlowers.Flower flower : BedrockFlowers.ALL) INSTANCE.registerFlower(flower);
        return INSTANCE.entries().size();
    }

    private void registerFlower(BedrockFlowers.Flower flower) {
        String id = flower.id();
        ItemStack input = item("gregtech:" + id, 1);

        String grind = flower.family() == 'A' && flower.meta() < 2 ? "Wheat"
                : flower.family() == 'B' && flower.meta() < 2 ? "Acacia"
                : flower.family() == 'B' && flower.meta() == 6 ? "Palm" : null;
        if (grind != null) {
            ItemStack dust = mat(MaterialPrefix.dust, grind, 1);
            add(id + "/mortar", MachineRecipeMaps.Mortar, 16, 16,
                    items(input.copy()), items(dust.copy()), null, null);
            add(id + "/shredder", MachineRecipeMaps.Shredder, 16, 16,
                    items(input.copy()), items(dust.copy()), null, null);
        }

        if (flower.family() == 'A' && flower.meta() < 8) {
            String[] dye = {"Yellow", "Yellow", "Magenta", "Yellow",
                    "Pink", "White", "LightBlue", "Brown"};
            String dust = flower.meta() < 2 ? "Wheat" : dye[flower.meta()];
            pressDye(id, input, dye[flower.meta()], mat(MaterialPrefix.dust, dust, 1));
        } else if (flower.family() == 'B' && flower.meta() >= 2 && flower.meta() <= 5) {
            String[] dye = {"Yellow", "Pink", "Yellow", "Purple"};
            String colour = dye[flower.meta() - 2];
            pressDye(id, input, colour, mat(MaterialPrefix.dust, colour, 1));
        } else if (flower.family() == 'B' && flower.meta() == 7) {
            // GT6 IL.Dye_Cactus is Items.dye meta 2: vanilla green dye in 1.20.1.
            ItemStack green = new ItemStack(Items.GREEN_DYE, 2);
            add(id + "/squeezer", MachineRecipeMaps.Squeezer, 16, 16,
                    items(input.copy()), items(green.copy()), null, fluids(fluid("Juice_Cactus", 100)));
            add(id + "/juicer", MachineRecipeMaps.Juicer, 16, 16,
                    items(input.copy()), items(green.copy()), null, fluids(fluid("Juice_Cactus", 75)));
        }

        // RM.biomass(ST.make(this, 8, W)) consumes one flower and divides fluid quantities by 8.
        Set<String> seenFluids = new HashSet<>();
        biomass(id, input, "Rotten_Drink", 135, 405, 16, seenFluids);
        biomass(id, input, "Soup_Mushroom", 135, 405, 16, seenFluids);
        List<String> fields = new ArrayList<>(RegisteredFluids.all().keySet());
        fields.sort(Comparator.naturalOrder());
        for (String field : fields) {
            var entry = RegisteredFluids.get(field);
            if (entry.hasFlag(RegisteredFluids.FluidFlags.WATER))
                biomass(id, input, field, 135, 135, 32, seenFluids);
            else if (entry.hasFlag(RegisteredFluids.FluidFlags.MILK))
                biomass(id, input, field, 135, 270, 24, seenFluids);
            else if (entry.hasFlag(RegisteredFluids.FluidFlags.JUICE)
                    || entry.hasFlag(RegisteredFluids.FluidFlags.HONEY))
                biomass(id, input, field, 135, 405, 24, seenFluids);
        }
        biomass(id, input, "Honeydew", 135, 405, 16, seenFluids);
        biomass(id, input, "RoyalJelly", 135, 1570, 16, seenFluids);
    }

    private void pressDye(String id, ItemStack flower, String colour, ItemStack dust) {
        FluidStack dye = fluid("Dye_Flower_" + colour, 144);
        for (var map : List.of(MachineRecipeMaps.Squeezer, MachineRecipeMaps.Juicer)) {
            add(id + "/" + (map == MachineRecipeMaps.Squeezer ? "squeezer" : "juicer"),
                    map, 16, 16, items(flower.copy()), items(dust.copy()), null, fluids(dye.copy()));
        }
    }

    private void biomass(String id, ItemStack flower, String field, int inputMb,
                         int outputMb, int ticks, Set<String> seenFluids) {
        FluidStack liquid = GTFluids.stack(field, inputMb);
        FluidStack result = GTFluids.stack("BiomassIC2", outputMb);
        if (liquid == null || result == null)
            throw new IllegalStateException("Missing GT6 bedrock flower fluid " + field);
        String fluidId = ForgeRegistries.FLUIDS.getKey(liquid.getFluid()).toString();
        if (!seenFluids.add(fluidId)) return;
        add(id + "/biomass/" + field, MachineRecipeMaps.Fermenter, ticks, 16,
                items(flower.copy()), null, fluids(liquid), fluids(result));
    }
}
