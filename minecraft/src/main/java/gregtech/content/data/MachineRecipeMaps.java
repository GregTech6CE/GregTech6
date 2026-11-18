package gregtech.content.data;

import gregtech.api.material.MaterialProperty;
import gregtech.content.machine.BasicMachineCatalog;
import gregtech.engine.recipe.RecipeBuilder;
import gregtech.engine.recipe.RecipeMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;

//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}

/** Built-in machine recipe maps in registration order. */
public final class MachineRecipeMaps {
    private MachineRecipeMaps() {}

    // ── RecipeMap instances ──────────────────────────────────────────────

    public static final RecipeMap
    Furnace                   = new RecipeMap(MachineRecipeMapDefinitions.Furnace)
    , Microwave                = new RecipeMap(MachineRecipeMapDefinitions.Microwave)
    , Roasting                 = new RecipeMap(MachineRecipeMapDefinitions.Roasting)
    , Distillery               = new RecipeMap(MachineRecipeMapDefinitions.Distillery)
    , Extruder                 = new RecipeMap(MachineRecipeMapDefinitions.Extruder)
    , Smelter                  = new RecipeMap(MachineRecipeMapDefinitions.Smelter)
    , CrystallisationCrucible  = new RecipeMap(MachineRecipeMapDefinitions.CrystallisationCrucible)
    , Drying                   = new RecipeMap(MachineRecipeMapDefinitions.Drying)
    , Laminator                = new RecipeMap(MachineRecipeMapDefinitions.Laminator)
    , CatalyticCracking        = new RecipeMap(MachineRecipeMapDefinitions.CatalyticCracking)
    , SteamCracking            = new RecipeMap(MachineRecipeMapDefinitions.SteamCracking)
    , DistillationTower        = new RecipeMap(MachineRecipeMapDefinitions.DistillationTower)
    , CryoDistillationTower    = new RecipeMap(MachineRecipeMapDefinitions.CryoDistillationTower)
    // RU / KU machines
    , Shredder                 = new RecipeMap(MachineRecipeMapDefinitions.Shredder)
    , Lathe                    = new RecipeMap(MachineRecipeMapDefinitions.Lathe)
    , Cutter                   = new RecipeMap(MachineRecipeMapDefinitions.Cutter)
    , Centrifuge               = new RecipeMap(MachineRecipeMapDefinitions.Centrifuge)
    , RollingMill              = new RecipeMap(MachineRecipeMapDefinitions.RollingMill)
    , RollBender               = new RecipeMap(MachineRecipeMapDefinitions.RollBender)
    , RollFormer               = new RecipeMap(MachineRecipeMapDefinitions.RollFormer)
    , ClusterMill              = new RecipeMap(MachineRecipeMapDefinitions.ClusterMill)
    , Wiremill                 = new RecipeMap(MachineRecipeMapDefinitions.Wiremill)
    , Mixer                    = new RecipeMap(MachineRecipeMapDefinitions.Mixer)
    , Loom                     = new RecipeMap(MachineRecipeMapDefinitions.Loom)
    , Sluice                   = new RecipeMap(MachineRecipeMapDefinitions.Sluice)
    , Sharpening               = new RecipeMap(MachineRecipeMapDefinitions.Sharpening)
    , BurnMixer                = new RecipeMap(MachineRecipeMapDefinitions.BurnMixer)
    , PressureWasher           = new RecipeMap(MachineRecipeMapDefinitions.PressureWasher)
    , Crusher                  = new RecipeMap(MachineRecipeMapDefinitions.Crusher)
    , Sifting                  = new RecipeMap(MachineRecipeMapDefinitions.Sifting)
    , Squeezer                 = new RecipeMap(MachineRecipeMapDefinitions.Squeezer)
    , Compressor               = new RecipeMap(MachineRecipeMapDefinitions.Compressor)
    , Press                    = new RecipeMap(MachineRecipeMapDefinitions.Press)
    // Minimal-input validation relaxed vs GT6 (was: 1 item / 2 total): GT6 met those minimums
    // with ST.tag selector pseudo-items, which this port does not have; fluid-only electrolysis
    // (water) and single-dust decomposition are legitimate recipes here.
    , Electrolyzer             = new RecipeMap(MachineRecipeMapDefinitions.Electrolyzer)
    , Canner                   = new RecipeMap(MachineRecipeMapDefinitions.Canner)
    , Injector                 = new RecipeMap(MachineRecipeMapDefinitions.Injector)
    , Printer                  = new RecipeMap(MachineRecipeMapDefinitions.Printer)
    , ScannerVisuals           = new RecipeMap(MachineRecipeMapDefinitions.ScannerVisuals)
    , Autocrafter              = new RecipeMap(MachineRecipeMapDefinitions.Autocrafter).contextualRecipes(gregtech.content.recipe.AutocraftingRecipes::find)
    , Slicer                   = new RecipeMap(MachineRecipeMapDefinitions.Slicer)
    , Nanofab                  = new RecipeMap(MachineRecipeMapDefinitions.Nanofab)
    , Plantalyzer              = new RecipeMap(MachineRecipeMapDefinitions.Plantalyzer)
    , Bumblelyzer              = new RecipeMap(MachineRecipeMapDefinitions.Bumblelyzer)
    , Boxinator                = new RecipeMap(MachineRecipeMapDefinitions.Boxinator)
    , Unboxinator              = new RecipeMap(MachineRecipeMapDefinitions.Unboxinator)
    , Polarizer                = new RecipeMap(MachineRecipeMapDefinitions.Polarizer)
    , MagneticSeparator        = new RecipeMap(MachineRecipeMapDefinitions.MagneticSeparator)
    , LaserEngraver            = new RecipeMap(MachineRecipeMapDefinitions.LaserEngraver)
    , Welder                   = new RecipeMap(MachineRecipeMapDefinitions.Welder)
    , Freezer                  = new RecipeMap(MachineRecipeMapDefinitions.Freezer)
    , CryoMixer                = new RecipeMap(MachineRecipeMapDefinitions.CryoMixer)
    , Massfab                  = new RecipeMap(MachineRecipeMapDefinitions.Massfab)
    , ScannerMolecular         = new RecipeMap(MachineRecipeMapDefinitions.ScannerMolecular)
    , Replicator               = new RecipeMap(MachineRecipeMapDefinitions.Replicator)
    , Autoclave                = new RecipeMap(MachineRecipeMapDefinitions.Autoclave)
    , Bath                     = new RecipeMap(MachineRecipeMapDefinitions.Bath)
    , Generifier               = new RecipeMap(MachineRecipeMapDefinitions.Generifier)
    , Coagulator               = new RecipeMap(MachineRecipeMapDefinitions.Coagulator)
    , Fermenter                = new RecipeMap(MachineRecipeMapDefinitions.Fermenter)
    , Melter                   = new RecipeMap(MachineRecipeMapDefinitions.Melter)
    , CokeOven                 = new RecipeMap(MachineRecipeMapDefinitions.CokeOven)
    , Lightning                = new RecipeMap(MachineRecipeMapDefinitions.Lightning)
    , ImplosionCompressor      = new RecipeMap(MachineRecipeMapDefinitions.ImplosionCompressor)
    , Fusion                   = new RecipeMap(MachineRecipeMapDefinitions.Fusion)

