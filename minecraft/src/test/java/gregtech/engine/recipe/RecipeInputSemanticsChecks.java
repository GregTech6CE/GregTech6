package gregtech.engine.recipe;

import gregtech.content.data.MaterialPrefix;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.item.PortableFluidContainerItem;
import gregtech.content.registry.GTItems;
import gregtech.engine.material.GTMaterialRegistry;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
//?}

/** Loader-neutral native checks; local stack copies and one finally-restored recipe-map fixture. */
public final class RecipeInputSemanticsChecks {
    private RecipeInputSemanticsChecks() {}

    public static void mixedSlots(Recipe stoneRow, Recipe otherRow, ItemStack stone, ItemStack other, ItemStack wrong) {
        var two = recipe(stone.copyWithCount(2)).withSameItemInputPredicate(0, s -> stoneRow.matchesItemInput(0, s));
        for (var supplied : List.of(List.of(wrong.copyWithCount(64), stone.copy(), stone.copy()),
                List.of(stone.copy(), stone.copy(), wrong.copyWithCount(64)))) {
            var before = supplied.stream().map(ItemStack::copy).toList();
            var remaining = RecipeInputs.consume(two, supplied, List.of(), 1);
            require(remaining != null, "correct host split across later/earlier slots must match");
            for (int slot = 0; slot < supplied.size(); slot++) {
                require(remaining.items().get(slot).getCount() == (supplied.get(slot).getCount() == 64 ? 64 : 0),
                        "only eligible host slots may be consumed: " + slot);
                require(ItemStack.matches(before.get(slot), supplied.get(slot)), "consume must not mutate offered stacks");
            }
        }
        require(RecipeInputs.consume(two, List.of(wrong.copyWithCount(64), stone.copy()), List.of(), 1) == null,
                "wrong host cannot complete a partial correct stack");
        var mixed = recipe(stone.copyWithCount(2), other.copy())
                .withSameItemInputPredicate(0, s -> stoneRow.matchesItemInput(0, s))
                .withSameItemInputPredicate(1, s -> otherRow.matchesItemInput(0, s));
        var inputs = List.of(other.copy(), stone.copy(), wrong.copyWithCount(64), stone.copy());
        var consumed = RecipeInputs.consume(mixed, inputs, List.of(), 1);
        require(consumed != null && consumed.items().get(2).getCount() == 64
                && consumed.items().get(0).isEmpty() && consumed.items().get(1).isEmpty()
                && consumed.items().get(3).isEmpty(), "each required input has its own host predicate");
        var swapped = recipe(stone.copyWithCount(2), other.copy())
                .withSameItemInputPredicate(0, s -> otherRow.matchesItemInput(0, s))
                .withSameItemInputPredicate(1, s -> stoneRow.matchesItemInput(0, s));
        require(RecipeInputs.consume(swapped, inputs, List.of(), 1) == null, "input predicate index affects required amounts");
        var matcher = new RecipeInputs.Matcher(inputs, List.of());
        require(!matcher.matches(swapped, 1) && matcher.matches(mixed, 1), "failed candidates reset the reusable count workspace");
        var kept = recipe(stone.copyWithCount(2), stone.copy()).withCatalystInputs(1)
                .withSameItemInputPredicate(0, s -> stoneRow.matchesItemInput(0, s))
                .withSameItemInputPredicate(1, s -> stoneRow.matchesItemInput(0, s));
        var remaining = RecipeInputs.consume(kept, List.of(wrong.copyWithCount(64), stone.copyWithCount(5)), List.of(), 2);
        require(remaining != null && remaining.items().get(0).getCount() == 64
                && remaining.items().get(1).getCount() == 1, "parallel consumes four correct items and retains one catalyst");
        require(RecipeInputs.consume(kept, List.of(wrong.copyWithCount(64), stone.copyWithCount(4)), List.of(), 2) == null,
                "wrong hosts cannot supply a catalyst or increase parallelism");
        // A mixed marked/unmarked row must not let its unrelated unmarked slot bypass the host predicate.
        var map = MachineRecipeMaps.Mixer;
        require(!map.containsInput(stone), "isolated Mixer fixture must have no pre-existing Cinnabar ore input");
        var indexed = recipe(stone.copy(), new ItemStack(Items.FLINT))
                .withSameItemInputPredicate(0, s -> stoneRow.matchesItemInput(0, s));
        try {
            require(map.addRecipe(indexed) == indexed, "ordinary mixed-slot test admission");
            require(map.containsInput(stone) && !map.containsInput(wrong), "unrelated unmarked slot cannot override marked input filtering");
        } finally { map.unlink(indexed); }
        require(!map.containsInput(stone), "mixed-slot fixture restores its input index");
    }

