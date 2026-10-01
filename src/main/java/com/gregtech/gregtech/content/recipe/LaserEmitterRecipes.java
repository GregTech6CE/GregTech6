package com.gregtech.gregtech.content.recipe;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.api.recipe.Recipe;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.registry.GTTechnological;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * GT6's gas laser emitters: filled in the Canning Machine from the empty emitter plus one unit of
 * the laser gas ({@code MultiItemTechnological:396-403}).
 *
 * <p>GT6 defines nine emitters — an empty one and eight gases — and fills them with
 * {@code RM.Canner.addRecipe1(T, 16, 128, IL.Comp_Laser_Gas_Empty.get(1), <gas>.gas(U, T), NF, <filled>)}.
 * The port registered all nine items (they are used by the Nanoscale Fabricator's recipe) but had no
 * way to make any of them, so the Nanofab was unreachable in survival.</p>
 *
 * <p>The empty emitter's crafting recipe is a datapack recipe
 * ({@code data/gregtech/recipes/components/laser_emitter_emptygas.json}) because it is an ordinary
 * shaped recipe; the gas filling lives here because it is a machine recipe.</p>
 */
public final class LaserEmitterRecipes {
    /** GT6 gas -> port emitter item id, in the original's order. */
    private static final String[][] GASES = {
            {"Helium", "laser_emitter_helium"},
            {"Neon", "laser_emitter_neon"},
            {"Argon", "laser_emitter_argon"},
            {"Krypton", "laser_emitter_krypton"},
            {"Xenon", "laser_emitter_xenon"},
            {"HeliumNeon", "laser_emitter_heliumneon"},
            {"CarbonMonoxide", "laser_emitter_carbonmonoxide"},
            {"CarbonDioxide", "laser_emitter_carbondioxide"}};

    private static final List<String> ENTRIES = new ArrayList<>();
    private static final List<String> SKIPPED = new ArrayList<>();

    private static boolean registered;

    private LaserEmitterRecipes() {}

    public static List<String> entries() { return Collections.unmodifiableList(ENTRIES); }

    public static List<String> skipped() { return Collections.unmodifiableList(SKIPPED); }

    public static int register() {
        if (registered) throw new IllegalStateException("Laser emitter recipes registered twice");
        registered = true;
        ItemStack empty = item("laser_emitter_emptygas");
        if (empty.isEmpty()) {
            SKIPPED.add("laser emitter rows: the port has no laser_emitter_emptygas item");
            return 0;
        }
        for (String[] gas : GASES) {
            ItemStack filled = item(gas[1]);
            if (filled.isEmpty()) {
                SKIPPED.add(gas[1] + ": the port registers no such emitter");
                continue;
            }
            FluidStack fluid = gas(gas[0], 1000);
            if (fluid == null) {
                SKIPPED.add(gas[0] + ": the port registers no such gas");
                continue;
            }
            // MultiItemTechnological:396-403 - T, 16 EU/t, 128 ticks
            Recipe recipe = MachineRecipeMaps.Canner.addRecipe(new Recipe(
                    new ItemStack[]{empty.copy()}, new ItemStack[]{filled}, null, null,
                    new FluidStack[]{fluid}, null, 128, 16, 0));
            if (recipe != null) ENTRIES.add(gas[0] + " -> " + gas[1]);
        }
        GregTech.LOGGER.info("Registered {} GT6 laser emitter filling recipes ({} skipped: {})",
                ENTRIES.size(), SKIPPED.size(), SKIPPED);
        return ENTRIES.size();
    }

    /** The material's gas phase, registered either as a dedicated fluid or under its material name. */
    private static FluidStack gas(String materialName, int mb) {
        // GTGeneratedChem resolves a material's gas exactly like the transpiled GT6 rows do
        var material = com.gregtech.gregtech.api.material.GTMaterialRegistry.get(materialName);
        if (material != null && material.isValid()) {
            FluidStack stack = com.gregtech.gregtech.loaders.c.GTGeneratedChem.materialFluid(material.getName(), mb);
            if (stack != null) return stack;
        }
        var still = com.gregtech.gregtech.registry.GTFluids.still(materialName);
        if (still != null && still.isPresent()) return new FluidStack(still.get(), mb);
        // port fluid keys use the lower-cased registry name for generated gases
        var generated = com.gregtech.gregtech.registry.GTFluids.still("GenGas_" + materialName);
        return generated != null && generated.isPresent() ? new FluidStack(generated.get(), mb) : null;
    }

    private static ItemStack item(String id) {
        var item = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath(GregTech.MODID, id));
        return item == null || item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    /** The empty emitter itself, so the coverage report can name its crafting recipe. */
    public static ItemStack emptyEmitter() {
        return GTTechnological.get("laser_emitter_emptygas") == null
                ? ItemStack.EMPTY : new ItemStack(GTTechnological.get("laser_emitter_emptygas"));
    }
}