    , Mortar = new RecipeMap(MachineRecipeMapDefinitions.Mortar)

    // ── Misc / special ────────────────────────────────────────────────────
    , DidYouKnow               = new RecipeMap(MachineRecipeMapDefinitions.DidYouKnow)
    , ToolHeads                = new RecipeMap(MachineRecipeMapDefinitions.ToolHeads)
    , Cooking                  = new RecipeMap(MachineRecipeMapDefinitions.Cooking)

    // ── Deprecated / reserved ──────────────────────────────────────────────
    , BlastFurnace             = new RecipeMap(MachineRecipeMapDefinitions.BlastFurnace)
    , VacuumFreezer            = new RecipeMap(MachineRecipeMapDefinitions.VacuumFreezer)
    , Assembler                = new RecipeMap(MachineRecipeMapDefinitions.Assembler)
    , CNC                      = new RecipeMap(MachineRecipeMapDefinitions.CNC)

    // ── GT6 smeltery / alloying maps ──────────────────────────────────────
    // Both crucible maps carry GT6's NBT/NEI special value: the temperature in Kelvin
    // (RM.java declares "Temperature: " / 1 / " K" for CrucibleAlloying and CrucibleSmelting).
    // Both are instant: the crucible converts the moment the input reaches its melting point, so the
    // recipe viewer omits duration for them.
    , CrucibleAlloying         = new RecipeMap(MachineRecipeMapDefinitions.CrucibleAlloying)
    , CrucibleSmelting         = new RecipeMap(MachineRecipeMapDefinitions.CrucibleSmelting)

