package gregtech.gametest;

import gregtech.internal.machine.BasicMachineSpec;
//? if >=1.21.1 {
/**///?} else {
import gregtech.internal.machine.MachineRegistry;
//?}
import gregtech.internal.recipe.Recipe;
//? if >=1.21.1 {
/*import gregtech.blockentity.machine.BasicMachineBlockEntity;
*///?} else {
//?}
import gregtech.data.GregTechTags;
//? if >=1.21.1 {
/**///?} else {
import gregtech.blockentity.machine.BasicMachineBlockEntity;
//?}
import gregtech.data.MachineRecipeMaps;
//? if >=1.21.1 {
/*import gregtech.platform.neoforge.machine.BasicMachineRegistries;
*///?} else {
//?}
import gregtech.recipe.MachineRecipeOverlay;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.LevelResource;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** One datapack reload path: add, remove, tag expansion, a real machine, and a second identical reload. */
@GameTestHolder("gregtech_machine_overlay")
@PrefixGameTestTemplate(false)
public final class MachineRecipeOverlayTests {
    private MachineRecipeOverlayTests() {}

    @GameTest(template = "test_empty", batch = "machine_recipe_overlay", timeoutTicks = 200)
    public static void furnaceMarkersSurviveOvenMirrors(GameTestHelper helper) {
        verifyFurnaceMirrorOverlay(helper.getLevel().getServer());
        helper.succeed();
    }

    /** Also callable from the single opt-in client/world smoke, without starting another world. */
    public static int verifyFurnaceMirrorOverlay(net.minecraft.server.MinecraftServer server) {
        var furnace = MachineRecipeMaps.Furnace;
        int checks = 0;
        try {
            for (int mode = 1; mode <= 2; mode++) {
                var manager = furnaceProbeManager(server, mode);
                MachineRecipeOverlay.apply(manager);
                int applications = MachineRecipeOverlay.applications();
                int otherRows = MachineRecipeMaps.Crusher.mRecipeList.size();
                for (int round = 0; round < 2; round++) {
                    gregtech.content.recipe.OvenRecipeBridge.apply(manager, server.registryAccess());
                    requireFurnace(recipesWith(furnace, Items.JIGSAW) == 0,
                            "the oven mirror reintroduced the removed Jigsaw input");
                    requireFurnace(recipesWith(furnace, Items.STRUCTURE_VOID) == 1,
                            "the custom Furnace input is missing or duplicated");
                    var row = furnace.findRecipe(List.of(new ItemStack(Items.STRUCTURE_VOID)), List.of(), false, 1, 12);
                    requireFurnace(row != null && row.mOutputs[0].is(Items.NETHER_STAR),
                            "the oven mirror replaced the custom Furnace output with vanilla smelting");
                    if (mode == 1) requireFurnace(furnace.mRecipeList.size() == 1,
                            "clear:true must still clear the vanilla rows imported by the oven");
                    int rows = furnace.mRecipeList.size();
                    MachineRecipeOverlay.reapplyFurnace(manager);
                    requireFurnace(furnace.mRecipeList.size() == rows,
                            "reapplying Furnace markers duplicated their additions");
                    requireFurnace(MachineRecipeMaps.Crusher.mRecipeList.size() == otherRows
                                    && MachineRecipeOverlay.applications() == applications,
                            "the oven must not rebuild other maps or apply the full overlay again");
                    checks++;
                }
            }
            var withoutMarkers = furnaceProbeManager(server, 0);
            MachineRecipeOverlay.apply(withoutMarkers);
            gregtech.content.recipe.OvenRecipeBridge.apply(withoutMarkers, server.registryAccess());
            var restored = furnace.findRecipe(List.of(new ItemStack(Items.STRUCTURE_VOID)), List.of(), false, 1, 12);
            requireFurnace(restored != null && restored.mOutputs[0].is(Items.EMERALD),
                    "dropping the markers left a stale custom Furnace row in displaced mirrors");
            requireFurnace(recipesWith(furnace, Items.JIGSAW) == 1,
                    "dropping the removal did not restore the vanilla Furnace mirror");
            checks++;
        } finally {
            MachineRecipeOverlay.apply(server.getRecipeManager());
            gregtech.content.recipe.OvenRecipeBridge.apply(server.getRecipeManager(), server.registryAccess());
        }
        return checks;
    }

