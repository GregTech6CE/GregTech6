package com.gregtech.gregtech.content.recipe;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTValues;
import com.gregtech.gregtech.api.material.ItemMaterialRegistry;
import com.gregtech.gregtech.api.recipe.Recipe;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.registry.GTItems;
import com.gregtech.gregtech.registry.GTTechnological;
import com.gregtech.gregtech.content.material.Materials;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/** The processing rows of GT6 {@code BlockDiggable:69-96} and {@code MultiItemFood:123-165}. */
public final class DiggableRecipes {
    public record Variant(String block, String ball, GTMaterial material) {}

    // Metadata 0, 1, 3, 4, 5, 6. Metadata 2 (turf) has a peat composition and ingot drops,
    // but GT6 registers no drying, firing, or four-ball packing recipe for it.
    private static final List<Variant> PACKABLE = List.of(
            new Variant("mud", "mud_2", null),
            new Variant("clay_brown", "brown_clay", Materials.ClayBrown),
            new Variant("clay_red", "red_clay", Materials.ClayRed),
            new Variant("clay_yellow", "yellow_clay", Materials.Bentonite),
            new Variant("clay_blue", "blue_clay", Materials.Palygorskite),
            new Variant("clay_white", "white_clay", Materials.Kaolinite));

    private static final List<Recipe> ENTRIES = new ArrayList<>();
    private static boolean registered;
    private static int registeredCount;

    private DiggableRecipes() {}

    public static List<Variant> packable() { return PACKABLE; }
    public static List<Recipe> entries() { return List.copyOf(ENTRIES); }

    /** GT6 {@code OM.data}: five clay blocks and turf contain four units; clay balls one unit. */
    public static void registerMaterials() {
        for (Variant row : PACKABLE) {
            if (row.material() == null) continue; // GT6 UNUSED.Mud has no composition.
            ItemMaterialRegistry.register(item(row.block()), null, row.material(), GTValues.U * 4);
            ItemMaterialRegistry.register(item(row.ball()), null, row.material(), GTValues.U);
        }
        ItemMaterialRegistry.register(item("turf"), null, Materials.Peat, GTValues.U * 4);
        // Earlier port ids remain placeable in saved worlds and represent these same two variants.
        ItemMaterialRegistry.register(item("diggable_clay"), null, Materials.ClayBrown, GTValues.U * 4);
        ItemMaterialRegistry.register(item("diggable_peat"), null, Materials.Peat, GTValues.U * 4);
    }

    public static int register() {
        if (registered) throw new IllegalStateException("Diggable processing rows registered twice");
        registered = true;
        // BlockDiggable:75 includes vanilla clay alongside the five colored clay blocks.
        add(MachineRecipeMaps.Drying.addRecipe1(true, 16, 64,
                new ItemStack(Blocks.CLAY), new ItemStack(Blocks.TERRACOTTA)), "dry vanilla clay");
        // MultiItemFood:159-165 also packs and unpacks the ordinary vanilla clay block.
        ItemStack vanillaBalls = new ItemStack(Items.CLAY_BALL, 4);
        ItemStack vanillaClay = new ItemStack(Blocks.CLAY);
        add(MachineRecipeMaps.Boxinator.addRecipe2(true, 16, 16, vanillaBalls,
                new ItemStack(GTTechnological.selectorTag(4)), vanillaClay), "box vanilla clay");
        add(MachineRecipeMaps.Compressor.addRecipe1(true, 16, 16, vanillaBalls, vanillaClay),
                "compress vanilla clay");
        add(MachineRecipeMaps.Unboxinator.addRecipe1(true, 16, 16, vanillaClay, vanillaBalls),
                "unbox vanilla clay");
        for (Variant row : PACKABLE) {
            ItemStack block = stack(row.block(), 1);
            ItemStack balls = stack(row.ball(), 4);
            ItemStack dried = row.material() == null
                    ? new ItemStack(Blocks.COARSE_DIRT) : new ItemStack(Blocks.TERRACOTTA);

            add(MachineRecipeMaps.Drying.addRecipe1(true, 16, 64, block, dried),
                    "dry " + row.block());
            accept(MachineRecipeMaps.add_smelting(block, dried, 0, false, false,
                    row.material() != null), "furnace " + row.block());

            // RM.compactunpack: Boxinator with selector 4, Compressor, and Unboxinator.
            add(MachineRecipeMaps.Boxinator.addRecipe2(true, 16, 16,
                    balls, new ItemStack(GTTechnological.selectorTag(4)), block),
                    "box " + row.block());
            add(MachineRecipeMaps.Compressor.addRecipe1(true, 16, 16, balls, block),
                    "compress " + row.block());
            add(MachineRecipeMaps.Unboxinator.addRecipe1(true, 16, 16, block, balls),
                    "unbox " + row.block());

            // RM.generify is a directed Generifier conversion, not a global clay-material alias.
            if (row.material() != null) {
                accept(MachineRecipeMaps.generify(block, new ItemStack(Blocks.CLAY)),
                        "generify block " + row.block());
                accept(MachineRecipeMaps.generify(stack(row.ball(), 1), new ItemStack(Items.CLAY_BALL)),
                        "generify ball " + row.ball());
            }
        }
        // MultiItemFood:168-178: the ordinary ball and five colored balls roll into plates,
        // while one dust unit compresses back into the matching ball.
        clayBallForms(Materials.Clay, new ItemStack(Items.CLAY_BALL), "vanilla clay");
        for (Variant row : PACKABLE) if (row.material() != null)
            clayBallForms(row.material(), stack(row.ball(), 1), row.ball());
        return registeredCount;
    }

    private static void clayBallForms(GTMaterial material, ItemStack ball, String name) {
        ItemStack plate = GTItems.getStack(MaterialPrefix.plate, material);
        ItemStack dust = GTItems.getStack(MaterialPrefix.dust, material);
        if (plate.isEmpty() || dust.isEmpty())
            throw new IllegalStateException("Missing GT6 clay plate/dust form for " + name);
        add(MachineRecipeMaps.RollingMill.addRecipe1(true, 16, 32, ball, plate),
                "roll " + name);
        add(MachineRecipeMaps.Compressor.addRecipe1(true, 16, 16, dust, ball),
                "compress clay dust " + name);
    }

    private static void add(Recipe recipe, String source) {
        if (recipe == null) throw new IllegalStateException("Rejected GT6 diggable row: " + source);
        ENTRIES.add(recipe);
        registeredCount++;
    }

    private static void accept(boolean registered, String source) {
        if (!registered) throw new IllegalStateException("Rejected GT6 diggable row: " + source);
        registeredCount++;
    }

    private static Item item(String id) {
        Item value = ForgeRegistries.ITEMS.getValue(GregTech.id(id));
        if (value == null || value == Items.AIR) throw new IllegalStateException("Missing GT6 diggable item " + id);
        return value;
    }

    private static ItemStack stack(String id, int count) { return new ItemStack(item(id), count); }
}