    // ── Extra / utility ───────────────────────────────────────────────────
    , BedrockOreList           = new RecipeMap(MachineRecipeMapDefinitions.BedrockOreList)
    , ByProductList            = new RecipeMap(MachineRecipeMapDefinitions.ByProductList)
    , Hammer                   = new RecipeMap(MachineRecipeMapDefinitions.Hammer)
    , Chisel                   = new RecipeMap(MachineRecipeMapDefinitions.Chisel)
    , Calciner                 = new RecipeMap(MachineRecipeMapDefinitions.Calciner)
    , Juicer                   = new RecipeMap(MachineRecipeMapDefinitions.Juicer)
    , Anvil                    = new RecipeMap(MachineRecipeMapDefinitions.Anvil)
    , AnvilBendSmall           = new RecipeMap(MachineRecipeMapDefinitions.AnvilBendSmall)
    , AnvilBendBig             = new RecipeMap(MachineRecipeMapDefinitions.AnvilBendBig)
    , BumbleQueens             = new RecipeMap(MachineRecipeMapDefinitions.BumbleQueens)
    , Trees                    = new RecipeMap(MachineRecipeMapDefinitions.Trees)
    ;

    // -- Dynamic (input-computed) providers -------------------------------
    // GT6 subclasses RecipeMap and overrides findRecipe() whenever a row depends on the identity of
    // the inputs rather than on a fixed table: the scanner, printer and replicator read the NBT a
    // scanner wrote onto a USB stick (wired from GregTech's phase-C loader list), and the Bumblelyzer
    // scans whichever living bee sits in its input slots (RecipeMapBumblelyzer). The
    // Bumblelyzer's provider is installed here, in the map's own initializer, so it is in place for
    // every entry point - the loader list, the recipe-viewer index and the game tests alike.
//? if >=1.21.1 {
//?} else {
    static {
        gregtech.content.recipe.GTBumbleBeeRecipes.register();
    }
//?}

    // ── Machine name → RecipeMap lookup ───────────────────────────────────

    // Imported rows use original RM aliases; both platforms must resolve them to the same map.
    public static final RecipeMap Oven = Furnace, Cooker = Furnace, HeatMixer = Mixer,
            Debarker = PressureWasher, buzzsaw = Cutter, sander = Sharpening,
            debarker_alias = PressureWasher, electricmixer = Mixer, electricloom = Loom,
            electricsifter = Sifting, Other = DidYouKnow;

    /** Initializes built-in maps before dependent registries are populated. */
    public static void bootstrap() {}

    /**
     * Legacy import mapping. Unknown names fail instead of silently behaving as a furnace.
     */
    public static RecipeMap byMachineName(String machineName) {
        var spec = BasicMachineCatalog.recipeMap(machineName);
        var map = RecipeMap.RECIPE_MAPS.get(spec.internalName);
        if (map == null) throw new IllegalStateException("Missing built-in recipe map: " + spec.internalName);
        return map;
    }

    // ── Static recipe helpers (1.20.1 port of the gregapi.data.RM helpers) ─
    // Mod-bridge registrations of the original (IC2 macerator, TE pulverizer,
    // Railcraft rock crusher, AE grinder, EtFu smoker/blast) are dropped; the
    // GT6-internal RecipeMaps are filled instead.

    private static boolean invalid(ItemStack aStack) {
        return aStack == null || aStack.isEmpty();
    }

    // ── Pulverizing (Crusher + Shredder) ──────────────────────────────────

    public static boolean pulverizing(ItemStack input, ItemStack output) {
        return pulverizing(input, output, ItemStack.EMPTY, 0);
    }

    /** Registers a Crusher and Shredder row with a by-product chance in percent. */
    public static boolean pulverizing(ItemStack input, ItemStack output, ItemStack byProduct, int chancePercent) {
        if (invalid(input) || invalid(output)) return false;
        long chance = (chancePercent <= 0 ? 10 : chancePercent) * 100L;
        var recipe = RecipeBuilder.of(16, 64)
                .itemInputs(input)
                .itemOutputs(output, byProduct)
                .chances(10000, chance)
                .optimize(true);
        Crusher.register(recipe);
        Shredder.register(recipe);
        return true;
    }

    // ── Smelting (vanilla-style furnace results -> RM.Furnace) ────────────

    /** Adds a furnace row using the map's ordinary collision rules. */
    public static boolean addSmelting(ItemStack input, ItemStack output) {
        return registerSmelting(input, output, false);
    }

    /** Replaces plain furnace rows for this input and keeps its fluid-consuming variants. */
    public static boolean replaceSmelting(ItemStack input, ItemStack output) {
        return registerSmelting(input, output, true);
    }

    private static boolean registerSmelting(ItemStack input, ItemStack output, boolean replacePlain) {
        if (invalid(input) || invalid(output)) return false;
        if (replacePlain) removePlainSmelting(input);
        return Furnace.register(RecipeBuilder.of(16, 16)
                .itemInputs(input)
                .itemOutputs(neverFurnaceOutput(output))
                .optimize(true)) != null;
    }

