package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.addon.platform.AddonRecipes;
import com.gregtech.gregtech.api.recipe.Recipe;
import com.gregtech.gregtech.api.recipe.RecipeInputs;
import com.gregtech.gregtech.api.recipe.RecipeMap;
import com.gregtech.gregtech.api.recipe.data.MachineRecipeDocuments;
import com.gregtech.gregtech.recipe.MachineDatapackRecipe;
import com.gregtech.gregtech.recipe.MachineRecipeOverlay;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/** Real native stacks cannot store a zero-amount fluid identity; presence is separate recipe metadata. */
@GameTestHolder("gregtech_issue_audit")
@PrefixGameTestTemplate(false)
public final class FluidPresenceRecipeTests {
    private static FluidStack water(int amount) { return new FluidStack(Fluids.WATER, amount); }
    private static Recipe row(FluidStack... inputs) {
        return new Recipe(new ItemStack[0], new ItemStack[]{new ItemStack(Items.DIAMOND)}, null, null,
                inputs, new FluidStack[0], 20, 1, 0);
    }
    private static RecipeMap map(String suffix) {
        return new RecipeMap(new ArrayList<>(), "gregtech.issue.presence_" + suffix, "Presence test", "test",
                1, 1, 0, 4, 0, 0, 1, true, false, false, false);
    }
    private static void remove(RecipeMap map) {
        RecipeMap.unregister(map);
    }

