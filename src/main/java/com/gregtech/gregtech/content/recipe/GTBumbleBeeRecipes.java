package com.gregtech.gregtech.content.recipe;

import com.gregtech.gregtech.api.recipe.Recipe;
import com.gregtech.gregtech.content.bumble.BumbleBeeType;
import com.gregtech.gregtech.content.bumble.GTBumbleSpecies;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.data.RegisteredFluids;
import com.gregtech.gregtech.registry.GTFluids;
import com.gregtech.gregtech.registry.GTItems;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * GT6's <b>Bumblelyzer</b> - the port of {@code RecipeMapBumblelyzer.findRecipe}
 * ({@code gregapi/recipes/maps/RecipeMapBumblelyzer.java:51-74}), installed on
 * {@link MachineRecipeMaps#Bumblelyzer} through {@link
 * com.gregtech.gregtech.api.recipe.RecipeMap#dynamicRecipes}.
 *
 * <p>The original is a {@code RecipeMap} subclass whose {@code findRecipe} builds a row from the
 * <em>identity</em> of the bee in the input slots, which no static table can express: a living bee of
 * any of GT6's 320 (species, type) combinations turns into its own scanned variant, keeping its
 * genome. GT6's whole branch, line by line:</p>
 *
 * <ul>
 *   <li>{@code :55} - only honey counts ({@code FluidsGT.HONEY}, i.e. the three fluids declared with
 *       the {@code HONEY} flag in {@code FL.java:139-141}, plus {@code FL.Honeydew}).</li>
 *   <li>{@code :56-57} - every valid input stack, truncated to a single item
 *       ({@code ST.amount(1, aInput)}).</li>
 *   <li>{@code :58-60} - a bee whose {@code bumbleType < 5} (a <em>living</em>, not yet scanned bee;
 *       GT6's {@code bumbleScan} adds 5 to the type) plus one tiny paper plate becomes the scanned
 *       variant of that same bee, at the price of 10 mB of the tank's honey.</li>
 *   <li>{@code :61} - the "no paper" fallback: a bee that is <em>already</em> scanned is passed
 *       through unchanged (input = output, 1 tick), the row GT6 also publishes as
 *       "Was already scanned, auto-skipping" ({@code MultiItemBumbles:586-587}).</li>
 *   <li>{@code :63-69} - the Forestry bee branch ({@code IL.FR_Bee_Drone/Princess/Queen} analysed
 *       through {@code AlleleManager}). <b>Skipped:</b> the port has no Forestry bees, so no stack can
 *       ever satisfy it (see the note on {@link #find}).</li>
 * </ul>
 *
 * <p>Faithfulness notes, since GT6's own code is easy to misread here:</p>
 * <ul>
 *   <li>The tiny paper plate is an <b>input</b>, not an output: {@code :60} passes
 *       {@code ST.array(aInput, OP.plateTiny.mat(MT.Paper, aInput.stackSize))} as the recipe's
 *       <em>inputs</em> and the scanned bee as its only output. GT6's recipe-viewer rows show the
 *       same layout ({@code MultiItemBumbles:584-585}).</li>
 *   <li>That plate count is always <b>1</b>, whatever the input stack size: {@code :57} has already
 *       replaced the stack with {@code ST.amount(1, aInput)} (= {@code ST.copy_} + {@code ST.size_}),
 *       so {@code aInput.stackSize} read one line later is 1 by construction. GT6 therefore has no
 *       "paper only for a single bee" rule; a stack of five bees also consumes one plate and produces
 *       one scanned bee.</li>
 *   <li>Duration and power are {@code 64, 16} in {@code :60}, and GT6's {@code Recipe} constructor
 *       takes them in that order - {@code Recipe.java:877} is
 *       {@code (..., aInputs, aOutputs, aSpecialItems, aChances, aFluidInputs, aFluidOutputs,
 *       aDuration, aEUt, aSpecialValue)} - so the row is <b>64 ticks at 16 EU/t</b>, exactly like the
 *       port stores the molecular scanner's row from the analogous {@code :57} of
 *       {@code RecipeMapScannerMolecular}. The pass-through row of {@code :61} is {@code 1, 16}.</li>
 *   <li>Rows are built per lookup instead of being cached (unlike {@link GTMaterialDataRecipes}):
 *       the row carries the input bee's genome NBT, so a row cached for one genome would not match a
 *       bee with another. GT6 builds a fresh {@code Recipe} per {@code findRecipe} call for the same
 *       reason.</li>
 *   <li>GT6's {@code GAPI_POST.mFinishedServerStarted <= 0} early-out ({@code :53}) has no port
 *       equivalent; these providers only ever run from a machine's recipe lookup, which cannot happen
 *       before the server started.</li>
 *   <li>GT6 additionally registers two <em>fake</em> recipe-viewer rows per bee
 *       ({@code MultiItemBumbles:584-585}, honey and honeydew); the port registers them too, marked as
 *       fake so the machine can never run one ({@link #registerDisplayRows()}).</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = com.gregtech.gregtech.GregTech.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GTBumbleBeeRecipes {

    /** GT6 {@code :60} - the honey a single scan consumes ({@code FL.amount(aFluids[0], 10)}). */
    public static final int HONEY_MB = 10;
    /** GT6 {@code :60} - the scan's duration, the 9th argument of its {@code Recipe} constructor. */
    public static final long SCAN_TICKS = 64;
    /** GT6 {@code :60} - the scan's power. */
    public static final long SCAN_EU = 16;
    /** GT6 {@code :61} - the already-scanned pass-through is instant. */
    public static final long PASS_TICKS = 1;
    /** GT6 {@code :61} - and costs the machine's minimum. */
    public static final long PASS_EU = 16;

    /**
     * GT6's three display-row bee types ({@code MultiItemBumbles:582}, {@code for (int i : {0, 1, 4})}:
     * drone, princess and the dead bee - a living bee's metas below 5).
     */
    public static final BumbleBeeType[] DISPLAY_TYPES = {
            BumbleBeeType.DRONE, BumbleBeeType.PRINCESS, BumbleBeeType.DEAD};

    /**
     * GT6's display-row fluids: every fluid declared with the {@code HONEY} flag
     * ({@code FluidsGT.HONEY}, the three {@code FL.java:139-141} entries) plus honeydew
     * ({@code MultiItemBumbles:584-585}).
     */
    private static final String[] DISPLAY_FLUIDS = {"Honey", "HoneyGrC", "HoneyBoP", "Honeydew"};

    private static boolean registered;
    private static final List<Recipe> DISPLAY_ROWS = new ArrayList<>();
    private static final List<String> DISPLAY_FLUID_NAMES = new ArrayList<>();

    private GTBumbleBeeRecipes() {}

    /**
     * Installs the provider on the Bumblelyzer map; idempotent. Returns 1 once it is in place.
     *
     * <p>Only the provider is installed here: {@code MachineRecipeMaps}'s own initializer calls this, and
     * that initializer runs inside the mod constructor (phase B,
     * {@code Loader_MultiTileEntities} -&gt; {@code GTBasicMachines} -&gt;
     * {@code BasicMachineDefinitions} -&gt; {@code MachineRecipeMaps.byMachineName}), i.e. before Forge has
     * filled the item and fluid registries. The display rows need real bee items and a honey fluid, so
     * they are registered in common setup instead ({@link #onCommonSetup}).</p>
     */
    public static synchronized int register() {
        if (registered) return 0;
        registered = true;
        MachineRecipeMaps.Bumblelyzer.dynamicRecipes(GTBumbleBeeRecipes::find);
        return 1;
    }

    /** Registers GT6's display rows once the registries are filled; see {@link #register()}. */
    @SubscribeEvent
    public static void onCommonSetup(net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event) {
        event.enqueueWork(GTBumbleBeeRecipes::registerDisplayRows);
    }

    /** Whether the provider is installed. */
    public static boolean isRegistered() { return registered; }

    /**
     * GT6's two recipe-viewer rows per bee ({@code MultiItemBumbles:584-585}, inside the per-species
     * {@code make}): {@code [bee, tiny paper plate] + 10 mB of honey -> the scanned bee}, once per honey
     * fluid, and the same row with honeydew instead. GT6 adds them with {@code addFakeRecipe}, which is
     * why the Bumblelyzer's recipe page is never empty while the machine itself ignores them
     * ({@code RecipeMap:591}, {@code :632} skip {@code mFakeRecipe} rows).
     *
     * <p>The rows are built from the port's own 80 species and 320 bee items, so the count is
     * {@code 80 x 3 x (honey fluids + honeydew)}; a fluid the port does not register is skipped, exactly
     * like GT6's {@code if (FL.exists(tFluid))}.</p>
     */
    public static synchronized int registerDisplayRows() {
        if (!DISPLAY_ROWS.isEmpty()) return DISPLAY_ROWS.size();
        ItemStack plate = paperPlate();
        if (plate.isEmpty()) return 0;
        for (String name : DISPLAY_FLUIDS) {
            FluidStack fluid = GTFluids.stack(name, HONEY_MB);
            if (fluid == null || fluid.isEmpty()) continue;
            fluid.setAmount(HONEY_MB);
            DISPLAY_FLUID_NAMES.add(name);
            for (GTBumbleSpecies.Species species : GTBumbleSpecies.SPECIES) {
                for (BumbleBeeType type : DISPLAY_TYPES) {
                    ItemStack bee = BumbleBeeType.stack(species, type, null, 1);
                    ItemStack scanned = BumbleBeeType.stack(species, type.scannedVariant(), null, 1);
                    if (bee.isEmpty() || scanned.isEmpty()) continue;
                    // GT6 passes F as the collision flag; the row is display only.
                    Recipe row = MachineRecipeMaps.Bumblelyzer.addFakeRecipe(false,
                            new ItemStack[]{bee, plate.copy()}, new ItemStack[]{scanned}, null,
                            new FluidStack[]{fluid.copy()}, null, SCAN_TICKS, SCAN_EU, 0);
                    if (row != null) DISPLAY_ROWS.add(row);
                }
            }
        }
        return DISPLAY_ROWS.size();
    }

    /** The display rows GT6 publishes per bee, for the recipe viewer and the tests. */
    public static List<Recipe> displayRows() { return List.copyOf(DISPLAY_ROWS); }

    /** How many display rows are registered. */
    public static int displayRowCount() { return DISPLAY_ROWS.size(); }

    /** The fluids the display rows use, in GT6's order (the honey fluids that exist, then honeydew). */
    public static List<String> displayFluidNames() { return List.copyOf(DISPLAY_FLUID_NAMES); }

    /** GT6's {@code OP.plateTiny.mat(MT.Paper, 1)} - the plate every scan consumes. */
    public static ItemStack paperPlate() {
        return GTItems.getStack(MaterialPrefix.plateTiny, Materials.Paper, 1);
    }

    /**
     * GT6 {@code :51-74}: a living bee in the input slots plus honey in the tank is scanned into its
     * own scanned variant; an already-scanned bee is passed straight through.
     *
     * <p>Returns null whenever the inputs cannot satisfy the row GT6 would build - no honey (or less
     * than {@link #HONEY_MB} of it), no bee, no paper plate for a living bee, or a bee type the port
     * has no item for. GT6 leaves that verdict to the machine's own input matching; the port's
     * {@code RecipeMap#findRecipe} instead treats an unsatisfiable dynamic row as "no recipe", so the
     * provider has to be as strict as the machine would be.</p>
     */
    @Nullable
    public static Recipe find(List<ItemStack> items, List<FluidStack> fluids) {
        // GT6 only looks at the first tank (`aFluids[0]`); the Bumblelyzer has exactly one.
        FluidStack tank = fluids.isEmpty() ? null : fluids.get(0);
        if (!isHoney(tank) || tank.getAmount() < HONEY_MB) return null;
        for (ItemStack stack : items) {
            if (stack == null || stack.isEmpty()) continue;
            BumbleBeeType type = BumbleBeeType.of(stack);
            if (type == null) continue;
            GTBumbleSpecies.Species species = BumbleBeeType.speciesOf(stack);
            // The suffix test alone also matches non-bee items ending in "_dead" and the like; a
            // species the port knows is what makes the stack a GT6 bumblebee.
            if (species == null) continue;
            // GT6's `bumbleType(aInput) < 5`: the four living types scan, the four scanned pass
            // through (`BumbleBeeType.meta()` is exactly GT6's type meta).
            if (type.scanned()) return passThrough(stack);
            // The scan row needs its paper plate; without one the machine could not run it.
            if (plateIn(items).isEmpty()) return null;
            return scan(stack, species, type, tank);
        }
        return null;
    }

    /**
     * GT6 {@code :60}: {@code [bee, tiny paper plate] + 10 mB honey -> scanned bee}, 64 ticks at
     * 16 EU/t. The output keeps the input's whole tag, genome included ({@code bumbleScan} is
     * {@code ST.copy} plus the meta offset).
     */
    @Nullable
    private static Recipe scan(ItemStack bee, GTBumbleSpecies.Species species, BumbleBeeType type,
                               FluidStack honey) {
        ItemStack plate = paperPlate();
        if (plate.isEmpty()) return null;
        ItemStack output = BumbleBeeType.stack(species, type.scannedVariant(), null, 1);
        if (output.isEmpty()) return null;
        if (bee.hasTag()) output.setTag(bee.getTag().copy());
        FluidStack consumed = honey.copy();
        consumed.setAmount(HONEY_MB);
        return new Recipe(
                new ItemStack[]{bee.copyWithCount(1), plate},
                new ItemStack[]{output},
                null, null,
                new FluidStack[]{consumed},
                null,
                SCAN_TICKS, SCAN_EU, 0);
    }

    /**
     * GT6 {@code :61}: an already-scanned bee leaves the machine untouched - the same stack on both
     * sides, no paper and no honey, 1 tick at 16 EU/t. GT6 shares one stack instance between the two
     * arrays; the port hands out two copies so the machine can never shrink the output along with the
     * input.
     */
    private static Recipe passThrough(ItemStack bee) {
        return new Recipe(
                new ItemStack[]{bee.copyWithCount(1)},
                new ItemStack[]{bee.copyWithCount(1)},
                null, null, null, null,
                PASS_TICKS, PASS_EU, 0);
    }

    /** The paper plate of the input list, or an empty stack when the machine has none. */
    private static ItemStack plateIn(List<ItemStack> items) {
        ItemStack plate = paperPlate();
        if (plate.isEmpty()) return ItemStack.EMPTY;
        for (ItemStack stack : items) {
            if (stack != null && !stack.isEmpty() && stack.is(plate.getItem())) return stack;
        }
        return ItemStack.EMPTY;
    }

    /**
     * GT6 {@code :55}: a fluid declared with the {@code HONEY} flag ({@code FL.java:139-141}:
     * {@code for.honey}, {@code grc.honey} and BoP's {@code honey}) or honeydew.
     */
    private static boolean isHoney(@Nullable FluidStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        // entryForFluid, not the FluidType key: the three world waters report vanilla water's
        // FluidType (see GTWorldWaterFluid), so the type key would lose their GT6 entry.
        RegisteredFluids.FluidEntry entry = GTFluids.entryForFluid(stack.getFluid());
        if (entry != null && entry.hasFlag(RegisteredFluids.FluidFlags.HONEY)) return true;
        FluidStack honeydew = GTFluids.stack("Honeydew", 1);
        return honeydew != null && !honeydew.isEmpty() && honeydew.getFluid() == stack.getFluid();
    }
}