    /**
     * Replaces the plain rows for an input but keeps fluid-consuming variants: a cooking-oil row
     * ({@link gregtech.content.recipe.CookingOilRecipes}) is a distinct recipe with its own tank
     * input, so the vanilla mirror pass must not erase it while adding the plain row back.
     */
    private static void removePlainSmelting(ItemStack aInput) {
        var pool = Furnace.recipesByInputItem.get(aInput.getItem());
        if (pool == null) return;
        var plain = new java.util.ArrayList<gregtech.engine.recipe.Recipe>();
        for (var row : pool) {
            boolean fluid = false;
            for (var stack : row.fluidInputs) if (stack != null && !stack.isEmpty()) { fluid = true; break; }
            if (!fluid) plain.add(row);
        }
        if (!plain.isEmpty()) Furnace.removeAll(plain);
    }

    /**
     * GT6's "unsmelt" substitution: the scrap form of a {@code NEVER_FURNACE} material, or the
     * stack itself when the material may be furnace-smelted. Amount follows the original
     * {@code (materialAmount * outputCount) / scrapAmount}.
     */
    public static ItemStack neverFurnaceOutput(ItemStack aOutput) {
        if (invalid(aOutput)) return aOutput;
        if (!(aOutput.getItem() instanceof gregtech.engine.material.MaterialFormItem materialItem)) return aOutput;
        gregtech.api.material.GTMaterial material = materialItem.getMaterial().resolve();
        if (material == null || !material.isValid() || !material.has(MaterialProperty.NEVER_FURNACE)) {
            return aOutput;
        }
        long amount = materialItem.getPrefix().getMaterialWeight() * aOutput.getCount();
        long scrapUnit = MaterialPrefix.scrapGt.getMaterialWeight();
        ItemStack scrap = gregtech.content.registry.GTItems.getStack(MaterialPrefix.scrapGt, material,
                (int) Math.max(1, Math.min(64, amount / Math.max(1, scrapUnit))));
        return scrap.isEmpty() ? aOutput : scrap;
    }

    /** Whether the stack's material may not be produced by furnace smelting (GT6 NEVER_FURNACE). */
    public static boolean isNeverFurnace(ItemStack aStack) {
        return aStack != null && !aStack.isEmpty()
                && aStack.getItem() instanceof gregtech.engine.material.MaterialFormItem materialItem
                && materialItem.getMaterial().resolve() != null
                && materialItem.getMaterial().resolve().has(MaterialProperty.NEVER_FURNACE);
    }

    // ── Generifier ────────────────────────────────────────────────────────

    private static final RecipeMap.Registration GENERIFICATION = RecipeMap.Registration.DEFAULT.withLogErrors(false);

    public static boolean generify(ItemStack aStack1, ItemStack aStack2) {
        if (invalid(aStack1) || invalid(aStack2)) return false;
        return Generifier.register(RecipeBuilder.of(0, 1).itemInputs(aStack1).itemOutputs(aStack2),
                GENERIFICATION) != null;
    }

    // ── Pressure washing ──────────────────────────────────────────────────

    public static boolean pressurewash(ItemStack input, ItemStack output) {
        if (invalid(input) || invalid(output)) return false;
        PressureWasher.register(RecipeBuilder.of(16, 64)
                .itemInputs(input)
                .itemOutputs(output)
                .fluidInputs(new FluidStack(Fluids.WATER, 200))
                .optimize(true));
        return true;
    }

    // ── Sawing (Cutter) ───────────────────────────────────────────────────

    /** GT6 water/distilled water/lubricant variants; food never uses industrial oil. */
    public static boolean sawing(long aEUt, long aDuration, boolean aIsFoodItem, long aLubricantAmount, ItemStack aInput, ItemStack... aOutputs) {
        if (invalid(aInput) || aOutputs == null || aOutputs.length <= 0 || invalid(aOutputs[0])) return false;
        long amount = Math.max(1, aLubricantAmount);
        boolean registered = false;
        for (var variant : gregtech.content.recipe.RecipeInputGroups.coolants(aIsFoodItem)) {
            var coolant = gregtech.content.recipe.DyeProcessingRecipes.fluid(variant.field(), Math.toIntExact(amount * variant.multiplier()));
            var recipe = new gregtech.engine.recipe.Recipe(new ItemStack[]{aInput}, aOutputs,
                    null, new FluidStack[]{coolant}, null, aDuration * variant.multiplier(), aEUt, 0).withEmptyContainerInputs();
            registered |= Cutter.addRecipe(variant.apply(recipe)) != null;
        }
        return registered;
    }
}
