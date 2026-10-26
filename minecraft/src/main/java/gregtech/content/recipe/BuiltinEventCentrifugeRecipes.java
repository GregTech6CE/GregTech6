package gregtech.content.recipe;

import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.food.GTFoodItems;
import gregtech.content.recipe.SourceCentrifugeBuiltinEvents.*;
import gregtech.content.recipe.generated.GeneratedRecipeResolver;
import gregtech.content.registry.GTMultiItems;
import gregtech.engine.material.MaterialEquivalence;
import gregtech.engine.mod.SetupOnce;
import gregtech.engine.recipe.Recipe;
import gregtech.engine.recipe.RecipeMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
//? if >=1.21.1 {
/*import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
*///?} else {
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
//?}

/** Four original default C event instances, NOT a modern OreDictionary/tag listener.
 * Runs once in load-complete queued work, after native capability/default-component providers
 * and all existing bootstrap/addon recipes: existing recipes win ordinary collisions. No row sweep,
 * ownership rebuild, registry mutation, or reinterpretation on datapack reload occurs here.
 * Source defaults are finite pinned registrations; missing native tags do not erase them.
 */
//? if >=1.21.1 {
/*@EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
*///?} else {
@Mod.EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
//?}
public final class BuiltinEventCentrifugeRecipes {
    private static final SetupOnce SETUP = new SetupOnce();
    private static final List<Outcome> OUTCOMES = new ArrayList<>();
    private BuiltinEventCentrifugeRecipes() {}

    /** These records report only this producer's startup observations and returned new rows.
     * They do not assign ownership to a pre-existing collision or imply dynamic event coverage. */
    public record IngredientProbe(Fact capabilityPresent, boolean capacityReadComplete,
                                  List<Long> tankCapacities, Fact remainder, IngredientFacts facts) {
        public IngredientProbe { tankCapacities = List.copyOf(tankCapacities); }
    }
    public record Outcome(Instance instance, String status, String reason, Facts facts,
                          IngredientProbe ingredientProbe, Recipe acceptedRecipe) {}
    private record Pending(Recipe recipe, Facts facts, IngredientProbe ingredient, String reason) {}
    public static List<Outcome> outcomes() { return List.copyOf(OUTCOMES); }

    @SubscribeEvent
    public static void loadComplete(FMLLoadCompleteEvent event) {
        // Both loaders drain this queue after native capability registration and before servers
        // bind tags/capture recipe baselines. Never sample absent Neo capabilities in common setup.
        java.util.Objects.requireNonNull(event, "load-complete event").enqueueWork(
                () -> registerAfterNativeLoad());
    }

    // Private to this actual lifecycle entry: an early public call cannot consume SETUP.
    private static int registerAfterNativeLoad() {
        SETUP.run(() -> {
            Shell shell = shellDefault();
            for (Instance instance : SourceCentrifugeBuiltinEvents.instances()) {
                final Pending pending;
                try { pending = prepareOne(instance, shell); }
                catch (RuntimeException | LinkageError unreviewedRuntime) {
                    OUTCOMES.add(new Outcome(instance, "DEFER", "NATIVE_OBSERVATION_FAILED:"
                            + unreviewedRuntime.getClass().getSimpleName(), null, null, null));
                    continue;
                }
                if (pending != null) {
                    // Publication is outside the observation catch: an unexpected mutation failure
                    // must propagate, never be mislabeled as a safely deferred/absent row.
                    Recipe accepted = MachineRecipeMaps.Centrifuge.addRecipe(pending.recipe());
                    OUTCOMES.add(new Outcome(instance, accepted == null ? "SKIPPED_COLLISION_OR_MAP_REJECTION" : "ADDED",
                            accepted == null ? "EXISTING_ROWS_WIN_NO_REPLACEMENT" : pending.reason(),
                            pending.facts(), pending.ingredient(), accepted));
                }
            }
        });
        return (int) OUTCOMES.stream().filter(o -> o.acceptedRecipe() != null).count();
    }

