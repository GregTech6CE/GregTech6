package com.gregtech.gregtech.gametest;

import com.google.gson.JsonObject;
import com.gregtech.gregtech.api.recipe.Recipe;
import com.gregtech.gregtech.content.bumble.BumbleBeeGenes;
import com.gregtech.gregtech.content.bumble.BumbleBeeType;
import com.gregtech.gregtech.content.bumble.GTBumbleSpecies;
import com.gregtech.gregtech.content.recipe.GTBumbleBeeRecipes;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.registry.GTFluids;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * GT6's bumblebee <em>scanning</em> chain: the 320 scanned bee items
 * ({@code MultiItemBumbles.make:594-597}, the meta offsets +5/+6/+7/+9) and the Bumblelyzer's
 * input-computed recipe ({@code RecipeMapBumblelyzer.findRecipe:51-74}).
 *
 * <p>The scanned items are registered as ordinary multi-items
 * ({@code registry/GTMultiItemsGen.java}), so their translation keys are the same
 * {@code item.gregtech.<id>} keys every other bee uses - and {@code MaterialCompatibilityTests}
 * demands one in both languages for every registered item.</p>
 */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class BumbleBeeScanningTests {

    /** GT6's four living types: the metas below 5 that {@code bumbleScan} turns into a scanned one. */
    private static final BumbleBeeType[] LIVING = {
            BumbleBeeType.DRONE, BumbleBeeType.PRINCESS, BumbleBeeType.QUEEN, BumbleBeeType.DEAD};

    /** The species the recipe tests use (GT6's rocky colony, so the ids are not the first row). */
    private static final GTBumbleSpecies.Species SPECIES = species(500);

    private static GTBumbleSpecies.Species species(int id) {
        GTBumbleSpecies.Species found = GTBumbleSpecies.byId(id);
        if (found == null) throw new IllegalStateException("species " + id + " is not registered");
        return found;
    }

    private static JsonObject lang(String language) throws Exception {
        String path = "assets/gregtech/lang/" + language + ".json";
        try (InputStream stream = BumbleBeeScanningTests.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) throw new IllegalStateException("missing resource " + path);
            return GsonHelper.parse(new InputStreamReader(stream, StandardCharsets.UTF_8));
        }
    }

    /** The scanned bee the recipe handed out, or an empty stack. */
    private static ItemStack output(Recipe recipe) {
        return recipe == null ? ItemStack.EMPTY : recipe.getOutput(0);
    }

    /** A genome that survives the scan, so the NBT hand-over is observable. */
    private static CompoundTag genome() {
        CompoundTag genes = new CompoundTag();
        BumbleBeeGenes.setWorkForce(genes, 2500);
        BumbleBeeGenes.setOffspring(genes, 7);
        return genes;
    }

    /** GT6's eighty species, four scanned types each: 320 items, each with a name in both languages. */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void everySpeciesHasItsFourScannedBees(GameTestHelper helper) throws Exception {
        helper.assertTrue(GTBumbleSpecies.SPECIES.size() == 80,
                "GT6 registers 80 bumblebee species, got " + GTBumbleSpecies.SPECIES.size());
        JsonObject english = lang("en_us");
        JsonObject chinese = lang("zh_cn");
        List<String> problems = new ArrayList<>();
        int registered = 0;
        int translated = 0;
        for (GTBumbleSpecies.Species species : GTBumbleSpecies.SPECIES) {
            for (BumbleBeeType living : LIVING) {
                BumbleBeeType scanned = living.scannedVariant();
                String id = BumbleBeeType.itemId(species, scanned);
                ItemStack stack = BumbleBeeType.stack(species, scanned, null, 1);
                if (stack.isEmpty() || !BumbleBeeType.exists(species, scanned)) {
                    problems.add("missing item " + id);
                    continue;
                }
                registered++;
                // The type comes back out of the id, and the species out of the prefix.
                if (BumbleBeeType.of(stack) != scanned) {
                    problems.add(id + " reads back as " + BumbleBeeType.of(stack));
                }
                if (BumbleBeeType.speciesOf(stack) != species) {
                    problems.add(id + " does not read back as " + species.name());
                }
                // The living sibling is untouched next to it.
                ItemStack alive = BumbleBeeType.stack(species, living, null, 1);
                if (alive.isEmpty() || BumbleBeeType.of(alive) != living) {
                    problems.add("living " + BumbleBeeType.itemId(species, living) + " is gone or misread");
                }
                // Every registered item needs both names (the port's localization guard).
                String key = "item.gregtech." + id;
                if (!english.has(key) || !chinese.has(key)) {
                    problems.add("no translation for " + key);
                } else {
                    translated++;
                }
            }
        }
        helper.assertTrue(problems.isEmpty(), "scanned bee registration (" + problems.size() + "): "
                + problems.subList(0, Math.min(6, problems.size())));
        helper.assertTrue(registered == 320, "GT6 has 320 scanned bees, registered: " + registered);
        helper.assertTrue(translated == 320, "both languages name all 320 scanned bees: " + translated);
        // GT6's own naming: the living name plus the scanned marker, the dead one inside its parens.
        helper.assertTrue("Wild Bumblebee Drone (Scanned)"
                        .equals(english.get("item.gregtech.wild_bumblebee_scanned_drone").getAsString()),
                "the scanned drone is GT6's \"<name> Drone (Scanned)\"");
        helper.assertTrue("Wild Bumblebee (Dead, Scanned)"
                        .equals(english.get("item.gregtech.wild_bumblebee_scanned_dead").getAsString()),
                "the scanned dead bee keeps one pair of parentheses");
        helper.succeed();
    }

    /** GT6's type metas: all eight exist, and the four living ones scan to four distinct variants. */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void everyTypeIsRegisteredAndScansToItsOwnVariant(GameTestHelper helper) {
        for (BumbleBeeType type : BumbleBeeType.values()) {
            helper.assertTrue(type.registered(), type.suffix() + " is registered now that its items exist");
        }
        Set<BumbleBeeType> scanned = new HashSet<>();
        for (BumbleBeeType living : LIVING) {
            helper.assertTrue(!living.scanned(), living.suffix() + " is a living type");
            BumbleBeeType variant = living.scannedVariant();
            helper.assertTrue(variant.scanned(), living.suffix() + " scans to a scanned type");
            helper.assertTrue(scanned.add(variant), living.suffix() + " scans to its own variant");
            helper.assertTrue(variant.aliveVariant() == living,
                    variant.suffix() + " reads back as " + living.suffix());
        }
        helper.assertTrue(scanned.size() == 4, "four distinct scanned variants, got " + scanned.size());
        // GT6's metas (MultiItemBumbles:564-577) and the death rule that keeps the scan state.
        helper.assertTrue(BumbleBeeType.DRONE.meta() == 0 && BumbleBeeType.PRINCESS.meta() == 1
                        && BumbleBeeType.QUEEN.meta() == 2 && BumbleBeeType.DEAD.meta() == 4,
                "the living metas are GT6's 0/1/2/4");
        helper.assertTrue(BumbleBeeType.SCANNED_DRONE.meta() == 5 && BumbleBeeType.SCANNED_PRINCESS.meta() == 6
                        && BumbleBeeType.SCANNED_QUEEN.meta() == 7 && BumbleBeeType.SCANNED_DEAD.meta() == 9,
                "the scanned metas are GT6's 5/6/7/9");
        helper.assertTrue(BumbleBeeType.PRINCESS.deadVariant() == BumbleBeeType.DEAD
                        && BumbleBeeType.SCANNED_PRINCESS.deadVariant() == BumbleBeeType.SCANNED_DEAD,
                "dying keeps the scan state");
        // The scanned items carry the species and type in their ids only, like the living ones.
        ItemStack scannedQueen = BumbleBeeType.stack(SPECIES, BumbleBeeType.SCANNED_QUEEN, null, 1);
        helper.assertTrue(!scannedQueen.isEmpty()
                        && "stoned_bumblebee_scanned_queen".equals(
                                ForgeRegistries.ITEMS.getKey(scannedQueen.getItem()).getPath()),
                "the scanned queen's item id is <species>_scanned_queen");
        helper.succeed();
    }

    /** GT6's Bumblelyzer row: a living bee + one tiny paper plate + 10 mB honey -> the scanned bee. */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void bumblelyzerScansALivingPrincess(GameTestHelper helper) {
        // The map is touched first on purpose: its initializer is what installs the provider.
        helper.assertTrue(MachineRecipeMaps.Bumblelyzer.hasDynamicRecipes(),
                "the Bumblelyzer computes its rows from the input bee");
        helper.assertTrue(GTBumbleBeeRecipes.isRegistered(), "the bumblebee provider is installed");

        ItemStack bee = BumbleBeeType.stack(SPECIES, BumbleBeeType.PRINCESS, genome(), 1);
        helper.assertTrue(!bee.isEmpty(), "the test princess exists");
        ItemStack plate = GTBumbleBeeRecipes.paperPlate();
        helper.assertTrue(!plate.isEmpty() && plate.getCount() == 1,
                "GT6's tiny paper plate exists: " + plate);
        FluidStack honey = GTFluids.stack("Honey", 1000);
        helper.assertTrue(honey != null && !honey.isEmpty(), "the port registers GT6's honey fluid");

        Recipe recipe = MachineRecipeMaps.Bumblelyzer.findRecipe(
                List.of(bee, plate), List.of(honey), false, 2, 2);
        helper.assertTrue(recipe != null, "a living princess with a plate and honey is scanned");
        ItemStack scanned = output(recipe);
        helper.assertTrue(BumbleBeeType.of(scanned) == BumbleBeeType.SCANNED_PRINCESS,
                "the output is the scanned princess, got " + scanned);
        helper.assertTrue(BumbleBeeType.speciesOf(scanned) == SPECIES,
                "and it is still a " + SPECIES.name());
        helper.assertTrue(scanned.getCount() == 1, "one bee per scan, got " + scanned.getCount());
        // GT6 keeps the whole stack: bumbleScan is ST.copy plus the meta offset, genome included.
        CompoundTag genes = BumbleBeeGenes.peek(scanned);
        helper.assertTrue(genes != null && BumbleBeeGenes.workForce(genes) == 2500
                        && BumbleBeeGenes.offspring(genes) == 7,
                "the scanned bee carries the genome of the one that went in");

        // GT6 :60 - [bee, plate] and 10 mB of the tank's honey, 64 ticks at 16 EU/t.
        helper.assertTrue(recipe.mInputs.length == 2, "the row takes the bee and the plate");
        int bees = 0, plates = 0;
        for (ItemStack input : recipe.mInputs) {
            if (input.is(plate.getItem())) plates += input.getCount();
            else if (input.is(bee.getItem())) bees += input.getCount();
        }
        helper.assertTrue(bees == 1 && plates == 1,
                "one bee and one tiny paper plate: " + bees + " bee(s), " + plates + " plate(s)");
        helper.assertTrue(recipe.mFluidInputs.length == 1
                        && recipe.mFluidInputs[0].isFluidEqual(honey)
                        && recipe.mFluidInputs[0].getAmount() == GTBumbleBeeRecipes.HONEY_MB,
                "10 mB of honey: " + java.util.Arrays.toString(recipe.mFluidInputs));
        helper.assertTrue(recipe.mEUt == 16 && recipe.mDuration == 64,
                "GT6's 64 ticks at 16 EU/t, got " + recipe.mDuration + " ticks at " + recipe.mEUt + " EU/t");
        helper.assertTrue(recipe.mOutputs.length == 1, "and exactly one output");

        // GT6 accepts honeydew as well (:55).
        FluidStack honeydew = GTFluids.stack("Honeydew", 1000);
        if (honeydew != null && !honeydew.isEmpty()) {
            helper.assertTrue(MachineRecipeMaps.Bumblelyzer.findRecipe(
                            List.of(bee.copy(), plate.copy()), List.of(honeydew), false, 2, 2) != null,
                    "honeydew feeds the Bumblelyzer too");
        }

        // GT6 truncates the stack with ST.amount(1, aInput) before reading its size, so a stack of
        // five bees is scanned one bee per row and still needs exactly one plate.
        Recipe batch = MachineRecipeMaps.Bumblelyzer.findRecipe(
                List.of(bee.copyWithCount(5), new ItemStack(plate.getItem(), 4)), List.of(honey), false, 2, 2);
        helper.assertTrue(batch != null, "a stack of bees is scanned too");
        int batchBees = 0, batchPlates = 0;
        for (ItemStack input : batch.mInputs) {
            if (input.is(plate.getItem())) batchPlates += input.getCount();
            else if (input.is(bee.getItem())) batchBees += input.getCount();
        }
        helper.assertTrue(batchBees == 1 && batchPlates == 1,
                "GT6's row is one bee plus one plate, whatever the stack sizes are: "
                        + batchBees + " bee(s), " + batchPlates + " plate(s)");
        helper.succeed();
    }

    /** Without honey, without a plate, or with something that is not a bee: no row at all. */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void bumblelyzerNeedsHoneyAPlateAndABee(GameTestHelper helper) {
        ItemStack bee = BumbleBeeType.stack(SPECIES, BumbleBeeType.PRINCESS, genome(), 1);
        ItemStack plate = GTBumbleBeeRecipes.paperPlate();
        FluidStack honey = GTFluids.stack("Honey", 1000);
        helper.assertTrue(!bee.isEmpty() && !plate.isEmpty() && honey != null, "the fixtures exist");

        helper.assertTrue(MachineRecipeMaps.Bumblelyzer.findRecipe(
                        List.of(bee, plate), List.of(), false, 2, 2) == null,
                "without honey the Bumblelyzer does nothing");
        FluidStack sip = honey.copy();
        sip.setAmount(GTBumbleBeeRecipes.HONEY_MB - 1);
        helper.assertTrue(MachineRecipeMaps.Bumblelyzer.findRecipe(
                        List.of(bee, plate), List.of(sip), false, 2, 2) == null,
                "nine mB of honey is not enough");
        helper.assertTrue(MachineRecipeMaps.Bumblelyzer.findRecipe(
                        List.of(bee), List.of(honey), false, 2, 2) == null,
                "without the tiny paper plate there is nothing to scan onto");
        helper.assertTrue(MachineRecipeMaps.Bumblelyzer.findRecipe(
                        List.of(new ItemStack(Items.DIRT), plate), List.of(honey), false, 2, 2) == null,
                "a non-bee item is never scanned");
        helper.assertTrue(MachineRecipeMaps.Bumblelyzer.findRecipe(
                        List.of(plate), List.of(honey), false, 1, 2) == null,
                "neither is the paper plate on its own");
        FluidStack water = new FluidStack(Fluids.WATER, 1000);
        helper.assertTrue(MachineRecipeMaps.Bumblelyzer.findRecipe(
                        List.of(bee, plate), List.of(water), false, 2, 2) == null,
                "water is not honey");

        // GT6 :61 - an already-scanned bee is passed through, with no paper and no honey spent.
        ItemStack scanned = BumbleBeeType.stack(SPECIES, BumbleBeeType.SCANNED_PRINCESS, genome(), 1);
        helper.assertTrue(!scanned.isEmpty(), "the scanned princess exists");
        Recipe pass = MachineRecipeMaps.Bumblelyzer.findRecipe(
                List.of(scanned), List.of(honey), false, 2, 2);
        helper.assertTrue(pass != null, "an already scanned bee is passed through");
        helper.assertTrue(pass.mInputs.length == 1 && pass.mInputs[0].is(scanned.getItem()),
                "the pass-through takes the bee alone");
        helper.assertTrue(output(pass).is(scanned.getItem())
                        && BumbleBeeType.of(output(pass)) == BumbleBeeType.SCANNED_PRINCESS,
                "and hands the same scanned bee back: " + output(pass));
        helper.assertTrue(pass.mFluidInputs.length == 0 && pass.mEUt == 16 && pass.mDuration == 1,
                "GT6's pass-through is instant and consumes no honey: " + pass.mDuration + " ticks at "
                        + pass.mEUt + " EU/t");
        helper.succeed();
    }

    /**
     * GT6's display rows ({@code MultiItemBumbles:584-585}): one honey and one honeydew row per bee, so
     * the Bumblelyzer's recipe page is never empty - registered as <em>fake</em> recipes, which is how
     * the original marks them and what keeps the machine from running them
     * ({@code RecipeMap:591} and {@code :632} skip {@code mFakeRecipe} rows).
     */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void bumblelyzerDisplayRowsAreFakeRecipes(GameTestHelper helper) {
        helper.assertTrue(GTBumbleBeeRecipes.isRegistered(), "the bumblebee provider is installed");
        int rows = GTBumbleBeeRecipes.displayRowCount();
        helper.assertTrue(rows > 0, "the Bumblelyzer's recipe page has GT6's display rows");
        // GT6: 80 species x the three bee types of :582 x (the honey fluids that exist + honeydew).
        int expected = GTBumbleSpecies.SPECIES.size() * GTBumbleBeeRecipes.DISPLAY_TYPES.length
                * GTBumbleBeeRecipes.displayFluidNames().size();
        helper.assertTrue(rows == expected, "display rows: " + rows + ", expected " + expected);
        helper.assertTrue(GTBumbleBeeRecipes.displayFluidNames().contains("Honey")
                        && GTBumbleBeeRecipes.displayFluidNames().contains("Honeydew"),
                "GT6's honey and honeydew rows: " + GTBumbleBeeRecipes.displayFluidNames());

        ItemStack plate = GTBumbleBeeRecipes.paperPlate();
        int checked = 0;
        for (Recipe row : GTBumbleBeeRecipes.displayRows()) {
            helper.assertTrue(row.mFakeRecipe, "a display row is a fake recipe: " + row);
            helper.assertTrue(row.mInputs.length == 2 && row.mOutputs.length == 1,
                    "GT6's [bee, plate] -> scanned bee layout: " + row.mInputs.length + " in, "
                            + row.mOutputs.length + " out");
            boolean bee = false;
            for (ItemStack input : row.mInputs) {
                if (input.is(plate.getItem())) continue;
                bee = BumbleBeeType.of(input) != null && !BumbleBeeType.of(input).scanned()
                        && BumbleBeeType.speciesOf(input) != null;
            }
            helper.assertTrue(bee, "the row scans a living bee");
            helper.assertTrue(BumbleBeeType.of(row.mOutputs[0]) != null
                            && BumbleBeeType.of(row.mOutputs[0]).scanned(),
                    "the row's output is the scanned variant: " + row.mOutputs[0]);
            helper.assertTrue(row.mFluidInputs.length == 1
                            && row.mFluidInputs[0].getAmount() == GTBumbleBeeRecipes.HONEY_MB,
                    "the row costs GT6's 10 mB: " + java.util.Arrays.toString(row.mFluidInputs));
            helper.assertTrue(row.mDuration == GTBumbleBeeRecipes.SCAN_TICKS
                            && row.mEUt == GTBumbleBeeRecipes.SCAN_EU,
                    "the row keeps GT6's 64 ticks at 16 EU/t");
            checked++;
        }
        helper.assertTrue(checked == rows, "display rows checked: " + checked);

        // The machine itself never sees them: the same grid resolves through the dynamic provider,
        // whose row is a real one and a different instance.
        ItemStack bee = BumbleBeeType.stack(SPECIES, BumbleBeeType.PRINCESS, genome(), 1);
        FluidStack honey = GTFluids.stack("Honey", 1000);
        Recipe machine = MachineRecipeMaps.Bumblelyzer.findRecipe(
                List.of(bee, plate), List.of(honey), false, 2, 2);
        helper.assertTrue(machine != null && !machine.mFakeRecipe,
                "the Bumblelyzer computes its own row, not a display row");
        helper.assertTrue(GTBumbleBeeRecipes.displayRows().stream().noneMatch(row -> row == machine),
                "the computed row is not one of the display rows");
        // A display row's own grid still resolves to the computed row, i.e. the machine ignores the
        // fake row that describes the same scan.
        Recipe display = GTBumbleBeeRecipes.displayRows().stream()
                .filter(row -> row.mInputs[0].is(bee.getItem()))
                .filter(row -> row.mFluidInputs[0].isFluidEqual(honey))
                .findFirst().orElse(null);
        helper.assertTrue(display != null, "the princess has a honey display row");
        Recipe resolved = MachineRecipeMaps.Bumblelyzer.findRecipe(
                List.of(display.mInputs[0].copy(), plate.copy()),
                List.of(display.mFluidInputs[0].copy()), false, 2, 2);
        helper.assertTrue(resolved != null && resolved != display && !resolved.mFakeRecipe,
                "the machine resolves that grid through the provider, not through the display row");
        helper.succeed();
    }
}