    public static void legacyChecks(Recipe sourceRow, ItemStack canonical, ItemStack equivalentHost) {
        var decorated = canonical.copy(); extraData(decorated);
        require(RecipeInputs.consume(recipe(canonical.copy()), List.of(decorated), List.of(), 1) != null,
                "unmarked bare requirements retain legacy data wildcard");
        require(RecipeInputs.consume(recipe(decorated), List.of(canonical.copy()), List.of(), 1) == null,
                "unmarked tagged requirements retain exact data matching");
        var semantic = recipe(canonical.copy()).withSameItemInputPredicate(0, s -> sourceRow.matchesItemInput(0, s));
        require(RecipeInputs.consume(semantic, List.of(equivalentHost), List.of(), 1) != null,
                "marked same-host representations need not have identical full data");
        semantic.mExactItemInputs = true;
        require(RecipeInputs.consume(semantic, List.of(equivalentHost), List.of(), 1) == null,
                "explicit exact mode remains stronger than semantic host matching");
        var shell = new ItemStack(Items.CHEST); var contents = shell.copy(); storedData(contents);
        require(RecipeInputs.consume(recipe(shell).withEmptyContainerInputs(), List.of(contents), List.of(), 1) == null,
                "stored contents remain protected");
        var tool = new ItemStack(Items.IRON_PICKAXE); var damaged = tool.copy(); damaged.setDamageValue(1);
        require(RecipeInputs.consume(recipe(tool).withMaterialRecovery(), List.of(damaged), List.of(), 1) == null,
                "material recovery still refuses damaged inputs");
        var vessel = BuiltInRegistries.ITEM.stream().filter(i -> i instanceof PortableFluidContainerItem p
                && p.accepts(new FluidStack(Fluids.WATER, 1000))).findFirst().orElseThrow();
        var empty = new ItemStack(vessel); var handler = FluidUtil.getFluidHandler(empty.copy()).orElse(null);
        require(handler != null && handler.fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE) > 0,
                "actual portable container accepts water in a local stack copy");
        require(RecipeInputs.consume(recipe(empty), List.of(handler.getContainer()), List.of(), 1) == null,
                "filled and empty portable containers are not interchangeable");
        require(RecipeInputs.consume(recipe(empty).withSameItemInputPredicate(0, stack -> true),
                List.of(handler.getContainer()), List.of(), 1) == null, "a custom predicate cannot bypass empty-fluid-shell protection");
        require(RecipeInputs.consume(recipe(empty), List.of(empty.copy()), List.of(), 1) != null,
                "the actual empty shell remains usable");
        var quartz = GTItems.getStack(MaterialPrefix.gem, GTMaterialRegistry.get("NetherQuartz").resolve(), 1);
        var vanilla = new ItemStack(Items.QUARTZ);
        require(!quartz.isEmpty() && quartz.getItem() != vanilla.getItem()
                && RecipeInputs.consume(recipe(quartz), List.of(vanilla), List.of(), 1) != null,
                "existing vanilla material equivalence remains valid on unmarked inputs");
        require(RecipeInputs.consume(recipe(quartz).withSameItemInputPredicate(0, stack -> true),
                List.of(vanilla), List.of(), 1) == null, "opt-in semantic predicates never broaden Item identity");
    }

    private static Recipe recipe(ItemStack... inputs) {
        return new Recipe(inputs, new ItemStack[]{new ItemStack(Items.FLINT)}, null, null, null, null, 1, 16, 0);
    }
    private static void extraData(ItemStack stack) {
//? if >=1.21.1 {
        /*var data = new CompoundTag(); data.putString("fixture", "extra");
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(data));
*///?} else {
        stack.getOrCreateTag().putString("fixture", "extra");
//?}
    }
    private static void storedData(ItemStack stack) {
//? if >=1.21.1 {
        /*var data = new CompoundTag(); data.put("BlockEntityTag", new CompoundTag());
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(data));
*///?} else {
        stack.addTagElement("BlockEntityTag", new CompoundTag());
//?}
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