    private static Pending prepareOne(Instance instance, Shell shell) {
        ItemStack input = exactInput(instance);
        IngredientProbe ingredient = instance == Instance.VANILLA_SLIME ? ingredient(input) : null;
        Facts facts = SourceCentrifugeBuiltinEvents.sourceDefaults(instance,
                input.isEmpty() ? Fact.NO : Fact.YES, ingredient == null ? null : ingredient.facts(), shell);
        Result decision = SourceCentrifugeBuiltinEvents.decide(instance, facts);
        if (decision.decision() != Decision.PLAN) {
            OUTCOMES.add(new Outcome(instance, decision.decision().name(), decision.reason(), facts, ingredient, null));
            return null;
        }
        Plan plan = decision.plan();
        List<ItemStack> items = new ArrayList<>();
        for (ItemAmount output : plan.itemOutputs()) {
            ItemStack stack = outputItem(output.role());
            if (stack.isEmpty()) {
                OUTCOMES.add(new Outcome(instance, "DEFER", "UNREVIEWED_OUTPUT:" + output.role(), facts, ingredient, null));
                return null;
            }
            ItemStack canonical = gregtech.engine.material.MaterialUnification.canonical(stack.copy());
            if (!plain(canonical) || canonical.getItem() != stack.getItem()) {
                OUTCOMES.add(new Outcome(instance, "DEFER", "CHANGED_OUTPUT_UNIFICATION:" + output.role(), facts, ingredient, null));
                return null;
            }
            stack.setCount(output.count()); items.add(stack);
        }
        List<FluidStack> fluids = new ArrayList<>();
        for (FluidAmount output : plan.fluidOutputs()) {
            String field = output.role() == FluidRole.LATEX ? "Latex" : "Glue";
            FluidStack fluid = DyeProcessingRecipes.fluid(field, output.millibuckets());
            ResourceLocation expectedId = ResourceLocation.parse(output.role() == FluidRole.LATEX ? "gt6ce:latex" : "gt6ce:glue");
            if (fluid == null || fluid.isEmpty() || fluid.getFluid() == Fluids.EMPTY
                    || fluid.getAmount() != output.millibuckets()
                    || !expectedId.equals(BuiltInRegistries.FLUID.getKey(fluid.getFluid()))
                    || BuiltInRegistries.FLUID.get(expectedId) != fluid.getFluid()) {
                OUTCOMES.add(new Outcome(instance, "DEFER", "UNREVIEWED_FLUID:" + field, facts, ingredient, null));
                return null;
            }
            fluids.add(fluid.copy());
        }
        long[] chances = plan.chances().stream().mapToLong(Long::longValue).toArray();
        Recipe candidate = RecipeMap.make(plan.optimize(), new ItemStack[]{input.copy()},
                items.toArray(RecipeMap.ZL_IS), null, chances, RecipeMap.ZL_FS,
                fluids.toArray(RecipeMap.ZL_FS), plan.ticks(), plan.power(), 0);
        if (candidate == null) {
            OUTCOMES.add(new Outcome(instance, "DEFER", "MAP_CANDIDATE_REJECTED", facts, ingredient, null));
            return null;
        }
        return new Pending(candidate, facts, ingredient, decision.reason());
    }