    private static void requireFurnace(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static net.minecraft.world.item.crafting.RecipeManager furnaceProbeManager(
            net.minecraft.server.MinecraftServer server, int mode) {
//? if >=1.21.1 {
/*        var manager = new net.minecraft.world.item.crafting.RecipeManager(server.registryAccess());
*///?} else {
        var manager = new net.minecraft.world.item.crafting.RecipeManager();
//?}
        var furnace = MachineRecipeMaps.Furnace;
        var ids = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech_machine_overlay", "furnace_probe");
        var clear = gregtech.internal.recipe.data.MachineRecipeDocuments.removal(
                mode == 1 ? "{\"map\":\"" + furnace.mNameInternal + "\",\"clear\":true}"
                        : removal(furnace.mNameInternal, "minecraft:structure_void", 1));
        var removed = gregtech.internal.recipe.data.MachineRecipeDocuments.removal(
                removal(furnace.mNameInternal, "minecraft:jigsaw", 1));
        var added = gregtech.internal.recipe.data.MachineRecipeDocuments.addition(
                addition(furnace.mNameInternal, "minecraft:structure_void", "minecraft:nether_star"));
//? if >=1.21.1 {
/*        var rows = new ArrayList<net.minecraft.world.item.crafting.RecipeHolder<?>>(server.getRecipeManager().getRecipes());
        rows.add(new net.minecraft.world.item.crafting.RecipeHolder<>(ids.withSuffix("_vanilla_a"),
                new net.minecraft.world.item.crafting.SmeltingRecipe("", net.minecraft.world.item.crafting.CookingBookCategory.MISC,
                        net.minecraft.world.item.crafting.Ingredient.of(Items.STRUCTURE_VOID), new ItemStack(Items.EMERALD), 0, 200)));
        rows.add(new net.minecraft.world.item.crafting.RecipeHolder<>(ids.withSuffix("_vanilla_b"),
                new net.minecraft.world.item.crafting.SmeltingRecipe("", net.minecraft.world.item.crafting.CookingBookCategory.MISC,
                        net.minecraft.world.item.crafting.Ingredient.of(Items.JIGSAW), new ItemStack(Items.DIAMOND), 0, 200)));
        if (mode != 0) {
            rows.add(new net.minecraft.world.item.crafting.RecipeHolder<>(ids.withSuffix("_remove_a"),
                    gregtech.recipe.MachineDatapackRecipe.removal(clear)));
            rows.add(new net.minecraft.world.item.crafting.RecipeHolder<>(ids.withSuffix("_remove_b"),
                    gregtech.recipe.MachineDatapackRecipe.removal(removed)));
            rows.add(new net.minecraft.world.item.crafting.RecipeHolder<>(ids.withSuffix("_add"),
                    gregtech.recipe.MachineDatapackRecipe.addition(added)));
        }
*///?} else {
        var rows = new ArrayList<net.minecraft.world.item.crafting.Recipe<?>>(server.getRecipeManager().getRecipes());
        rows.add(new net.minecraft.world.item.crafting.SmeltingRecipe(ids.withSuffix("_vanilla_a"), "",
                net.minecraft.world.item.crafting.CookingBookCategory.MISC,
                net.minecraft.world.item.crafting.Ingredient.of(Items.STRUCTURE_VOID), new ItemStack(Items.EMERALD), 0, 200));
        rows.add(new net.minecraft.world.item.crafting.SmeltingRecipe(ids.withSuffix("_vanilla_b"), "",
                net.minecraft.world.item.crafting.CookingBookCategory.MISC,
                net.minecraft.world.item.crafting.Ingredient.of(Items.JIGSAW), new ItemStack(Items.DIAMOND), 0, 200));
        if (mode != 0) {
            rows.add(gregtech.recipe.MachineDatapackRecipe.removal(ids.withSuffix("_remove_a"), clear));
            rows.add(gregtech.recipe.MachineDatapackRecipe.removal(ids.withSuffix("_remove_b"), removed));
            rows.add(gregtech.recipe.MachineDatapackRecipe.addition(ids.withSuffix("_add"), added));
        }
//?}
        manager.replaceRecipes(rows);
        return manager;
    }

    @GameTest(template = "test_empty", batch = "machine_recipe_overlay", timeoutTicks = 8000)
    public static void datapackAddsAndRemovesMachineRecipes(GameTestHelper helper) throws Exception {
        var server = helper.getLevel().getServer();
        var ids = new ArrayList<>(server.getPackRepository().getSelectedIds());
        for (int round = 0; round < 2; round++) server.reloadResources(ids).join();
        int untouched = MachineRecipeOverlay.staticRows();
        var crusher = MachineRecipeMaps.Crusher;
        var victim = uniqueInput(crusher);
        helper.assertTrue(victim != null, "crusher has a one-input recipe that can be removed exactly");
        Item input = onlyInput(victim).getItem();
        String inputId = BuiltInRegistries.ITEM.getKey(input).toString();
        helper.assertTrue(crusher.mRecipeList.contains(victim), "the victim starts in the crusher map");

        Path root = server.getWorldPath(LevelResource.DATAPACK_DIR).resolve("gt-machine-overlay-probe");
        helper.assertTrue(!Files.exists(root), "temporary machine overlay pack must not overwrite an existing pack");
//? if >=1.21.1 {
/*        Path addition = root.resolve("data/gt6ce/recipe/machine/heart_of_the_sea.json");
        Path tagged = root.resolve("data/gt6ce/recipe/machine/soul_fire.json");
        Path removal = root.resolve("data/gt6ce/recipe/machine/remove_unique.json");
*///?} else {
        Path addition = root.resolve("data/gt6ce/recipes/machine/heart_of_the_sea.json");
        Path tagged = root.resolve("data/gt6ce/recipes/machine/soul_fire.json");
        Path removal = root.resolve("data/gt6ce/recipes/machine/remove_unique.json");
//?}
        Path metadata = root.resolve("pack.mcmeta");
        Files.createDirectories(addition.getParent());
        try {
//? if >=1.21.1 {
/*            Files.writeString(metadata, "{\"pack\":{\"pack_format\":48,\"description\":\"Temporary GT machine recipe overlay\"}}");
*///?} else {
            Files.writeString(metadata, "{\"pack\":{\"pack_format\":15,\"description\":\"Temporary GT machine recipe overlay\"}}");
//?}
            Files.writeString(addition, addition(crusher.mNameInternal, "minecraft:heart_of_the_sea", "minecraft:nether_star"));
            Files.writeString(tagged, tagAddition(crusher.mNameInternal, "minecraft:soul_fire_base_blocks", "minecraft:diamond"));
            Files.writeString(removal, removal(crusher.mNameInternal, inputId, onlyInput(victim).getCount()));
            server.getPackRepository().reload();
            var withProbe = new ArrayList<>(ids);
            withProbe.add("file/gt-machine-overlay-probe");
            server.reloadResources(withProbe).join();
            assertOverlay(helper, crusher, victim, input);
            int overlaid = MachineRecipeOverlay.staticRows();
            server.reloadResources(withProbe).join();
            helper.assertTrue(MachineRecipeOverlay.staticRows() == overlaid, "a second reload does not add the datapack row again");
            assertOverlay(helper, crusher, victim, input);

//? if >=1.21.1 {
/*            var block = BasicMachineRegistries.all().stream().map(entry -> entry.get())
*///?} else {
            var block = MachineRegistry.basicMachines().stream().map(entry -> entry.get())
//?}
                    .filter(candidate -> candidate.basicSpec().machineName().equals("crusher")).findFirst().orElseThrow();
            var pos = BlockPos.ZERO;
            helper.setBlock(pos, block);
            var machine = (BasicMachineBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
            machine.setSpec(BasicMachineSpec.builder("overlay_crusher", block.basicSpec().material())
                    .machineType("crusher").energy(GregTechTags.Energy.TU, 1).energyRange(1, 1).recipes(crusher)
                    .faces(block.basicSpec().faceConfig()).build());
            machine.inventory().setStackInSlot(0, new ItemStack(Items.HEART_OF_THE_SEA));
            for (int tick = 0; tick < 64; tick++)
                BasicMachineBlockEntity.serverTick(helper.getLevel(), helper.absolutePos(pos), machine.getBlockState(), machine);
            helper.assertTrue(machine.inventory().getStackInSlot(1).is(Items.NETHER_STAR), "the crusher processes the datapack recipe");

            machine.inventory().setStackInSlot(1, ItemStack.EMPTY);
            machine.inventory().setStackInSlot(0, new ItemStack(Items.HEART_OF_THE_SEA));
            boolean started = false;
            for (int tick = 0; tick < 8 && !started; tick++) {
                BasicMachineBlockEntity.serverTick(helper.getLevel(), helper.absolutePos(pos), machine.getBlockState(), machine);
                started = machine.inventory().getStackInSlot(0).isEmpty() && machine.inventory().getStackInSlot(1).isEmpty();
            }
            helper.assertTrue(started, "the next job consumes its input before the output is flushed");
            server.reloadResources(withProbe).join();
            for (int tick = 0; tick < 64; tick++)
                BasicMachineBlockEntity.serverTick(helper.getLevel(), helper.absolutePos(pos), machine.getBlockState(), machine);
            helper.assertTrue(machine.inventory().getStackInSlot(1).is(Items.NETHER_STAR), "a reload keeps the output already rolled for the running job");

            long reindex = MachineRecipeOverlay.reindexMillis();
            helper.assertTrue(reindex < 120_000, "baseline reindex took " + reindex + " ms");
            server.reloadResources(withProbe).join();
            assertOverlay(helper, crusher, victim, input);
        } finally {
            if (!new java.util.HashSet<>(server.getPackRepository().getSelectedIds()).equals(new java.util.HashSet<>(ids)))
                server.reloadResources(ids).join();
            for (Path path : List.of(addition, tagged, removal, metadata, addition.getParent(),
                    addition.getParent().getParent(), addition.getParent().getParent().getParent(),
                    root.resolve("data"), root))
                Files.deleteIfExists(path);
        }
        helper.assertTrue(recipesWith(crusher, input) == 1, "dropping the pack restores the removed row");
        helper.assertTrue(MachineRecipeOverlay.staticRows() == untouched, "no datapack leaves the machine recipe count unchanged");
        helper.succeed();
    }

    private static void assertOverlay(GameTestHelper helper, gregtech.internal.recipe.RecipeMap crusher, Recipe victim, Item removedInput) {
        helper.assertTrue(!crusher.mRecipeList.contains(victim), "the datapack removal unlinked the matched row");
        helper.assertTrue(recipesWith(crusher, removedInput) == 0, "the exact input removal leaves no row of that item");
        var heart = crusher.findRecipe(List.of(new ItemStack(Items.HEART_OF_THE_SEA)), List.of(), false, 1, 12);
        helper.assertTrue(heart != null && heart.mOutputs[0].is(Items.NETHER_STAR), "heart of the sea crushes to a nether star");
        var sand = crusher.findRecipe(List.of(new ItemStack(Items.SOUL_SAND)), List.of(), false, 1, 12);
        var soil = crusher.findRecipe(List.of(new ItemStack(Items.SOUL_SOIL)), List.of(), false, 1, 12);
        helper.assertTrue(sand != null && sand.mOutputs[0].is(Items.DIAMOND), "soul sand expands from minecraft:soul_fire_base_blocks");
        helper.assertTrue(soil != null && soil.mOutputs[0].is(Items.DIAMOND), "soul soil expands from minecraft:soul_fire_base_blocks");
    }

    private static Recipe uniqueInput(gregtech.internal.recipe.RecipeMap map) {
        for (var recipe : map.mRecipeList) {
            var input = onlyInput(recipe);
            if (input == null || input.getCount() != 1) continue;
            if (input.is(Items.HEART_OF_THE_SEA) || input.is(Items.SOUL_SAND) || input.is(Items.SOUL_SOIL)) continue;
            if (recipesWith(map, input.getItem()) == 1) return recipe;
        }
        return null;
    }

    private static ItemStack onlyInput(Recipe recipe) {
        ItemStack found = null;
        for (var stack : recipe.mInputs) {
            if (stack == null || stack.isEmpty()) continue;
            if (found != null) return null;
            found = stack;
        }
        return found;
    }

    private static int recipesWith(gregtech.internal.recipe.RecipeMap map, Item item) {
        int count = 0;
        for (var recipe : map.mRecipeList)
            for (var stack : recipe.mInputs)
                if (stack != null && !stack.isEmpty() && stack.is(item)) { count++; break; }
        return count;
    }

    private static String addition(String map, String item, String output) {
        return "{\"type\":\"gt6ce:machine\",\"map\":\"" + map + "\",\"item_inputs\":[{\"item\":\"" + item
                + "\"}],\"item_outputs\":[{\"item\":\"" + output + "\"}],\"eu_per_tick\":0,\"duration\":16}";
    }

    private static String tagAddition(String map, String tag, String output) {
        return "{\"type\":\"gt6ce:machine\",\"map\":\"" + map + "\",\"item_inputs\":[{\"tag\":\"" + tag
                + "\"}],\"item_outputs\":[{\"item\":\"" + output + "\"}],\"eu_per_tick\":0,\"duration\":4}";
    }

    private static String removal(String map, String item, int count) {
        return "{\"type\":\"gt6ce:machine_removal\",\"map\":\"" + map + "\",\"item_inputs\":[{\"item\":\""
                + item + "\",\"count\":" + count + "}]}";
    }
}
