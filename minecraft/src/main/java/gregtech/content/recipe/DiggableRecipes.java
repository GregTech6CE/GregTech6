package gregtech.content.recipe;

import gregtech.api.material.GTMaterial;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.material.Materials;
import gregtech.content.registry.GTItems;
import gregtech.content.registry.GTTechnological;
import gregtech.engine.recipe.Recipe;
import gregtech.engine.recipe.RecipeBuilder;
import gregtech.platform.Mc;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/** The processing rows of GT6 {@code BlockDiggable} and {@code MultiItemFood}. */
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

    public static List<Variant> packable() {
        return PACKABLE;
    }

    public static List<Recipe> entries() {
        return List.copyOf(ENTRIES);
    }

    public static int register() {
        if (registered) throw new IllegalStateException("Diggable processing rows registered twice");
        registered = true;
        // BlockDiggable includes vanilla clay alongside the five colored clay blocks.
        add(
                MachineRecipeMaps.Drying.register(RecipeBuilder.of(16, 64)
                        .optimize(true)
                        .itemInputs(new ItemStack(Blocks.CLAY))
                        .itemOutputs(new ItemStack(Blocks.TERRACOTTA))),
                "dry vanilla clay");
        // MultiItemFood also packs and unpacks the ordinary vanilla clay block.
        ItemStack vanillaBalls = new ItemStack(Items.CLAY_BALL, 4);
        ItemStack vanillaClay = new ItemStack(Blocks.CLAY);
        add(
                MachineRecipeMaps.Boxinator.register(RecipeBuilder.of(16, 16)
                        .optimize(true)
                        .itemInputs(vanillaBalls, new ItemStack(GTTechnological.selectorTag(4)))
                        .itemOutputs(vanillaClay)),
                "box vanilla clay");
        add(
                MachineRecipeMaps.Compressor.register(RecipeBuilder.of(16, 16)
                        .optimize(true)
                        .itemInputs(vanillaBalls)
                        .itemOutputs(vanillaClay)),
                "compress vanilla clay");
        add(
                MachineRecipeMaps.Unboxinator.register(RecipeBuilder.of(16, 16)
                        .optimize(true)
                        .itemInputs(vanillaClay)
                        .itemOutputs(vanillaBalls)),
                "unbox vanilla clay");
        for (Variant row : PACKABLE) {
            ItemStack block = stack(row.block(), 1);
            ItemStack balls = stack(row.ball(), 4);
            ItemStack dried =
                    row.material() == null ? new ItemStack(Blocks.COARSE_DIRT) : new ItemStack(Blocks.TERRACOTTA);

            add(
                    MachineRecipeMaps.Drying.register(RecipeBuilder.of(16, 64)
                            .optimize(true)
                            .itemInputs(block)
                            .itemOutputs(dried)),
                    "dry " + row.block());
            accept(MachineRecipeMaps.addSmelting(block, dried), "furnace " + row.block());

            // RM.compactunpack: Boxinator with selector 4, Compressor, and Unboxinator.
            add(
                    MachineRecipeMaps.Boxinator.register(RecipeBuilder.of(16, 16)
                            .optimize(true)
                            .itemInputs(balls, new ItemStack(GTTechnological.selectorTag(4)))
                            .itemOutputs(block)),
                    "box " + row.block());
            add(
                    MachineRecipeMaps.Compressor.register(RecipeBuilder.of(16, 16)
                            .optimize(true)
                            .itemInputs(balls)
                            .itemOutputs(block)),
                    "compress " + row.block());
            add(
                    MachineRecipeMaps.Unboxinator.register(RecipeBuilder.of(16, 16)
                            .optimize(true)
                            .itemInputs(block)
                            .itemOutputs(balls)),
                    "unbox " + row.block());

            // RM.generify is a directed Generifier conversion, not a global clay-material alias.
            if (row.material() != null) {
                accept(MachineRecipeMaps.generify(block, new ItemStack(Blocks.CLAY)), "generify block " + row.block());
                accept(
                        MachineRecipeMaps.generify(stack(row.ball(), 1), new ItemStack(Items.CLAY_BALL)),
                        "generify ball " + row.ball());
            }
        }
        // MultiItemFood: the ordinary ball and five colored balls roll into plates,
        // while one dust unit compresses back into the matching ball.
        clayBalls().forEach((material, ball) -> clayBallForms(material, ball, material.getName()));
        return registeredCount;
    }

    static java.util.Map<GTMaterial, ItemStack> clayBalls() {
        var result = new java.util.LinkedHashMap<GTMaterial, ItemStack>();
        result.put(Materials.Clay, new ItemStack(Items.CLAY_BALL));
        for (Variant row : PACKABLE) if (row.material() != null) result.put(row.material(), stack(row.ball(), 1));
        return result;
    }

    private static void clayBallForms(GTMaterial material, ItemStack ball, String name) {
        ItemStack plate = GTItems.getStack(MaterialPrefix.plate, material);
        ItemStack dust = GTItems.getStack(MaterialPrefix.dust, material);
        if (plate.isEmpty() || dust.isEmpty())
            throw new IllegalStateException("Missing GT6 clay plate/dust form for " + name);
        add(
                MachineRecipeMaps.RollingMill.register(
                        RecipeBuilder.of(16, 32).optimize(true).itemInputs(ball).itemOutputs(plate)),
                "roll " + name);
        add(
                MachineRecipeMaps.Compressor.register(
                        RecipeBuilder.of(16, 16).optimize(true).itemInputs(dust).itemOutputs(ball)),
                "compress clay dust " + name);
        // Unfired clay is moldable but not SMITHABLE; metal-only shaping rules exclude it.
        var curved = GTItems.getStack(MaterialPrefix.plateCurved, material);
        if (!curved.isEmpty()) {
            add(
                    MachineRecipeMaps.RollBender.register(RecipeBuilder.of(16, 16)
                            .optimize(true)
                            .itemInputs(plate)
                            .itemOutputs(curved)),
                    "bend clay " + name);
            add(
                    MachineRecipeMaps.RollingMill.register(RecipeBuilder.of(16, 16)
                            .optimize(true)
                            .itemInputs(curved)
                            .itemOutputs(plate)),
                    "flatten clay " + name);
        }
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
        Item value = Mc.item(Mc.gregtechId(id));
        if (value == null || value == Items.AIR) throw new IllegalStateException("Missing GT6 diggable item " + id);
        return value;
    }

    private static ItemStack stack(String id, int count) {
        return new ItemStack(item(id), count);
    }
}