    private static ItemStack exactInput(Instance instance) {
        if (instance.originalMeta() >= 0) return ownFood(instance.registryId(), instance.originalMeta());
        Item expected = instance == Instance.VANILLA_EGG ? Items.EGG : Items.SLIME_BALL;
        ItemStack stack = exactItem(instance.registryId());
        if (stack.isEmpty() || stack.getItem() != expected) return ItemStack.EMPTY;
        // ST.ingredable's original tool/durability/special-cell identities are disjoint from this
        // exact plain vanilla Item. An unreviewed replacement/subclass does not inherit eligibility.
        if (instance == Instance.VANILLA_SLIME && stack.getItem().getClass() != Item.class) return ItemStack.EMPTY;
        return stack;
    }
    private static ItemStack ownFood(String id, int originalMeta) {
        ItemStack stack = exactItem(id);
        if (stack.isEmpty()) return ItemStack.EMPTY;
        var entry = GTFoodItems.entryFor(stack.getItem());
        if (entry == null || entry.meta() != originalMeta || !id.equals("gt6ce:" + entry.id())) return ItemStack.EMPTY;
        boolean registered = false;
        for (var holder : GTMultiItems.all()) if (holder.get() == stack.getItem()) { registered = true; break; }
        return registered ? stack : ItemStack.EMPTY;
    }
    private static ItemStack outputItem(ItemRole role) {
        return switch (role) {
            case EGG_WHITE -> ownFood("gt6ce:egg_white", 1074);
            case EGG_YOLK -> ownFood("gt6ce:egg_yolk", 1073);
            case BONE_MEAL -> boneMeal();
            // The exact legacy exoticbirds symbol is known; no modern item/version binding is.
            case REVIEWED_BIRB_SHELL -> ItemStack.EMPTY;
        };
    }
    private static ItemStack boneMeal() {
        // Reuse the existing reviewed C Bone target policy, not a new global unifier.
        ItemStack selected = GeneratedRecipeResolver.resolveSpec("sci:Bone:dust:1");
        if (!plain(selected) || selected.getItem() != Items.BONE_MEAL) return ItemStack.EMPTY;
        var form = MaterialEquivalence.form(selected);
        if (form == null || !form.prefix().getName().equals("dust")
                || form.material().getId() != 8219 || !form.material().getName().equals("Bone")) return ItemStack.EMPTY;
        return selected.copy();
    }
    private static ItemStack exactItem(String id) {
        ResourceLocation key = ResourceLocation.tryParse(id);
        if (key == null || !BuiltInRegistries.ITEM.containsKey(key)) return ItemStack.EMPTY;
        Item item = BuiltInRegistries.ITEM.get(key);
        if (item == null || item == Items.AIR || !key.equals(BuiltInRegistries.ITEM.getKey(item))) return ItemStack.EMPTY;
        ItemStack stack = item.getDefaultInstance();
        return plain(stack) && stack.getItem() == item ? stack.copy() : ItemStack.EMPTY;
    }
    private static boolean plain(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getCount() != 1 || stack.isDamaged()) return false;
//? if >=1.21.1 {
        /*return stack.getComponentsPatch().isEmpty();*/
//?} else {
        return !stack.hasTag();
//?}
    }

    /** Complete native provider list, not an arbitrary tag/ID guess. A present but unreviewed
     * exoticbirds binding defers both egg rows; absence enables only the reviewed Bone fallback. */
    private static Shell shellDefault() {
        try {
            var mods = ModList.get();
            if (mods == null) return Shell.UNKNOWN;
            var ids = new HashSet<String>();
            for (var mod : mods.getMods()) if (mod.getModId() == null || !ids.add(mod.getModId())) return Shell.UNKNOWN;
//? if >=1.21.1 {
            /*String loader = "neoforge";*/
//?} else {
            String loader = "forge";
//?}
            if (!ids.contains("minecraft") || !ids.contains("gt6ce") || !ids.contains(loader)) return Shell.UNKNOWN;
            return ids.contains("exoticbirds") ? Shell.UNKNOWN : Shell.ABSENT;
        } catch (RuntimeException | LinkageError unavailableProviderList) { return Shell.UNKNOWN; }
    }

    /** Read only a local default-stack copy. No fill/drain, registry mutation or synthetic
     * capability provider is used. Failure/negative capacity remains UNKNOWN, never eligible. */
    private static IngredientProbe ingredient(ItemStack input) {
        if (!plain(input) || input.getItem() != Items.SLIME_BALL || input.getItem().getClass() != Item.class)
            return new IngredientProbe(Fact.UNKNOWN, false, List.of(), Fact.UNKNOWN,
                    new IngredientFacts(Fact.NO, Fact.UNKNOWN, Fact.UNKNOWN, Fact.UNKNOWN,
                            Fact.UNKNOWN, Fact.UNKNOWN, Fact.UNKNOWN));
        Fact present = Fact.UNKNOWN, remainder = Fact.UNKNOWN;
        boolean complete = false;
        List<Long> capacities = new ArrayList<>();
        try {
//? if >=1.21.1 {
            /*var handler = FluidUtil.getFluidHandler(input.copy()).orElse(null);*/
//?} else {
            var handler = FluidUtil.getFluidHandler(input.copy()).resolve().orElse(null);
//?}
            present = handler == null ? Fact.NO : Fact.YES;
            if (handler != null) {
                int tanks = handler.getTanks();
                if (tanks < 0) throw new IllegalStateException("Negative tank count");
                for (int tank = 0; tank < tanks; tank++) capacities.add((long) handler.getTankCapacity(tank));
            }
            complete = true;
        } catch (RuntimeException | LinkageError unreviewedCapability) { complete = false; }
        try {
            ItemStack copy = input.copy();
            remainder = copy.hasCraftingRemainingItem() || !copy.getCraftingRemainingItem().isEmpty() ? Fact.YES : Fact.NO;
        } catch (RuntimeException | LinkageError unreviewedRemainder) { remainder = Fact.UNKNOWN; }
        Fact capacity = SourceCentrifugeBuiltinEvents.positiveFluidCapacity(present, complete, capacities);
        return new IngredientProbe(present, complete, capacities, remainder,
                new IngredientFacts(Fact.YES, Fact.NO, capacity, remainder, Fact.NO, Fact.NO, Fact.NO));
    }
}