    @GameTest(template = "test_empty")
    public static void presenceUsesOriginalTanksWithoutReservation(GameTestHelper h) {
        var gate = row(water(500), water(1)).withPresenceFluidInputs(0, 1);
        h.assertTrue(gate.mFluidInputs[0].getAmount() == 1, "canonical positive identity representative");
        h.assertTrue(RecipeInputs.consume(gate, List.of(), List.of(), 1) == null, "missing gate blocks");
        h.assertTrue(RecipeInputs.consume(gate, List.of(), List.of(new FluidStack(Fluids.LAVA, 100)), 1) == null,
                "wrong fluid blocks");
        var original = water(1);
        var result = RecipeInputs.consume(gate, List.of(), List.of(original), 64);
        h.assertTrue(result != null && result.fluids().get(0).getAmount() == 1 && original.getAmount() == 1,
                "repeated presence gates neither reserve nor multiply nor mutate caller");
        for (boolean gateFirst : new boolean[]{false, true}) {
            var recipe = gateFirst ? row(water(1), water(100), water(1)).withPresenceFluidInputs(0, 2)
                    : row(water(100), water(1), water(1)).withPresenceFluidInputs(1, 2);
            for (int parallel : new int[]{1, 4}) {
                result = RecipeInputs.consume(recipe, List.of(), List.of(water(100 * parallel)), parallel);
                h.assertTrue(result != null && result.fluids().get(0).isEmpty(), "exact consumable amount passes independent of order");
                h.assertTrue(RecipeInputs.consume(recipe, List.of(), List.of(water(100 * parallel - 1)), parallel) == null,
                        "consumable demand still enforced");
            }
        }
        var kept = row(water(100), water(1)).withCatalystFluidInputs(1);
        h.assertTrue(RecipeInputs.consume(kept, List.of(), List.of(water(100)), 1) == null, "positive kept still reserves");
        result = RecipeInputs.consume(kept, List.of(), List.of(water(401)), 4);
        h.assertTrue(result != null && result.fluids().get(0).getAmount() == 1, "positive kept restored once at parallel4");
        h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void addonPresenceSurvivesReadAddFindRoundTrip(GameTestHelper h) {
        var first = map("roundtrip_a");
        var second = map("roundtrip_b");
        try {
            var input = water(100);
            var line = new AddonRecipes.Line(new ItemStack[0], new ItemStack[]{new ItemStack(Items.DIAMOND)},
                    new FluidStack[]{input, water(1), new FluidStack(Fluids.LAVA, 5)}, new FluidStack[0],
                    20, 1, 0, null, null, new int[]{2}, new int[]{1});
            var added = AddonRecipes.map(first.mNameInternal).add(line);
            h.assertTrue(added != null && added.presenceFluidInputs().length == 1 && added.presenceFluidInputs()[0] == 1
                    && added.keptFluidInputs()[0] == 2, "add conversion retains distinct metadata");
            var api = AddonRecipes.map(second.mNameInternal);
            h.assertTrue(api.add(added) != null, "read row can be added again");
            h.assertTrue(api.find(List.of(), List.of(water(100)), false, 0, 1) == null, "positive kept gate still required");
            var found = api.find(List.of(), List.of(water(100), new FluidStack(Fluids.LAVA, 5)), false, 0, 1);
            h.assertTrue(found != null && found.presenceFluidInputs()[0] == 1 && found.keptFluidInputs()[0] == 2,
                    "find conversion retains metadata and exact same-fluid stock succeeds");
            h.assertTrue(input.getAmount() == 100, "native caller stack unmodified");
            boolean rejected = false;
            try { new AddonRecipes.Line(new ItemStack[0], new ItemStack[]{new ItemStack(Items.DIAMOND)},
                    new FluidStack[]{water(0)}, new FluidStack[0], 20, 1, 0); }
            catch (IllegalArgumentException expected) { rejected = true; }
            h.assertTrue(rejected, "unrecoverable native zero is rejected at public boundary");
        } finally { remove(first); remove(second); }
        h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void presenceCollisionDoesNotBecomeConsumableStock(GameTestHelper h) {
        var map = map("collision");
        try {
            var consumed = row(water(100));
            map.add(consumed);
            h.assertTrue(map.findCollision(row(water(1)).withPresenceFluidInputs(0)) == null,
                    "presence representative cannot cover consumed demand");
            map.unlink(consumed);
            var presence = row(water(1), water(1)).withPresenceFluidInputs(0, 1);
            map.add(presence);
            h.assertTrue(map.findCollision(row(water(100))) == presence, "repeated presence covered by same original fluid");
            h.assertTrue(map.findCollision(row(new FluidStack(Fluids.LAVA, 100))) == null, "wrong-fluid collision absent");
            map.unlink(presence);
            var mixed = row(water(100), water(1)).withPresenceFluidInputs(1);
            map.add(mixed);
            h.assertTrue(map.findCollision(row(water(100))) == mixed, "presence collision sees original stock after consumed demand");
        } finally { remove(map); }
        h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void unsupportedDirectFuelMapsFailClosed(GameTestHelper h) {
        for (String suffix : new String[]{"burn", "gas", "fluidbed", "engine", "hot"}) {
            String name = "gt.recipe.fuels." + suffix;
            var registered = RecipeMap.unregister(name);
            RecipeMap probe = null;
            try {
                probe = new RecipeMap(new ArrayList<>(), name, "Presence boundary test", "test",
                        1, 1, 0, 4, 1, 0, 1, false, false, false, false);
                var presence = row(water(1)).withPresenceFluidInputs(0);
                h.assertTrue(probe.add(presence) == null && probe.mRecipeList.isEmpty(), "direct add rejects " + name);
                h.assertTrue(probe.addRecipe(presence, false, false, false) == null, "validated add rejects " + name);
                probe.dynamicRecipes((items, fluids) -> presence);
                h.assertTrue(probe.findRecipe(List.of(), List.of(water(100)), false, 0, 1) == null,
                        "dynamic cannot bypass " + name);
                var ordinary = row(water(100));
                probe.addDynamicRecipes((items, fluids) -> ordinary);
                h.assertTrue(probe.findRecipe(List.of(), List.of(water(100)), false, 0, 1) == ordinary,
                        "rejected provider does not shadow ordinary fallback " + name);
            } finally {
                if (probe != null) remove(probe);
                if (registered != null) RecipeMap.restore(registered);
            }
        }
        var custom = new RecipeMap(new ArrayList<>(), "gt.recipe.fuels.hot_custom_presence", "Custom fuel", "test",
                1, 1, 0, 1, 0, 0, 1, false, false, false, false);
        try {
            var presence = row(water(1)).withPresenceFluidInputs(0);
            h.assertTrue(custom.add(presence) == presence
                    && custom.findRecipe(List.of(), List.of(water(1)), false, 0, 1) == presence,
                    "similar custom names are not blanket rejected");
            var fuel = new Recipe(new ItemStack[0], new ItemStack[0], null, null,
                    new FluidStack[]{water(1)}, new FluidStack[0], 20, -1, 0).withPresenceFluidInputs(0);
            var batch = com.gregtech.gregtech.api.recipe.FluidFuelBatch.plan(fuel, water(1),
                    List.of(), new int[0], 20, 10000);
            h.assertTrue(batch != null && batch.input().getAmount() == 1 && batch.energy() == 20,
                    "standard fuel batch preserves presence and still credits recipe energy");
        } finally { remove(custom); }
        h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void barrelSkipsPresenceWithoutDisablingOrdinaryFermenter(GameTestHelper h) {
        var map = com.gregtech.gregtech.data.MachineRecipeMaps.Fermenter;
        var original = new ArrayList<>(map.mRecipeList);
        var selector = com.gregtech.gregtech.registry.GTTechnological.selectorTag(0);
        h.assertTrue(selector != null, "registered fermentation selector");
        var presence = new Recipe(new ItemStack[]{new ItemStack(selector)}, new ItemStack[0], null, null,
                new FluidStack[]{water(1)}, new FluidStack[]{new FluidStack(Fluids.LAVA, 10)}, 20, 16, 0)
                .withPresenceFluidInputs(0);
        var ordinary = new Recipe(new ItemStack[]{new ItemStack(selector)}, new ItemStack[0], null, null,
                new FluidStack[]{water(100)}, new FluidStack[]{new FluidStack(Fluids.LAVA, 10)}, 20, 16, 0);
        try {
            map.mRecipeList.clear();
            h.assertTrue(map.add(presence) == presence, "ordinary fermenter map accepts presence");
            h.assertTrue(map.findRecipe(List.of(new ItemStack(selector)), List.of(water(1)), false, 1, 1) == presence,
                    "ordinary fermenter matching still supports presence");
            h.assertTrue(com.gregtech.gregtech.blockentity.machine.TankBlockEntity.findFermentationRecipe(water(100)) == null,
                    "barrel does not reinterpret identity representative as consumed quantity");
            map.add(ordinary);
            var found = com.gregtech.gregtech.blockentity.machine.TankBlockEntity.findFermentationRecipe(water(100));
//? if >=1.21.1 {
            /*h.assertTrue(found != null && found.input().getAmount() == 100, "barrel still finds ordinary row");
*///?} else {
            h.assertTrue(found == ordinary, "barrel still finds ordinary row");
//?}
        } finally {
            map.unlink(presence);
            map.unlink(ordinary);
            map.mRecipeList.clear();
            map.mRecipeList.addAll(original);
        }
        h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void customExchangerMapRejectsPresenceWithoutDraining(GameTestHelper h) {
        var map = map("custom_exchanger");
        try {
            var data = new net.minecraft.nbt.CompoundTag();
            data.putString("gt.fuelmap", map.mNameInternal);
            data.putLong("gt.output", 10);
            var settings = com.gregtech.gregtech.api.fluid.HeatExchangerData.settings(data);
            h.assertTrue(settings.fuelMap().equals(map.mNameInternal), "native saved gt.fuelmap resolves custom map");
            var presence = new Recipe(new ItemStack[0], new ItemStack[0], null, null,
                    new FluidStack[]{water(1)}, new FluidStack[]{new FluidStack(Fluids.LAVA, 10)}, 20, -1, 0)
                    .withPresenceFluidInputs(0);
            map.add(presence);
            var hot = new com.gregtech.gregtech.api.fluid.FluidTankGT(1000);
            var cold = new com.gregtech.gregtech.api.fluid.FluidTankGT(1000);
            hot.setFluid(water(100));
            for (Recipe cached : new Recipe[]{null, presence}) {
                var result = com.gregtech.gregtech.api.fluid.HotFluidFuelCycle.charge(settings, hot, cold, 0, 0, cached);
                h.assertTrue(!result.active() && result.heat() == 0 && hot.getAmount() == 100 && cold.isEmpty(),
                        "unsupported presence neither drains/purges nor credits energy, cached or new");
            }
            var ordinary = new Recipe(new ItemStack[0], new ItemStack[0], null, null,
                    new FluidStack[]{water(100)}, new FluidStack[]{new FluidStack(Fluids.LAVA, 10)}, 20, -1, 0);
            map.add(ordinary);
            var result = com.gregtech.gregtech.api.fluid.HotFluidFuelCycle.charge(settings, hot, cold, 0, 0, presence);
            h.assertTrue(result.active() && result.recipe() == ordinary && result.heat() == 20
                    && hot.isEmpty() && cold.getAmount() == 10, "presence cache does not shadow ordinary converter row");
        } finally { remove(map); }
        h.succeed();
    }

    @GameTest(template = "test_empty", batch = "machine_recipe_overlay", timeoutTicks = 8000)
    public static void textualZeroOverlayRetainsNativeFluidIdentity(GameTestHelper h) {
        var map = map("overlay");
        var server = h.getLevel().getServer();
        try {
            var spec = MachineRecipeDocuments.addition("{\"map\":\"" + map.mNameInternal
                    + "\",\"fluid_inputs\":[{\"fluid\":\"minecraft:water\",\"amount\":100},"
                    + "{\"fluid\":\"minecraft:water\",\"amount\":0},{\"fluid\":\"minecraft:water\",\"amount\":0}],"
                    + "\"item_outputs\":[{\"item\":\"minecraft:diamond\",\"count\":1}],\"duration\":20,\"eu_per_tick\":1}");
            var id = ResourceLocation.fromNamespaceAndPath("gregtech_issue_audit", "fluid_presence");
//? if >=1.21.1 {
            /*var manager = new net.minecraft.world.item.crafting.RecipeManager(server.registryAccess());
            var rows = new ArrayList<net.minecraft.world.item.crafting.RecipeHolder<?>>(server.getRecipeManager().getRecipes());
            rows.add(new net.minecraft.world.item.crafting.RecipeHolder<>(id, MachineDatapackRecipe.addition(spec)));
*///?} else {
            var manager = new net.minecraft.world.item.crafting.RecipeManager();
            var rows = new ArrayList<net.minecraft.world.item.crafting.Recipe<?>>(server.getRecipeManager().getRecipes());
            rows.add(MachineDatapackRecipe.addition(id, spec));
//?}
            manager.replaceRecipes(rows);
            MachineRecipeOverlay.apply(manager);
            h.assertTrue(map.mRecipeList.size() == 1, "textual zero row installed");
            var recipe = map.mRecipeList.iterator().next();
            h.assertTrue(recipe.isPresenceFluidInput(1) && recipe.isPresenceFluidInput(2)
                    && recipe.mFluidInputs[1].getFluid() == Fluids.WATER, "zero identity retained before native copies");
            h.assertTrue(map.findRecipe(List.of(), List.of(), false, 0, 1) == null, "absent fluid rejected");
            var remaining = RecipeInputs.consume(recipe, List.of(), List.of(water(400)), 4);
            h.assertTrue(remaining != null && remaining.fluids().get(0).isEmpty(), "overlay zero does not reserve an extra mB");
        } finally {
            MachineRecipeOverlay.apply(server.getRecipeManager());
            remove(map);
        }
        h.succeed();
    }
}
